import play.api.libs.json.{Json, OFormat}
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime

// Modèle de données pour la table "products"
case class Product(
                    id: Option[Int] = None,
                    ownerId: Int,
                    investmentId: Int,
                    createdAt: LocalDateTime = LocalDateTime.now(),
                    originalPrice: BigDecimal,
                    entreprise:String,
                    numshare:Int
                  )

object Product {
  import play.api.libs.json._



  implicit val productFormat: OFormat[Product] = Json.format[Product]
  val tupled = (Product.apply _).tupled
}

// Mapping Slick pour la table
class ProductsTable(tag: Tag) extends Table[Product](tag, "products") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def ownerId = column[Int]("owner_id")
  def investmentId = column[Int]("investment_id")
  def createdAt = column[LocalDateTime]("created_at")
  def originalPrice = column[BigDecimal]("original_price")
  def entreprise = column[String]("entreprise")
  def numshare = column[Int]("numshare")


  // Clés étrangères
  def ownerFk = foreignKey("owner_fk", ownerId, UsersTable.table)(_.id, onDelete = ForeignKeyAction.Cascade)
  def investmentFk = foreignKey("investment_fk", investmentId, InvestmentsTable.table)(_.id, onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, ownerId, investmentId, createdAt, originalPrice,entreprise,numshare) <> (Product.tupled, Product.unapply)
}

object ProductsTable {
  val table = TableQuery[ProductsTable]
}

// Gestion des produits avec Slick
class DBProducts(db: Database)(implicit ec: ExecutionContext) {

  // Ajouter un produit
  def addProduct(ownerId: Int, investmentId: Int, originalPrice: BigDecimal,entreprise:String,numshare:Int): Future[Int] = {
    println(s" [DBProducts] Ajout d'un produit: Owner: $ownerId, Investment: $investmentId")
    val newProduct = Product(None, ownerId, investmentId, LocalDateTime.now(), originalPrice,entreprise,numshare:Int)
    db.run(ProductsTable.table += newProduct)
  }

  // Récupérer un produit par ID
  def getProduct(id: Int): Future[Option[Product]] = {
    println(s" [DBProducts] Récupération du produit ID: $id")
    db.run(ProductsTable.table.filter(_.id === id).result.headOption)
  }

  // Récupérer tous les produits
  def getAllProducts(): Future[Seq[Product]] = {
    println(s" [DBProducts] Récupération de tous les produits")
    db.run(ProductsTable.table.result)
  }

  // Récupérer tous les produits au format JSON
  def getAllProductsString(): Future[String] = {
    println(s" [DBProducts] Récupération de tous les produits au format JSON")
    db.run(ProductsTable.table.result).map { products =>
      val json = Json.toJson(products.map(p => p.copy(id = p.id))) // Assure l'ID est préservé
      println(s" JSON de tous les produits envoyé : $json") // Debugging
      Json.stringify(json)
    }
  }

  // Récupérer les produits par propriétaire
  def getProductsByOwner(ownerId: Int): Future[Seq[Product]] = {
    println(s" [DBProducts] Récupération des produits du propriétaire ID: $ownerId")
    db.run(ProductsTable.table.filter(_.ownerId === ownerId).result)

  }

  // Récupérer les produits par propriétaire au format JSON
  def getProductsByOwnerString(ownerId: Int): Future[String] = {
    println(s" [DBProducts] Récupération des produits du propriétaire ID: $ownerId au format JSON")
    db.run(ProductsTable.table.filter(_.ownerId === ownerId).result).map { products =>
      val json = Json.toJson(products.map(p => p.copy(id = p.id))) // Assure l'ID est préservé
      println(s" JSON des produits du propriétaire envoyé : $json") // Debugging
      Json.stringify(json)
    }
  }

  // Récupérer les produits par investissement
  def getProductsByInvestment(investmentId: Int): Future[Seq[Product]] = {
    println(s" [DBProducts] Récupération des produits liés à l'investissement ID: $investmentId")
    db.run(ProductsTable.table.filter(_.investmentId === investmentId).result)
  }

  // Récupérer les produits par investissement au format JSON
  def getProductsByInvestmentString(investmentId: Int): Future[String] = {
    println(s" [DBProducts] Récupération des produits liés à l'investissement ID: $investmentId au format JSON")
    db.run(ProductsTable.table.filter(_.investmentId === investmentId).result).map { products =>
      val json = Json.toJson(products.map(p => p.copy(id = p.id))) // Assure l'ID est préservé
      println(s" JSON des produits par investissement envoyé : $json") // Debugging
      Json.stringify(json)
    }
  }


}