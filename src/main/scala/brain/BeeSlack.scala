package brain

import io.vertx.core.json.JsonObject
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.web.Router
import io.vertx.scala.servicediscovery.types.HttpEndpoint
import io.vertx.scala.ext.web.handler.StaticHandler
import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.servicediscovery.{ServiceDiscovery, ServiceDiscoveryOptions}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import io.vertx.lang.scala.ScalaVerticle
import scala.concurrent.{Future}

object BeeSlack extends App {

  val vertx = Vertx.vertx
  
  // Settings for the Redis backend
  val redisHost = sys.env.getOrElse("REDIS_HOST", "127.0.0.1")
  val redisPort = sys.env.getOrElse("REDIS_PORT", "6379").toInt
  val redisAuth = sys.env.getOrElse("REDIS_PASSWORD", null)
  val redisRecordsKey = sys.env.getOrElse("REDIS_RECORDS_KEY", "vert.x.ms")

  // Settings for record the service
  val serviceName = sys.env.getOrElse("SERVICE_NAME", "calculator")
  val serviceHost = sys.env.getOrElse("SERVICE_HOST", "localhost") // domain name
  val servicePort = sys.env.getOrElse("SERVICE_PORT", "8080").toInt // set to 80 on Clever Cloud
  val serviceRoot = sys.env.getOrElse("SERVICE_ROOT", "/api")

  // Mount the service discovery backend (Redis)
  val discovery = ServiceDiscovery.create(vertx, ServiceDiscoveryOptions()
    .setBackendConfiguration(
      new JsonObject()
        .put("host", redisHost)
        .put("port", redisPort)
        .put("auth", redisAuth)
        .put("key", redisRecordsKey)
    )
  )

  // create the microservice record
  val record = HttpEndpoint.createRecord(
    serviceName,
    serviceHost,
    servicePort,
    serviceRoot
  )

  vertx
    .deployVerticleFuture(ScalaVerticle.nameForVerticle[brain.BeeSlack])
    .onComplete {
      case Success(verticleId) => println(s"Successfully deployed verticle: $verticleId")
      case Failure(error) => println(s"Failed to deploy verticle: $error")
    }

}

class BeeSlack extends ScalaVerticle {

  override def stopFuture(): Future[_] = {

    /* Java Version
      public void stop(Future<Void> stopFuture) {
        System.out.println("Unregistration process is started ("+record.getRegistration()+")...");

        discovery.unpublish(record.getRegistration(), asyncResult -> {
          if(asyncResult.succeeded()) {
            System.out.println("👋 bye bye " + record.getRegistration());
          } else {
            System.out.println("😡 Not able to unpublish the microservice: " + asyncResult.cause().getMessage());
            //asyncResult.cause().printStackTrace();
          }
          stopFuture.complete();
        });
      }
    */
    var unpublishRecordFuture = BeeSlack.discovery.unpublishFuture(BeeSlack.record.getRegistration)
    
    unpublishRecordFuture.onComplete {
      case Success(result) => {
        println(s"😃 removing publication OK")
        Future.successful(())
      }
      case Failure(cause) => {
        println(s"😡 removing publication KO: $cause")
        Future.successful(())
        //Future.failed(new Throwable())
      }
    }
    unpublishRecordFuture
  }

  override def startFuture(): Future[_] = {
    
    val server = vertx.createHttpServer()
    val router = Router.router(vertx)
    val httpPort = sys.env.getOrElse("PORT", "8080").toInt

    // publish the microservice record
    BeeSlack.discovery.publishFuture(BeeSlack.record).onComplete{
      case Success(result) => println(s"😃 publication OK")
      case Failure(cause) => println(s"😡 publication KO: $cause")
    }
    
    router.route().handler(BodyHandler.create)

    router.get("/hey").handler(context => 
      context
        .response
        .putHeader("content-type", "application/json;charset=UTF-8")
        .end(new JsonObject().put("message", "👋 hey!").encodePrettily)                            
    )

    router.post("/yo").handler( context => {
          
      val message = context.getBodyAsJson match {
        case None => "???"
        case Some(jsonObject) => jsonObject.getString("message")
      }
      //println(message)
      
      context
        .response
        .putHeader("content-type", "application/json;charset=UTF-8")
        .end(new JsonObject()
          .put("message", s"👋 🌍 yo!: $message")
          .encodePrettily
        )
    })

    router.route("/*").handler(StaticHandler.create)

    println(s"🌍 🐝 Bee Slack module Listening on $httpPort  - Enjoy 😄")
    server.requestHandler(router.accept _).listenFuture(httpPort)
  }

}
