import akka.actor.{Actor, Props}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object InvestmentActor {
  case class AddInvestment(userId: Int, companyName: String, amountInvested: BigDecimal)
  case class GetInvestments(userId: Int)
  case class DeleteInvestment(investmentId: Int,companyName:String)
  case class UpdateInvestment(investmentId: Int, companyName:String,newAmount: BigDecimal)

  def props(dbService: DatabaseService1): Props = Props(new InvestmentActor(dbService))
}

class InvestmentActor(dbService: DatabaseService1) extends Actor {

  import InvestmentActor._

  def receive: Receive = {

    case AddInvestment(userId, companyName, amount) =>
      val sendeRef = sender()
      dbService.addInvestment(userId, companyName, amount) onComplete {
        case Success(_) =>sendeRef ! "Success ajoute"
          case Failure(e) =>sendeRef ! "Failure"
      }

    case GetInvestments(userId) =>
      val senderRef = sender()
      dbService.getInvestmentsByUser(userId).onComplete {
        case Success(investments) => senderRef ! investments

      }

    case DeleteInvestment(investmentId: Int,companyName:String) =>
      val senderRef = sender()
      dbService.deleteInvestment(investmentId,companyName).onComplete {
        case Success(_) => senderRef ! "Success"

      }

    case UpdateInvestment(investmentId: Int, companyName:String,newAmount: BigDecimal) =>
      val senderRef = sender()
      dbService.updateInvestment(investmentId,companyName,newAmount) onComplete {
        case Success(_) =>senderRef ! "Success oklm"
        case Failure(e) =>senderRef ! "Failure"
      }
  }
}
