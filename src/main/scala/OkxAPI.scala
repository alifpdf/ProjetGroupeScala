import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.util.ByteString
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.json._

object OkxAPI {

  val apiKey = "2b7bd3ab-1a0c-4d48-bae9-54f1dcc62e69"
  val secretKey = "E2B5EE85C2E604ACB51681AED8D51ED1"
  val passphrase = "CYTECH"  // Nom de la clé API
  val baseUrl = "https://www.okx.com/api/v5"

  implicit val system: ActorSystem = ActorSystem("OkxAPI")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Fonction pour signer la requête
  def signRequest(params: Map[String, String]): String = {
    val queryString = params.map { case (k, v) => s"$k=$v" }.mkString("&")
    val message = queryString.getBytes("UTF-8")
    val secretKeySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secretKeySpec)
    val signed = mac.doFinal(message)
    Base64.getEncoder.encodeToString(signed)
  }

  // Fonction pour récupérer les prix des cryptomonnaies via l'API OKX
  def fetchCryptoPrices()(implicit system: ActorSystem, ec: ExecutionContext): Future[Map[String, Double]] = {
    val endpoint = "/market/tickers"
    val url = s"$baseUrl$endpoint?instType=SPOT"

    // Faire la requête GET
    Http().singleRequest(HttpRequest(uri = url)).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }.map { responseJson =>
      val json = Json.parse(responseJson)
      val data = (json \ "data").as[Seq[JsObject]]

      // Filtrer les données pour ne garder que BTC, ETH, DOGE en USD
      val filteredData = data.filter { entry =>
        val instId = (entry \ "instId").as[String]
        instId == "BTC-USDT" || instId == "ETH-USDT" || instId == "DOGE-USDT"
      }

      // Créer un Map avec les prix des cryptomonnaies
      filteredData.map { entry =>
        val instId = (entry \ "instId").as[String]
        val lastPrice = (entry \ "last").as[String].toDouble
        instId -> lastPrice
      }.toMap
    }
  }

  // Exemple d'appel pour récupérer les prix des cryptos
  def getPrices(): Unit = {
    fetchCryptoPrices().map { prices =>
      println(s"Prix des cryptomonnaies : $prices")
    }.recover {
      case ex: Exception => println(s"Erreur lors de la récupération des prix : ${ex.getMessage}")
    }
  }

  // Fonction Getter pour obtenir le prix d'une crypto donnée
  def getPrice(crypto: String): Future[Double] = {
    fetchCryptoPrices().map { prices =>
      prices.getOrElse(crypto, 0.0)
    }
  }

  // Méthode main pour tester
  def main(args: Array[String]): Unit = {
    println("📩 Démarrage du test de récupération des prix des cryptos...")

    // Appeler la fonction pour récupérer les prix
    getPrices()

    // Exemple d'appel pour obtenir le prix de BTC
    getPrice("BTC-USDT").map { price =>
      println(s"Prix actuel de BTC-USD : $price USD")
    }.recover {
      case ex: Exception => println(s"Erreur lors de la récupération du prix de BTC : ${ex.getMessage}")
    }

    // Attendre quelques secondes pour laisser le temps à la requête de s'exécuter
    Thread.sleep(5000)

    // Arrêter l'ActorSystem après le test
    system.terminate()
  }
}
