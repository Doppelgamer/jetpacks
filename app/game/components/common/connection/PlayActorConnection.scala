package game.components.common.connection

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive

import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.Enumerator
import game.components.common.io.{ClientCommand, ServerCommand}
import play.api.libs.json.{JsObject, JsValue, Json}
import ClientCommand.ServerQuit

object PlayActorConnection {
  type MessageId = Long

  def props(channel: Channel[JsValue]) = Props(classOf[PlayActorConnection], channel)

  // Received Messages
  case object GetEnum

  case class Ack(id: MessageId) extends ServerCommand

  // Sent Messages
  case class ReturnEnum(enum: Enumerator[String])

}

class PlayActorConnection(val toClient: Channel[JsValue]) extends Actor {

  import PlayActorConnection._

  var toServer: Option[ActorRef] = None
  var seq: MessageId = 0
  var retryers: Map[MessageId, ActorRef] = Map()

  def retry(msg: JsValue) {
    val prop = Retryer.props(msg, toClient)
    retryers += seq -> context.actorOf(prop, "retryer-" + seq.toString)
  }

  def send(cc: ClientCommand) = {
    val msg = Json.obj(
      "seq" -> seq,
      "ack" -> cc.doRetry,
      "type" -> cc.typ,
      "message" -> cc.toJson
    )
    toClient.push(msg)
    if (cc.doRetry) {
      retry(msg)
      seq += 1
    }
  }

  /**
   * For a given MessageId, this kills its associated `helper` Actor
   * and removes it from the `helpers` map
   */
  def ack(id: MessageId) {
    for (ref <- retryers.get(id)) {
      ref ! PoisonPill
    }
    retryers -= id
  }

  override def receive = LoggingReceive {
    case json: JsObject if toServer.isDefined =>
      val data = json \ "data"
      (json \ "type").as[String] match {
        case "ack" => ack(data.as[Int])
        case _ => toServer.get ! json
      }

    case cc: ClientCommand => send(cc)
    case server: ActorRef => toServer = Some(server)
  }

  override def postStop() {
    send(ServerQuit)
    toClient.eofAndEnd()
  }
}