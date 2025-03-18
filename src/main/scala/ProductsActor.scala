import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.{ask, pipe}
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ProductsActor {
  // Messages pour interagir avec l'acteur
  case class AddProduct(ownerId: Int, investmentId: Int, originalPrice: BigDecimal)
  case class GetProduct(productId: Int)
  case class GetAllProducts()
  case class GetProductsByOwner(ownerId: Int)
  case class GetProductsByInvestment(investmentId: Int)
  case class UpdateProduct(productId: Int, ownerId: Int, investmentId: Int, originalPrice: BigDecimal)
  case class DeleteProduct(productId: Int)
  case class GetProductsString()
  case class GetProductsByOwnerString(ownerId: Int)
  case class GetProductsByInvestmentString(investmentId: Int)

  // Factory pour créer l'acteur
  def props(dbService: DBProducts): Props = Props(new ProductsActor(dbService))
}

class ProductsActor(dbService: DBProducts) extends Actor {
  import ProductsActor._
  import context._

  def receive: Receive = {
    case AddProduct(ownerId, investmentId, originalPrice) =>
      val senderRef = sender()
      println(s"📢 [ProductsActor] Ajout d'un nouveau produit: Owner: $ownerId, Investment: $investmentId, Prix: $originalPrice €")

      dbService.addProduct(ownerId, investmentId, originalPrice).map { productId =>
        println(s"✅ [ProductsActor] Produit ajouté avec ID: $productId")
        s"✅ Succès : Produit ajouté avec ID: $productId"
      }.recover {
        case e =>
          println(s"❌ [ProductsActor] Erreur lors de l'ajout du produit: ${e.getMessage}")
          s"❌ Échec : ${e.getMessage}"
      }.pipeTo(senderRef)

    case GetProduct(productId) =>
      val senderRef = sender()
      println(s"📢 [ProductsActor] Récupération du produit avec ID: $productId")

      dbService.getProduct(productId).map {
        case Some(product) =>
          println(s"✅ [ProductsActor] Produit trouvé: $product")
          product
        case None =>
          println(s"❓ [ProductsActor] Produit non trouvé avec ID: $productId")
          "Produit non trouvé"
      }.pipeTo(senderRef)

    case GetAllProducts() =>
      val senderRef = sender()
      println(s"📢 [ProductsActor] Récupération de tous les produits")

      dbService.getAllProducts().map { products =>
        println(s"✅ [ProductsActor] ${products.size} produits récupérés")
        products
      }.pipeTo(senderRef)

    case GetProductsString() =>
      val senderRef = sender()
      println(s"📢 [ProductsActor] Récupération de tous les produits au format JSON")

      dbService.getAllProductsString().map { json =>
        println(s"✅ [ProductsActor] Produits convertis en JSON")
        json
      }.pipeTo(senderRef)

    case GetProductsByOwner(ownerId) =>
      val senderRef = sender()
      println(s"📢 [ProductsActor] Récupération des produits pour le propriétaire ID: $ownerId")

      dbService.getProductsByOwner(ownerId).map { products =>
        println(s"✅ [ProductsActor] ${products.size} produits récupérés pour le propriétaire ID: $ownerId")
        products
      }.pipeTo(senderRef)

    case GetProductsByOwnerString(ownerId) =>
      val senderRef = sender()
      println(s"📢 [ProductsActor] Récupération des produits au format JSON pour le propriétaire ID: $ownerId")

      dbService.getProductsByOwnerString(ownerId).map { json =>
        println(s"✅ [ProductsActor] Produits au format JSON récupérés pour le propriétaire ID: $ownerId")
        json
      }.pipeTo(senderRef)

    case GetProductsByInvestment(investmentId) =>
      val senderRef = sender()
      println(s"📢 [ProductsActor] Récupération des produits liés à l'investissement ID: $investmentId")

      dbService.getProductsByInvestment(investmentId).map { products =>
        println(s"✅ [ProductsActor] ${products.size} produits récupérés pour l'investissement ID: $investmentId")
        products
      }.pipeTo(senderRef)

    case GetProductsByInvestmentString(investmentId) =>
      val senderRef = sender()
      println(s"📢 [ProductsActor] Récupération des produits au format JSON pour l'investissement ID: $investmentId")

      dbService.getProductsByInvestmentString(investmentId).map { json =>
        println(s"✅ [ProductsActor] Produits au format JSON récupérés pour l'investissement ID: $investmentId")
        json
      }.pipeTo(senderRef)

    case UpdateProduct(productId, ownerId, investmentId, originalPrice) =>
      val senderRef = sender()
      println(s"📢 [ProductsActor] Mise à jour du produit ID: $productId")

      dbService.updateProduct(productId, ownerId, investmentId, originalPrice).map { count =>
        if (count > 0) {
          println(s"✅ [ProductsActor] Produit ID: $productId mis à jour")
          s"✅ Succès : Produit mis à jour"
        } else {
          println(s"❓ [ProductsActor] Produit ID: $productId non trouvé pour mise à jour")
          s"❓ Produit non trouvé"
        }
      }.recover {
        case e =>
          println(s"❌ [ProductsActor] Erreur lors de la mise à jour du produit: ${e.getMessage}")
          s"❌ Échec : ${e.getMessage}"
      }.pipeTo(senderRef)

    case DeleteProduct(productId) =>
      val senderRef = sender()
      println(s"📢 [ProductsActor] Suppression du produit ID: $productId")

      dbService.deleteProduct(productId).map { count =>
        if (count > 0) {
          println(s"✅ [ProductsActor] Produit ID: $productId supprimé")
          s"✅ Succès : Produit supprimé"
        } else {
          println(s"❓ [ProductsActor] Produit ID: $productId non trouvé pour suppression")
          s"❓ Produit non trouvé"
        }
      }.recover {
        case e =>
          println(s"❌ [ProductsActor] Erreur lors de la suppression du produit: ${e.getMessage}")
          s"❌ Échec : ${e.getMessage}"
      }.pipeTo(senderRef)
  }
}