import akka.actor.{Actor, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

// Définitions des messages pour l'Acteur
object UtilisateurActor {
  case class AddUtilisateur(name: String, email: String, password: String)
  case class GetUtilisateur()
  case class GetEmail(email: String)
  case class VerifierPassword(email: String, password: String)

  // Méthode pour créer un acteur
  def props(dbService: DatabaseService): Props = Props(new UtilisateurActor(dbService))
}

// Implémentation de l'Acteur
class UtilisateurActor(dbService: DatabaseService) extends Actor {
  import UtilisateurActor._

  def receive: Receive = {
    case AddUtilisateur(name, email, password) =>
      val user = User(name, email, password)

      dbService.addUser(user)

    case GetUtilisateur() =>

      dbService.getUsers

    case GetEmail(email) =>

      dbService.getEmail(email)


    case VerifierPassword(email, password) =>
      val senderRef = sender()
      dbService.checkPassword(email, password).onComplete{
        case Success(true) => senderRef ! "success"
          case Success(false) => senderRef ! "failure"
      }
  }
}
