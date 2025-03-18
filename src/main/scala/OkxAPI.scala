import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.json._

object OkxAPI {

  val apiKey = "2b7bd3ab-1a0c-4d48-bae9-54f1dcc62e69"
  val secretKey = "E2B5EE85C2E604ACB51681AED8D51ED1"
  val baseUrl = "https://www.okx.com/api/v5"

  implicit val system: ActorSystem = ActorSystem("OkxAPI")
  implicit val ec: ExecutionContext = system.dispatcher




  def fetchCryptoPrices(): Future[Map[String, Double]] = {
    val endpoint = "/market/tickers"
    val url = s"$baseUrl$endpoint?instType=SPOT"

    val responseFuture = Http().singleRequest(HttpRequest(uri = url))

    for {
      response <- responseFuture
      responseJson <- Unmarshal(response.entity).to[String]
      data = (Json.parse(responseJson) \ "data").as[Seq[JsObject]]
    } yield {
      data.collect {
        case entry if Seq("BTC-USDT", "ETH-USDT", "DOGE-USDT").contains((entry \ "instId").as[String]) =>
          (entry \ "instId").as[String] -> (entry \ "last").as[String].toDouble
      }.toMap
    }
  }


  def getPrices(): Unit = {
    fetchCryptoPrices().map { prices =>
      println(s"Prix des cryptomonnaies : $prices")
    }.recover {
      case ex: Exception => println(s"Erreur lors de la récupération des prix : ${ex.getMessage}")
    }
  }

  def getPrice(crypto: String): Future[Double] = {
    fetchCryptoPrices().map(_.getOrElse(crypto, 0.0))
  }

  def main(args: Array[String]): Unit = {
    println("📩 Démarrage du test de récupération des prix des cryptos...")

    getPrices()

    getPrice("BTC-USDT").map { price =>
      val adjustedPrice = price / 1.09
      println(s"Prix actuel de BTC-USD : $adjustedPrice EUR USDT")
    }.recover {
      case ex: Exception => println(s"Erreur lors de la récupération du prix de BTC : ${ex.getMessage}")
    }

    Thread.sleep(5000)
    system.terminate()
  }
}
