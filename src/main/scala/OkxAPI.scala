import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.json._

object OkxAPI {

  val apiKey = "751b37d3-e834-4dcf-9591-5afa39c8cee0"
  val secretKey = "3DAFC01A28E3D48271522D59BA0CBCF8"
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
    val finalCrypto = crypto match {
      case "BTC"      => "BTC-USDT"
      case "ETH"      => "ETH-USDT"
      case "DOGE"     => "DOGE-USDT"
      case other      => other // Si déjà formaté "BTC-USDT", "ETH-USDT", etc.
    }

    fetchCryptoPrices().map(_.getOrElse(finalCrypto, 0.0))
  }


  def main(args: Array[String]): Unit = {
    println("📩 Démarrage du test de récupération des prix des cryptos...")

    getPrices()

    getPrice("BTC").map { price =>
      val adjustedPrice = price / 1.09
      println(s"Prix actuel de BTC-USD : $adjustedPrice EUR USDT")
    }
    system.terminate()
  }
}
