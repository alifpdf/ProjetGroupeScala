import WebSocketServer.system.dispatcher
import akka.actor.ActorSystem
import akka.http.scaladsl.Http

import akka.http.scaladsl.server.Directives._

import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors

import scala.io.StdIn

object WebSocketServer extends App {
  implicit val system: ActorSystem = ActorSystem("WebServerSystem")


  // Route HTTP avec CORS activé
  val route = cors() {
    pathEndOrSingleSlash {
      complete("Hello, World!")
    }
  }


  // Démarrer le serveur HTTP
  val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

  println("✅ Serveur HTTP lancé sur http://localhost:8080/")


}
