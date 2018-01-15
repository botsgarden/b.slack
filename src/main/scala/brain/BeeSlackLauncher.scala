package brain

import io.vertx.scala.core.Vertx
import io.vertx.lang.scala.ScalaVerticle
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object BeeSlackLauncher extends App {

  val vertx = Vertx.vertx
  val startFuture = vertx.deployVerticleFuture(ScalaVerticle.nameForVerticle[brain.BeeSlack])
  
  startFuture.onComplete {
    case Success(verticleId) => println(s"Successfully deployed verticle: $verticleId")
    case Failure(error) => println(s"Failed to deploy verticle: $error")
  }

}