import akka.actor.{Actor, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

// Définitions des messages pour l'Acteur
object UtilisateurActor {
  case class AddUtilisateur(name: String, email: String, password: String, balance: BigDecimal)
  case class VerifierPassword(email: String, password: String)
  case class GetBalance(email: String)
  case class updateBalance(email: String, balance: BigDecimal)

  // Méthode pour créer un acteur
  def props(dbService: DatabaseService): Props = Props(new UtilisateurActor(dbService))
}

// Implémentation de l'Acteur
class UtilisateurActor(dbService: DatabaseService) extends Actor {
  import UtilisateurActor._

  def receive: Receive = {
    case AddUtilisateur(name, email, password, balance) =>
      val senderRef = sender()
      val user = User(name, email, password, balance)
      dbService.addUser(user).onComplete {
        case Success(_) => senderRef ! s"✅ Utilisateur $name ajouté avec succès."

      }


    case VerifierPassword(email, password) =>
      val senderRef = sender()
      dbService.checkPassword(email, password).onComplete{
        case Success(true) => senderRef ! true
          case Success(false) => senderRef ! false
      }
    case GetBalance(email) =>
      val senderRef = sender()
      dbService.getsomme_restant(email).onComplete {
        case Success(balance) => senderRef ! balance
      }

    case updateBalance(email, amount) =>
      val senderRef = sender()
      dbService.updateSommeCompte(amount, email).onComplete{
        case Success(_) => senderRef ! "success"
      }


  }
}
