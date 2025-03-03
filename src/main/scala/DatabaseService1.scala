import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}

// Définition du modèle d'investissement
case class Investment(id: Option[Int], userId: Int, companyName: String, amountInvested: BigDecimal)

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
class DatabaseService1(db: Database)(implicit ec: ExecutionContext) {

  // Ajouter un investissement
  def addInvestment(userId: Int, companyName: String, amountInvested: BigDecimal): Future[Int] = {
    val newInvestment = Investment(None, userId, companyName, amountInvested)
    db.run(InvestmentsTable.table += newInvestment)
  }


  // Récupérer les investissements d'un utilisateur
  def getInvestmentsByUser(userId: Int): Future[Seq[Investment]] = {
    db.run(InvestmentsTable.table.filter(_.userId === userId).result)
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
