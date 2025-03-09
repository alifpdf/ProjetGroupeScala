import Main.{timeout, utilisateurActor}
import akka.actor.typed.ActorRef
import akka.actor.{Actor, Props}
import akka.pattern.{ask, pipe}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

// Définitions des messages pour l'Acteur
object UtilisateurActor {
  case class GetUser(id:Int)
  case class GetUsers()
  case class GetStringUsers()
  case class GetId(email: String)
  case class GetEmail(id:Int)
  case class AddUtilisateur(name: String, email: String, password: String, balance: BigDecimal)
  case class VerifierPassword(email: String, password: String)
  case class GetBalance(email: String)
  case class GetBalance1(id:Int)
  case class updateBalance(id: Int, balance: BigDecimal)
  case class connexion(email: String, password: String)

    // Méthode pour créer un acteur
    def props(dbService: DatabaseService): Props = Props(new UtilisateurActor(dbService))
}

// Implémentation de l'Acteur
class UtilisateurActor(dbService: DatabaseService) extends Actor {
  import UtilisateurActor._


  def receive: Receive = {

    case GetUsers =>
      val replyTo = sender() // Capture du sender() avant l'opération asynchrone
      println("🔍 [UtilisateurActor] Requête reçue : GetUsers") // DEBUG
      dbService.getUsers.onComplete {
        case Success(users) =>
          println(s"✅ [UtilisateurActor] Réponse envoyée : ${users.size} utilisateurs")
          replyTo ! users
      }

    case GetStringUsers =>
      val originalSender = sender()
      dbService.getAllUsers.onComplete {
        case Success(jsonString) =>
          println(s"📌 JSON récupéré depuis la base de données : $jsonString") // Debugging
          originalSender ! jsonString
      }




    case GetUser(id) =>
      val replyTo = sender
      dbService.getUser(id).pipeTo(replyTo)


      case GetEmail(id) =>
      val replyTo = sender
      dbService.getEmail(id).pipeTo(replyTo)



    case GetId(email:String) =>
      val senderRef = sender()
      dbService.getId(email).onComplete{
        case Success(id) =>senderRef ! id
      }

    case AddUtilisateur(name, email, password, balance) =>
      val senderRef = sender()
      val user = User(None,name, email, password, balance)

      dbService.addUser(user).map(_ => s"✅ Utilisateur $name ajouté avec succès.").pipeTo(senderRef)



    case VerifierPassword(email, password) =>
      val senderRef = sender()
      dbService.checkPassword(email, password).onComplete{
        case Success(true) => senderRef ! true
          case Success(false) => senderRef ! false
      }
    case GetBalance(email) =>
      val senderRef = sender()
      dbService.getsomme_restant(email).pipeTo(senderRef)


    case GetBalance1(userId) =>
      val senderRef = sender()
      println(s"📢 Récupération du solde utilisateur ID: $userId")

      dbService.getsomme_restant1(userId).pipeTo(senderRef)


    case updateBalance(id, amount) =>
      val senderRef = sender()
      dbService.updateSommeCompte(amount, id).onComplete{
        case Success(_) => senderRef ! "success"
      }


    case connexion(email, password) =>
      val senderRef = sender()

      // ✅ Vérifier le mot de passe
      (utilisateurActor ? UtilisateurActor.VerifierPassword(email, password)).mapTo[Boolean].flatMap {
        case true =>
          println(s"✅ [Connexion] Mot de passe correct pour $email")

          // ✅ Récupérer l'ID de l'utilisateur
          (utilisateurActor ? UtilisateurActor.GetId(email)).mapTo[Int].flatMap {
            case userId if userId > 0 =>
              println(s"✅ [Connexion] ID utilisateur récupéré : $userId")

              // ✅ Récupérer les informations utilisateur
              (utilisateurActor ? UtilisateurActor.GetUser(userId)).mapTo[Option[User]].map {
                case Some(user) =>
                  val userJson = Json.obj(
                    "id" -> user.id,
                    "name" -> user.name,
                    "email" -> user.email,
                    "balance" -> user.balance
                  ).toString()

                  println(s"✅ [Connexion] Utilisateur trouvé : $userJson")
                  senderRef ! userJson // ✅ Retourne l'utilisateur en `String JSON`
              }

          }

        case false =>
          println(s"❌ [Connexion] Mot de passe incorrect pour $email")
          senderRef ! "❌ Mot de passe incorrect"
          Future.successful(())
      }.recover {
        case e: Exception =>
          println(s"❌ [Connexion] Erreur serveur : ${e.getMessage}")
          senderRef ! "❌ Erreur interne du serveur"
      }


  }
}
