import Main.{timeout, utilisateurActor}
import akka.actor.{Actor, Props}
import akka.pattern.ask
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SocketActor {
  def props(dbNotification: DBNotification): Props = Props(new SocketActor(dbNotification))

  // Messages gérés par l'actor
  case class SendNotification(userId: Int, message: String) // Notification privée
  case class BroadcastMessage(message: String) // Diffusion globale
  case class GetNotifications(userId: Int) // Récupération des notifications
  case class DeleteNotification(notificationId: Int, userId: Int) // Suppression d'une notification
}

class SocketActor(dbNotification: DBNotification) extends Actor {

  import SocketActor._

  def receive: Receive = {

    // Envoi d'une notification privée
    case SendNotification(userId, message) =>
      println(s"Envoi de notification à l'utilisateur $userId : $message")
      dbNotification.insertNotification(userId, message)

    // Diffusion d'un message à tous les utilisateurs
    case BroadcastMessage(message) =>
      println(s"Diffusion globale : $message")

      // Récupérer tous les utilisateurs depuis l'UtilisateurActor
      (utilisateurActor ? UtilisateurActor.GetUsers).mapTo[Seq[User]].flatMap { users =>
        val userIds = users.flatMap(_.id)

        // Insérer une notification pour chaque utilisateur
        Future.sequence(userIds.map { userId =>
          dbNotification.insertNotification(userId, message)
        })
      }


    // Récupération des notifications d'un utilisateur
    case GetNotifications(userId) =>
      val senderRef = sender()
      dbNotification.getAllNotifications(userId).map { notifs =>
        senderRef ! notifs
      }

    // Suppression d'une notification
    case DeleteNotification(notificationId, userId) =>
      val senderRef = sender()
      dbNotification.deleteNotification(notificationId).map { deletedRows =>
        if (deletedRows > 0) {
          println(s"Notification $notificationId supprimée avec succès")
          senderRef ! Json.obj("success" -> true, "message" -> "Notification supprimée").toString()

        }
      }
  }

}
