package com.personal

//import com.personal.ServiceRegistry.ActionPerformed

//#json-formats
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val currentTempFormat = jsonFormat2(CurrentTemp)
  implicit val weatherRespFormat = jsonFormat4(WeatherResp)
  /*

  implicit val userJsonFormat = jsonFormat3(User)
  implicit val usersJsonFormat = jsonFormat1(Users)*/

 // implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
//#json-formats
