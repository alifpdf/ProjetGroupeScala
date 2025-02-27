import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors

import scala.concurrent.ExecutionContext


import AkkaStream._

class WebSocketServer(implicit system: ActorSystem, ec: ExecutionContext) {



  // **3️⃣ Route WebSocket**
  private val route = cors() {
    concat(
      pathEndOrSingleSlash {
        complete("✅ Serveur WebSocket en cours d'exécution. Connectez-vous sur /ws")
      },
      path("ws") {
        handleWebSocketMessages(websocketFlow())
      }
    )
  }

  // **4️⃣ Démarrer le serveur HTTP**
  def start(): Unit = {
    Http().newServerAt("localhost", 8080).bind(route)
    println("✅ Serveur WebSocket en écoute sur ws://localhost:8080/ws")
  }
}
