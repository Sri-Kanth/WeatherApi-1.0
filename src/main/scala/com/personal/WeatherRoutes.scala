package com.personal

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import com.personal.ServiceRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

//#import-json-formats
//#user-routes-class
class WeatherRoutes(serviceRegistry: ActorRef[ServiceRegistry.Command])(implicit val system: ActorSystem[_]) {

  //#user-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getWeather(latitude:String, longitude:String): Future[WeatherResp] =
    serviceRegistry.ask(GetCurrentTempLevel(latitude, longitude, _))


  //#all-routes
  //#users-get-post
  //#users-get-delete
  val weatherRoutes: Route =
    pathPrefix("weather") {

      concat(
        //#users-get-delete
        pathEnd {
          concat(
            get {
              parameters("latitude", "longitude") { (latitude, longitude) => {
                  println(latitude + " " + longitude);
                  onSuccess(getWeather(latitude, longitude)) { response =>
                    complete(response)
                  }


                }
              }
            })
        })
      //#users-get-delete
    }
  //#all-routes
}
