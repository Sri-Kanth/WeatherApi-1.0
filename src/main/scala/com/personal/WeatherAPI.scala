package com.personal

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.event.slf4j.Logger
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.Future
import scala.util.{Failure, Success}
import org.json4s._
import org.json4s.native.JsonMethods._

case class CurrentTemp(temp: Option[String], feels_like: Option[String]) {
  def hasTemp: Boolean = {
    temp.isDefined
  }
}

case class WeatherResp(current: Option[CurrentTemp], isFailure: Boolean = false, failMsg:Option[String] =  None, var currentDerivedFeelsLike:Option[String] = None)

object WeatherCondition extends Enumeration {
  val COLD = Value("Cold")
  val WARM = Value("Warm")
  val HOT = Value("Hot")
  val WRONG = Value("Something is wrong")

  def parseDouble(s: String) = try { Some(s.toDouble) } catch { case _ => None }

  def transform(weatherCurrent:Option[String]): Value = {
    weatherCurrent match {
      case Some(weatherValCurr) => {
        parseDouble(weatherValCurr) match {
          case Some(weather_dbl) => {
            mapToLevel(weather_dbl)
          }
          case None => WRONG
        }
      }
      case None => WRONG
    }

  }

  def mapToLevel(tempVal:Double) = tempVal match {
    case tempCold if tempCold <= 45 => COLD
    case tempWarm if (tempWarm > 45 && tempWarm <= 80) => WARM
    case tempHot if (tempHot > 80 && tempHot < 140) => HOT
    case _ => WRONG
  }
}

/**
 * Weather API object to invoke the external ONEAPI call and parse the response to extract temp.
 *
 */
object WeatherAPI extends App {

  lazy val logger = Logger("WeatherAPI")

  implicit class HttpResponseExtn(httpResp: HttpResponse) {
    def isJson: Boolean = httpResp.entity.contentType == ContentTypes.`application/json`

  }

  implicit class WeatherRespStringExtn(jsonString: String) {
    def getCurrentTemp: Option[String] = {
      implicit val formats: Formats = DefaultFormats

      val currentTemperature = parse(jsonString).extract[WeatherResp] match {
        case WeatherResp(current, _ , _ , _) if current.isDefined => {
          println(s"current temp: $current")

          current.get.temp
        }
        case _ => None
      }
      currentTemperature
    }


    implicit val formats: Formats = DefaultFormats

    def getWeatherResp = {
      val weatherResp = parse(jsonString).extract[WeatherResp]
      val currentTempLevel = WeatherCondition.transform(this.getCurrentTemp)

      weatherResp.currentDerivedFeelsLike = Some(currentTempLevel.toString)

      weatherResp
    }
  }

  /**
   * Fetch the current temperature given the latitude and longitude and respond back with derived feels like temp level.
   * @param latitude
   * @param longitude
   * @param replyTo
   */
  def getCurrentTempLevel(latitude:String, longitude:String, replyTo: ActorRef[WeatherResp])  {

    implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")


    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext


    lazy val apiKey = ConfigFactory.load().getString("weatherApi.apiKey")

    val baseWeatherApiPath = "https://api.openweathermap.org/data/2.5/onecall"

    println(apiKey)

    val weatherUri = Uri(baseWeatherApiPath).withQuery(Query("lat" -> latitude.toString,
      "lon" -> longitude.toString, "appid" -> apiKey.toString,
      "units" -> "imperial"))


    println(s"weatherUri: ${weatherUri.toString()}")

    //TODO use host level connection pool/sep dispatcher
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = weatherUri.toString()))

    //use separate dispatcher for blocking calls
    responseFuture
      .onComplete {
        case Success(res) if res.isJson => {
          Unmarshal(res.entity).to[String].map { jsonString =>
            println(jsonString)

            replyTo ! jsonString.getWeatherResp
          }
        }
        case Success(nonJsonResp) => {
          Failure(new IllegalArgumentException("Non Json Response"))
          replyTo ! WeatherResp(None, true, Some("FAILURE_NON_JSON"))
        }
        case Failure(_) => replyTo ! WeatherResp(None, true, Some("FAILURE_OTHER")) //Failure(new IllegalArgumentException("Failed to Invoke Weather API"))
      }


  }

}
