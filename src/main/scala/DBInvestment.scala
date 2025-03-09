import play.api.libs.json.{Json, OFormat}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

// Définition du modèle d'investissement
case class Investment(id: Option[Int], userId: Int, companyName: String, amountInvested: BigDecimal)

object Investment {
  implicit val investmentFormat: OFormat[Investment] = Json.format[Investment]
  val tupled = (Investment.apply _).tupled
}

// Définition de la table des investissements
class InvestmentsTable(tag: Tag) extends Table[Investment](tag, "investments") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id")
  def companyName = column[String]("company_name")
  def amountInvested = column[BigDecimal]("amount_invested")

  def userFk = foreignKey("user_fk", userId, UsersTable.table)(_.id, onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, userId, companyName, amountInvested) <> (Investment.tupled, Investment.unapply)
}

object InvestmentsTable {
  val table = TableQuery[InvestmentsTable]
}

// Service de gestion des investissements
class DBInvestment(db: Database)(implicit ec: ExecutionContext) {

  // Ajouter un investissement
  def addInvestment(userId: Int, companyName: String, amountInvested: BigDecimal): Future[Int] = {
    val newInvestment = Investment(None, userId, companyName, amountInvested)
    db.run(InvestmentsTable.table += newInvestment)
  }


  // Récupérer les investissements d'un utilisateur
  def getInvestmentsByUser(userId: Int): Future[Seq[Investment]] = {
    db.run(InvestmentsTable.table.filter(_.userId === userId).result)
  }

  def getInvestmentsByUserString(userId: Int): Future[String] = {
    db.run(InvestmentsTable.table.filter(_.userId === userId).result).map { investments =>
      val json = Json.toJson(investments.map(i => i.copy(id = i.id))) // 🔥 Remplace `None` par `Some(0)`
      println(s"📌 JSON des investissements envoyé : $json") // Debugging
      Json.stringify(json)
    }
  }

  def getAllInvestmentsString: Future[String] = {
    db.run(InvestmentsTable.table.result).map { investments =>
      val json = Json.toJson(investments.map(i => i.copy(id = i.id))) // 🔥 Remplace `None` par `Some(0)`
      println(s"📌 JSON de tous les investissements envoyé : $json") // Debugging
      Json.stringify(json)
    }
  }






  // Supprimer un investissement
  def deleteInvestment(investmentId: Int,companyName:String): Future[Int] = {
    db.run(InvestmentsTable.table.filter(_.userId === investmentId).filter(_.companyName === companyName).delete)
  }

  def updateInvestment(investmentId: Int, companyName:String,newAmount: BigDecimal): Future[Int] = {
    db.run(InvestmentsTable.table.filter(_.userId === investmentId).
      filter(_.companyName===companyName).
      map(_.amountInvested).update(newAmount))
  }
}
