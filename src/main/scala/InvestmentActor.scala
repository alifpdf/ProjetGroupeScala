import Main.timeout
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object InvestmentActor {
  case class AddInvestment(userId: Int, companyName: String, amountInvested: BigDecimal)
  case class GetInvestments(userId: Int)
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

    case DeleteInvestment(investmentId: Int, companyName: String) =>
      val senderRef = sender()
      dbService.deleteInvestment(investmentId, companyName).onComplete {
        case Success(_) => senderRef ! "Success"

      }

    case UpdateInvestment(investmentId: Int, companyName: String, newAmount: BigDecimal) =>
      val senderRef = sender()
      dbService.updateInvestment(investmentId, companyName, newAmount) onComplete {
        case Success(_) => senderRef ! "Success oklm"
        case Failure(e) => senderRef ! "Failure"
      }


    case RecupererlaSomme(companyName, id, sommeInvesti) =>
      val senderRef = sender() // Sauvegarde du sender
      // Étape 1 : Mise à jour de l'investissement
      (self ? UpdateInvestment(id, companyName, 0)).map {
        case Success(_) =>
          // Étape 2 : Récupération du solde actuel de l'utilisateur après la mise à jour
          (actor ? UtilisateurActor.GetBalance1(id)).map {
            case balance: BigDecimal =>
              // Étape 3 : Mise à jour de la balance utilisateur
              actor ! UtilisateurActor.updateBalance(id, balance + sommeInvesti)
              senderRef ! "Mise à jour réussie"
            case _ =>
              senderRef ! "Erreur : Impossible de récupérer la balance"
          }


      }
  }
}
