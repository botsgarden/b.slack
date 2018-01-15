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

object BeeSlack extends App {

  val vertx = Vertx.vertx()
  val vertx = Vertx.vertx()
  val server = vertx.createHttpServer()
  val router = Router.router(vertx)

  val httpPort = sys.env.getOrElse("PORT", "8080").toInt

  router.route().handler(BodyHandler.create)

  router.get("/hey").handler(context => 
    context
      .response
      .putHeader("content-type", "application/json;charset=UTF-8")
      .end(new JsonObject().put("message", "👋 hey!").encodePrettily)                            
  )

  router.post("/yo").handler( context => {
        
    val message = context.getBodyAsJson() match {
      case None => "???"
      case Some(jsonObject) => jsonObject.getString("message")
    }
    println(message)
    
    context
      .response
      .putHeader("content-type", "application/json;charset=UTF-8")
      .end(new JsonObject()
        .put("message", s"👋 🌍 yo!: $message")
        .encodePrettily
      ) 

  })

  router.route("/*").handler(StaticHandler.create)

  println(s"🌍 Listening on $httpPort  - Enjoy 😄")
  server.requestHandler(router.accept).listen(httpPort)
  
}
