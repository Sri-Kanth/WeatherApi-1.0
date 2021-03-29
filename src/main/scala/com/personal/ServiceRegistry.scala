package com.personal

//#user-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable



object ServiceRegistry {
  // actor protocol
  sealed trait Command
  final case class GetCurrentTempLevel(latitude:String, longitude:String, replyTo: ActorRef[WeatherResp]) extends Command


  def apply(): Behavior[Command] = registry

  private def registry: Behavior[Command] =
    Behaviors.receiveMessage {
      case GetCurrentTempLevel(latitude, longitude, replyTo) =>
        WeatherAPI.getCurrentTempLevel(latitude, longitude, replyTo)
        //currentTempLevel.onComplete(respTempLevel => replyTo ! respTempLevel.getOrElse("").toString)

        Behaviors.same
    }
}
//#user-registry-actor
