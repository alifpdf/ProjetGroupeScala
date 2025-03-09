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
  case object GetAllInvestmentsString

  def props(dbService: DBInvestment, actor:ActorRef): Props = Props(new InvestmentActor(dbService,actor))
}

class InvestmentActor(dbService: DBInvestment,actor: ActorRef) extends Actor {

  import InvestmentActor._
  import context._

  def receive: Receive = {

    case AddInvestment(userId, companyName, amount) =>
      val senderRef = sender() // Sauvegarde du sender

      val result = for {
        // 🔹 Étape 1 : Récupérer le solde actuel de l'utilisateur depuis `UtilisateurActor`
        balance <- (actor ? UtilisateurActor.GetBalance1(userId)).mapTo[BigDecimal]
        newBalance = balance - amount

        // 🔹 Vérification du solde
        _ <- if (newBalance < 0) {
          println(s"❌ [InvestmentActor] Solde insuffisant pour investir $amount €")
          Future.successful(senderRef ! "Échec : Solde insuffisant") // ✅ Répond immédiatement
        } else {
          (actor ? UtilisateurActor.updateBalance(userId, newBalance)).map(_ => ())
        }

        _ = println(s"✅ [InvestmentActor] Nouveau solde après investissement : $newBalance €")

        // 🔹 Étape 3 : Ajouter l’investissement en base de données
        _ <- dbService.addInvestment(userId, companyName, amount)

      } yield {
        println(s"✅ [InvestmentActor] Investissement ajouté pour $companyName")
        senderRef ! "✅ Succès : Investissement ajouté" // ✅ Répond immédiatement après l'ajout
      }
      result




    case GetAllInvestmentsString =>
      val senderRef = sender()
      dbService.getAllInvestmentsString.onComplete {
        case Success(investmentsJson) =>
          println(s"📌 Envoi de tous les investissements au frontend : $investmentsJson")
          senderRef ! investmentsJson
        case Failure(e) =>
          println(s"❌ Erreur lors de la récupération des investissements : ${e.getMessage}")
          senderRef ! "[]"
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
      val senderRef = sender() // 🔥 Capture du sender pour lui répondre
      println(s"📢 Mise à jour de l'investissement: $companyName (ID: $investmentId) → Nouveau montant: $newAmount")

      dbService.updateInvestment(investmentId, companyName, newAmount)
        .map(_ => "✅ Mise à jour réussie")
        .recover { case e => s"❌ Échec de la mise à jour: ${e.getMessage}" }
        .pipeTo(senderRef) // ✅ Envoie la réponse à `senderRef` automatiquement



    case RecupererlaSomme(companyName, id, sommeInvesti) =>
      val senderRef = sender()
      println(s"📢 [InvestmentActor] Demande reçue pour récupérer $sommeInvesti de $companyName (User ID: $id)")

      val result = for {
        updateResult <- (self ? UpdateInvestment(id, companyName, 0)).mapTo[String]
        balance <- (actor ? UtilisateurActor.GetBalance1(id)).mapTo[BigDecimal]
        _ <- (actor ? UtilisateurActor.updateBalance(id, balance + sommeInvesti))
      } yield {
        println(s"✅ [InvestmentActor] Somme de $sommeInvesti récupérée pour $companyName")
        "✅ Somme récupérée avec succès"
      }

      result.pipeTo(senderRef) // ✅ Envoie une réponse correcte
      // ✅ Envoie la réponse à `senderRef`



  }
}
