import AkkaStream.websocketFlow
import Main.{notificationActor, utilisateurActor, utilisateurActor2}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
// 🔥 Augmente le délai d'attente


case class RecupererSommeRequest(companyName: String, userId: Int, sommeInvesti: BigDecimal)

object RecupererSommeRequest {
  implicit val format: Format[RecupererSommeRequest] = Json.format[RecupererSommeRequest]
}
case class Connexion(email: String, password: String)

object Connexion{

  implicit val format: Format[Connexion] = Json.format[Connexion]
}

case class AddUserRequest(name: String, email: String, password: String)
object AddUserRequest {

  implicit val format: Format[AddUserRequest] = Json.format[AddUserRequest]
}

class WebSocketServer(implicit system: ActorSystem, ec: ExecutionContext) {

  implicit val timeout: Timeout = Timeout(5.seconds)

  val route: Route = cors() {
    concat(
      pathEndOrSingleSlash {
        complete(StatusCodes.OK, "✅ Serveur WebSocket en cours d'exécution. Connectez-vous sur /ws")
      },
//on envoie au frontend
      path("ws") {
        handleWebSocketMessages(websocketFlow())
      },

      //on récupère depuis le frontend
      path("api" / "recuperer-somme") {
        post {
          entity(as[String]) { body =>
            println(s"📢 Requête API reçue : $body") // Debugging

            Json.parse(body).validate[RecupererSommeRequest] match {
              case JsSuccess(request, _) =>
                println(s"✅ Demande valide pour récupérer ${request.sommeInvesti}€ de ${request.companyName} (user: ${request.userId})")
                notificationActor?SocketActor.SendNotification(request.userId,s"✅ Demande valide pour récupérer ${request.sommeInvesti}€ de ${request.companyName} (user: ${request.userId})")

                complete(
                  (utilisateurActor2 ? InvestmentActor.RecupererlaSomme(request.companyName, request.userId, request.sommeInvesti)).mapTo[String].map(response => Json.obj("success" -> true, "message" -> response).toString()))

                }

              }
        }
      },
      path("api" / "add-user") {
        post {
          entity(as[String]) { body =>
            Json.parse(body).validate[AddUserRequest] match {
              case JsSuccess(request, _) =>
                val futureResponse: Future[String] = (utilisateurActor ? UtilisateurActor.AddUtilisateur(request.name, request.email, request.password, 0)).mapTo[String]
                complete(
                  futureResponse.map(response =>
                    Json.obj("success" -> !response.startsWith("❌"), "message" -> response).toString()
                  )
                )

            }
          }
        }
        },
      path("api" / "login") {
        post {
          entity(as[String]) { body =>
            println(s"📢 [API] Requête de connexion reçue : $body")

            Json.parse(body).validate[Connexion] match {
              case JsSuccess(request, _) =>
                println(s"✅ [API] Connexion de ${request.email}")

                val futureResponse: Future[String] =
                  (utilisateurActor ? UtilisateurActor.connexion(request.email, request.password))
                    .mapTo[String]

                complete(
                  futureResponse.map(response =>
                    Json.obj("success" -> !response.startsWith("❌"), "user" -> Json.parse(response)).toString()
                  )
                )

             }
          }
        }
      },

      path("api" / "get-notifications") {
        post {
          entity(as[String]) { body =>
            val json = Json.parse(body)
            val userId = (json \ "userId").as[Int]

            println(s"📢 Récupération des notifications pour l'utilisateur $userId")

            val futureNotifications = (notificationActor ? SocketActor.GetNotifications(userId)).mapTo[Seq[Notification]]

            onComplete(futureNotifications) {
              case Success(notifs) =>
                complete(HttpEntity(ContentTypes.`application/json`, Json.obj(
                  "success" -> true,
                  "notifications" -> Json.toJson(notifs) // ✅ Maintenant Play JSON sait comment convertir la liste en JSON
                ).toString()))


            }
          }
        }
      },
      path("api" / "delete-notification") {
        post {
          entity(as[String]) { body =>
            val json = Json.parse(body)
            val notificationId = (json \ "notificationId").as[Int]
            val userId = (json \ "userId").as[Int] // ✅ Ajout de la récupération de l'ID utilisateur

            println(s"🗑️ Suppression de la notification $notificationId pour l'utilisateur $userId")

            val futureDelete = (notificationActor ? SocketActor.DeleteNotification(notificationId, userId)).mapTo[String]

            onComplete(futureDelete) {
              case Success(response) =>
                complete(HttpEntity(ContentTypes.`application/json`, response))


            }
          }
        }
      }




    )
  }

  def start(): Unit = {
    Http().newServerAt("localhost", 8080).bind(route)
    println("✅ Serveur WebSocket en écoute sur ws://localhost:8080/ws et API REST sur /api/recuperer-somme")
  }
}
