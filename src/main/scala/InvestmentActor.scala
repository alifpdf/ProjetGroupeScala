import Main.{timeout, utilisateurActor}
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.pattern.pipe

object InvestmentActor {
  case class AddInvestment(userId: Int, companyName: String, amountInvested: BigDecimal,originalPrice: BigDecimal)
  case class GetInvestments(userId: Int)
  case class GetInvestmentsString(userId: Int)
  case class DeleteInvestment(investmentId: Int,companyName:String)
  case class UpdateInvestment(investmentId: Int, companyName:String,newAmount: BigDecimal)
  case class RecupererlaSomme(companyName:String,id:Int, amount:BigDecimal)
  case object GetAllInvestmentsString


  def props(dbService: DBInvestment, actor:ActorRef): Props = Props(new InvestmentActor(dbService,actor))
}

class InvestmentActor(dbService: DBInvestment,actor: ActorRef) extends Actor {

  import InvestmentActor._
  import context._

  def receive: Receive = {



    case AddInvestment(userId, companyName, amount, originalPrice) =>
      val senderRef = sender()

      val result = for {
        // 🔹 Vérifier le solde de l'utilisateur
        balance <- (utilisateurActor ? UtilisateurActor.GetBalance1(userId)).mapTo[BigDecimal]
        newBalance = balance - amount

        // 🔹 Vérifier si l'utilisateur a déjà investi
        existingInvestmentOpt <- dbService.getInvestmentByUserAndCompany(userId, companyName)

        // 🔹 Vérifier le solde
        _ <- if (newBalance < 0) {
          println(s" [InvestmentActor] Solde insuffisant pour investir $amount €")
          Future.successful(senderRef ! "Échec : Solde insuffisant")
        } else {
          (utilisateurActor ? UtilisateurActor.updateBalance(userId, newBalance)).map(_ => ())
        }

        // 🔹 Mise à jour ou ajout de l'investissement
        investmentId <- existingInvestmentOpt match {
          case Some(existingInvestment) =>
            val updatedAmount = existingInvestment.amountInvested + amount
            dbService.updateInvestment(userId, companyName, updatedAmount).map { _ =>
              println(s" [InvestmentActor] Investissement mis à jour : $companyName -> $updatedAmount €")
              existingInvestment.id.get // On récupère l'ID existant
            }

          case None =>
            dbService.addInvestment(userId, companyName, amount, originalPrice).map { newId =>
              println(s" [InvestmentActor] Nouvel investissement ajouté pour $companyName avec ID $newId")
              newId // Retourne l'ID du nouvel investissement
            }
        }
      } yield {
        senderRef ! investmentId
      }

      result




    case GetAllInvestmentsString =>
      val senderRef = sender()
      dbService.getAllInvestmentsString.onComplete {
        case Success(investmentsJson) =>
          println(s" Envoi de tous les investissements au frontend : $investmentsJson")
          senderRef ! investmentsJson
      }


    case GetInvestments(userId) =>
      val senderRef = sender()
      dbService.getInvestmentsByUser(userId).pipeTo(senderRef)

    case GetInvestmentsString(userId) =>
      val senderRef = sender()
      dbService.getInvestmentsByUserString(userId).pipeTo(senderRef)



    case DeleteInvestment(investmentId: Int, companyName: String) =>
      val senderRef = sender()
      dbService.deleteInvestment(investmentId, companyName).pipeTo(senderRef)



    case UpdateInvestment(investmentId: Int, companyName: String, newAmount: BigDecimal) =>
      val senderRef = sender()
      println(s" Mise à jour de l'investissement: $companyName (ID: $investmentId) → Nouveau montant: $newAmount")

      dbService.updateInvestment(investmentId, companyName, newAmount)
        .map(_ => " Mise à jour réussie")
        .pipeTo(senderRef) //

    case RecupererlaSomme(companyName, id, sommeInvesti) =>
      val senderRef = sender()
      println(s"📢 [InvestmentActor] Demande reçue pour récupérer $sommeInvesti de $companyName (User ID: $id)")

      val result = for {
        updateResult <- (self ? UpdateInvestment(id, companyName, 0)).mapTo[String]
        balance <- (actor ? UtilisateurActor.GetBalance1(id)).mapTo[BigDecimal]
        _ <- (actor ? UtilisateurActor.updateBalance(id, balance + sommeInvesti))
      } yield {
        println(s" [InvestmentActor] Somme de $sommeInvesti récupérée pour $companyName")
        " Somme récupérée avec succès"
      }

      result.pipeTo(senderRef)


  }
}
