package game

import akka.actor.{ActorRef, ActorSystem}

import doppelengine.core.Engine
import game.systems.{VisualSystem, QuitSystem}
import game.systems.physics.PhysicsSystem
import scala.concurrent.duration._
import game.systems.connection.ConnectionSystem
import scala.concurrent.{Await, Future}
import doppelengine.system.SystemConfig

object MyGame {

  val numPlayers = 2

  private val sysConfigs: Set[SystemConfig] = Set(
    SystemConfig(ConnectionSystem.props, "connection-system"),
    SystemConfig(QuitSystem.props, "quit-system"),
    SystemConfig(VisualSystem.props, "visual-system"),
    SystemConfig(PhysicsSystem.props(0, -35), "physics-system")
  )

  //  private val walls: Set[(ComponentType, ComponentConfig)] = Set(
  //    Dimension -> new ComponentConfig(DimensionComponent.props(25, 0, 50, 1), "floor"),
  //    Dimension -> new ComponentConfig(DimensionComponent.props(25, 50, 50, 1), "ceiling"),
  //    Dimension -> new ComponentConfig(DimensionComponent.props(0, 25, 1, 50), "left_wall"),
  //    Dimension -> new ComponentConfig(DimensionComponent.props(50, 25, 1, 50), "right_wall")
  //  )

  implicit val timeout: akka.util.Timeout = 1.second

  private val actorSystem: ActorSystem = akka.actor.ActorSystem("Doppelsystem")

  private val doppelengine: ActorRef =
    actorSystem.actorOf(Engine.props(sysConfigs, Set()), name = "engine")

  val connectionSystem = {
    val fConnSystem: Future[ActorRef] =
      actorSystem.actorSelection(doppelengine.path / "connection-system").resolveOne
    Await.result(fConnSystem, 1000 millis)
  }
}
