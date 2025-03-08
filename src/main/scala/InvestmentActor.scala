import Main.timeout
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.pattern.pipe

object InvestmentActor {
  case class AddInvestment(userId: Int, companyName: String, amountInvested: BigDecimal)
  case class GetInvestments(userId: Int)
  case class GetInvestmentsString(userId: Int)
  case class DeleteInvestment(investmentId: Int,companyName:String)
  case class UpdateInvestment(investmentId: Int, companyName:String,newAmount: BigDecimal)
  case class RecupererlaSomme(companyName:String,id:Int, amount:BigDecimal)

  def props(dbService: DatabaseService1, actor:ActorRef): Props = Props(new InvestmentActor(dbService,actor))
}

class InvestmentActor(dbService: DatabaseService1,actor: ActorRef) extends Actor {

  import InvestmentActor._
  import context._

  def receive: Receive = {
    case AddInvestment(userId, companyName, amount) =>
      val senderRef = sender() // Sauvegarde du sender
      // Étape 1 : Récupérer le solde actuel de l'utilisateur
      (self ? UtilisateurActor.GetBalance1(userId)).flatMap {
        case balance: BigDecimal =>
          val newBalance = balance - amount
          if (newBalance < 0) {
            Future.successful(senderRef ! "Échec : Solde insuffisant")
          } else {
            // Étape 2 : Mettre à jour le solde avant d'ajouter l'investissement
            (self ? UtilisateurActor.updateBalance(userId, newBalance)).flatMap {
              case Success(_) =>
                // Étape 3 : Ajouter l’investissement après mise à jour du solde
                dbService.addInvestment(userId, companyName, amount).map { _ =>
                  senderRef ! "Succès : Investissement ajouté"
                }
            }
          }
      }




  case GetInvestments(userId) =>
      val senderRef = sender()
      dbService.getInvestmentsByUser(userId).onComplete {
        case Success(investments) => senderRef ! investments

      }

    case GetInvestmentsString(userId) =>
      val senderRef = sender()
      dbService.getInvestmentsByUserString(userId).onComplete {
        case Success(investmentsJson) =>
          println(s"📌 Envoi des investissements au frontend : $investmentsJson") // Debug
          senderRef ! investmentsJson

      }



    case DeleteInvestment(investmentId: Int, companyName: String) =>
      val senderRef = sender()
      dbService.deleteInvestment(investmentId, companyName).onComplete {
        case Success(_) => senderRef ! "Success"

      }



    case UpdateInvestment(investmentId: Int, companyName: String, newAmount: BigDecimal) =>
      val senderRef = sender() // 🔥 Capture du sender pour lui répondre
      println(s"📢 Mise à jour de l'investissement: $companyName (ID: $investmentId) → Nouveau montant: $newAmount")

      dbService.updateInvestment(investmentId, companyName, newAmount)
        .map(_ => "✅ Mise à jour réussie")
        .recover { case e => s"❌ Échec de la mise à jour: ${e.getMessage}" }
        .pipeTo(senderRef) // ✅ Envoie la réponse à `senderRef` automatiquement



    case RecupererlaSomme(companyName, id, sommeInvesti) =>
      val senderRef = sender()
      println(s"📢 [InvestmentActor] Demande reçue pour récupérer $sommeInvesti de $companyName (User ID: $id)")

      (self ? UpdateInvestment(id, companyName, 0)).flatMap {
        case "✅ Mise à jour réussie" =>
          println(s"✅ [InvestmentActor] Investissement de $companyName mis à 0 pour l'utilisateur $id.")

          (actor ? UtilisateurActor.GetBalance1(id)).flatMap {
            case balance: BigDecimal =>
              println(s"📢 [InvestmentActor] Solde actuel de l'utilisateur: $balance")
              val nouveauSolde = balance + sommeInvesti
              println(s"✅ [InvestmentActor] Nouveau solde après récupération: $nouveauSolde")

              (actor ? UtilisateurActor.updateBalance(id, nouveauSolde)).map { _ =>
                println(s"✅ [InvestmentActor] Solde mis à jour, envoi de la réponse...")
                "✅ Somme récupérée avec succès"
              }

          }
      }.pipeTo(senderRef) // ✅ Envoie la réponse à `senderRef`



  }
}
