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
                    originalPrice: BigDecimal
                  )

object Product {
  import play.api.libs.json._

  // Format personnalisé pour LocalDateTime
  implicit val localDateTimeFormat: Format[LocalDateTime] = new Format[LocalDateTime] {
    override def writes(o: LocalDateTime): JsValue = JsString(o.toString)
    override def reads(json: JsValue): JsResult[LocalDateTime] = json match {
      case JsString(s) => JsSuccess(LocalDateTime.parse(s))
      case _ => JsError("LocalDateTime expected")
    }
  }

  implicit val productFormat: OFormat[Product] = Json.format[Product]
  val tupled = (Product.apply _).tupled
}

// Définition de la table Products
class ProductsTable(tag: Tag) extends Table[Product](tag, "products") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def ownerId = column[Int]("owner_id")
  def investmentId = column[Int]("investment_id")
  def createdAt = column[LocalDateTime]("created_at")
  def originalPrice = column[BigDecimal]("original_price")

  // Clés étrangères
  def ownerFk = foreignKey("owner_fk", ownerId, UsersTable.table)(_.id, onDelete = ForeignKeyAction.Cascade)
  def investmentFk = foreignKey("investment_fk", investmentId, InvestmentsTable.table)(_.id, onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, ownerId, investmentId, createdAt, originalPrice) <> (Product.tupled, Product.unapply)
}

object ProductsTable {
  val table = TableQuery[ProductsTable]
}

// Gestion des produits avec Slick
class DBProducts(db: Database)(implicit ec: ExecutionContext) {
  // Création de la table
  def createTable(): Future[Unit] = db.run(ProductsTable.table.schema.createIfNotExists)

  // Ajouter un produit
  def addProduct(ownerId: Int, investmentId: Int, originalPrice: BigDecimal): Future[Int] = {
    println(s"📊 [DBProducts] Ajout d'un produit: Owner: $ownerId, Investment: $investmentId")
    val newProduct = Product(None, ownerId, investmentId, LocalDateTime.now(), originalPrice)
    db.run(ProductsTable.table += newProduct)
  }

  // Récupérer un produit par ID
  def getProduct(id: Int): Future[Option[Product]] = {
    println(s"📊 [DBProducts] Récupération du produit ID: $id")
    db.run(ProductsTable.table.filter(_.id === id).result.headOption)
  }

  // Récupérer tous les produits
  def getAllProducts(): Future[Seq[Product]] = {
    println(s"📊 [DBProducts] Récupération de tous les produits")
    db.run(ProductsTable.table.result)
  }

  // Récupérer tous les produits au format JSON
  def getAllProductsString(): Future[String] = {
    println(s"📊 [DBProducts] Récupération de tous les produits au format JSON")
    db.run(ProductsTable.table.result).map { products =>
      val json = Json.toJson(products.map(p => p.copy(id = p.id))) // Assure l'ID est préservé
      println(s"📌 JSON de tous les produits envoyé : $json") // Debugging
      Json.stringify(json)
    }
  }

  // Récupérer les produits par propriétaire
  def getProductsByOwner(ownerId: Int): Future[Seq[Product]] = {
    println(s"📊 [DBProducts] Récupération des produits du propriétaire ID: $ownerId")
    db.run(ProductsTable.table.filter(_.ownerId === ownerId).result)
  }

  // Récupérer les produits par propriétaire au format JSON
  def getProductsByOwnerString(ownerId: Int): Future[String] = {
    println(s"📊 [DBProducts] Récupération des produits du propriétaire ID: $ownerId au format JSON")
    db.run(ProductsTable.table.filter(_.ownerId === ownerId).result).map { products =>
      val json = Json.toJson(products.map(p => p.copy(id = p.id))) // Assure l'ID est préservé
      println(s"📌 JSON des produits du propriétaire envoyé : $json") // Debugging
      Json.stringify(json)
    }
  }

  // Récupérer les produits par investissement
  def getProductsByInvestment(investmentId: Int): Future[Seq[Product]] = {
    println(s"📊 [DBProducts] Récupération des produits liés à l'investissement ID: $investmentId")
    db.run(ProductsTable.table.filter(_.investmentId === investmentId).result)
  }

  // Récupérer les produits par investissement au format JSON
  def getProductsByInvestmentString(investmentId: Int): Future[String] = {
    println(s"📊 [DBProducts] Récupération des produits liés à l'investissement ID: $investmentId au format JSON")
    db.run(ProductsTable.table.filter(_.investmentId === investmentId).result).map { products =>
      val json = Json.toJson(products.map(p => p.copy(id = p.id))) // Assure l'ID est préservé
      println(s"📌 JSON des produits par investissement envoyé : $json") // Debugging
      Json.stringify(json)
    }
  }

  // Mettre à jour un produit
  def updateProduct(id: Int, ownerId: Int, investmentId: Int, originalPrice: BigDecimal): Future[Int] = {
    println(s"📊 [DBProducts] Mise à jour du produit ID: $id")
    db.run(ProductsTable.table.filter(_.id === id)
      .map(p => (p.ownerId, p.investmentId, p.originalPrice))
      .update((ownerId, investmentId, originalPrice)))
  }

  // Supprimer un produit
  def deleteProduct(id: Int): Future[Int] = {
    println(s"📊 [DBProducts] Suppression du produit ID: $id")
    db.run(ProductsTable.table.filter(_.id === id).delete)
  }

  // Recherche avancée de produits
  def searchProducts(ownerIdOpt: Option[Int] = None,
                     investmentIdOpt: Option[Int] = None,
                     minPrice: Option[BigDecimal] = None,
                     maxPrice: Option[BigDecimal] = None): Future[Seq[Product]] = {
    println(s"📊 [DBProducts] Recherche avancée de produits")

    var query = ProductsTable.table.filter(_ => true.bind)

    // Appliquer les filtres s'ils sont définis
    ownerIdOpt.foreach { ownerId =>
      query = query.filter(_.ownerId === ownerId)
    }

    investmentIdOpt.foreach { investmentId =>
      query = query.filter(_.investmentId === investmentId)
    }

    minPrice.foreach { price =>
      query = query.filter(_.originalPrice >= price)
    }

    maxPrice.foreach { price =>
      query = query.filter(_.originalPrice <= price)
    }

    db.run(query.result)
  }
}