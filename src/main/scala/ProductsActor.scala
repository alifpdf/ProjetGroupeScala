import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.pipe
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ProductsActor {
  case class AddProduct(ownerId: Int, investmentId: Int, originalPrice: BigDecimal,entreprise:String,numshare:Int)
  case class GetProduct(productId: Int)
  case class GetAllProducts()
  case class GetProductsByOwner(ownerId: Int)
  case class GetProductsByInvestment(investmentId: Int)
  case class GetProductsString()
  case class GetProductsByOwnerString(ownerId: Int)
  case class GetProductsByInvestmentString(investmentId: Int)

  def props(dbService: DBProducts): Props = Props(new ProductsActor(dbService))
}

class ProductsActor(dbService: DBProducts) extends Actor {
  import ProductsActor._
  import context.dispatcher

  def receive: Receive = {
    case AddProduct(ownerId, investmentId, originalPrice,entreprise,numshare) =>
      val originalSender = sender()
      println(s"ProductsActor: Ajout produit: Owner=$ownerId, Investment=$investmentId, Price=$originalPrice €")
      dbService.addProduct(ownerId, investmentId, originalPrice,entreprise,numshare).map { productId =>
        s"Produit ajouté avec ID: $productId"
      }.pipeTo(originalSender)

    case GetProduct(productId) =>
      val originalSender = sender()
      println(s"ProductsActor: Récupération produit ID: $productId")
      dbService.getProduct(productId).map {
        case Some(product) => product
      }.pipeTo(originalSender)

    case GetAllProducts() =>
      val originalSender = sender()
      println(s"ProductsActor: Récupération de tous les produits")
      dbService.getAllProducts().map { products =>
        println(s" ${products.size} produits récupérés")
        products
      }.pipeTo(originalSender)

    case GetProductsString() =>
      val originalSender = sender()
      println(s"ProductsActor: Récupération produits JSON")
      dbService.getAllProductsString().pipeTo(originalSender)

    case GetProductsByOwner(ownerId) =>
      val originalSender = sender()
      println(s"ProductsActor: Produits par Owner ID: $ownerId")

      dbService.getProductsByOwner(ownerId).pipeTo(originalSender)

    case GetProductsByOwnerString(ownerId) =>
      val originalSender = sender()
      println(s"ProductsActor: Produits JSON par Owner ID: $ownerId")
      dbService.getProductsByOwnerString(ownerId).pipeTo(originalSender)

    case GetProductsByInvestment(investmentId) =>
      val originalSender = sender()
      println(s"ProductsActor: Produits par Investment ID: $investmentId")
      dbService.getProductsByInvestment(investmentId).pipeTo(originalSender)

    case GetProductsByInvestmentString(investmentId) =>
      val originalSender = sender()
      println(s"ProductsActor: Produits JSON par Investment ID: $investmentId")
      dbService.getProductsByInvestmentString(investmentId).pipeTo(originalSender)


  }
}
