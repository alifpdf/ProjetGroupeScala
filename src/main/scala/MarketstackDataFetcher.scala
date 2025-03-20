import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.json._
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object MarketstackDataFetcher {

  implicit val system: ActorSystem = ActorSystem("MarketstackDataFetcher")
  implicit val ec: ExecutionContext = system.dispatcher

  val apiKey = "8717ecfc3c4fff52643a4e0e9ad08873" //j'ai ajoute 4
  val baseUrl = "http://api.marketstack.com/v1"

  // Fonction pour récupérer les 6 dernières données de marché pour un symbole
  def getLastMarketPrices(symbol: String): Future[List[Double]] = {
    val endpoint = "eod"
    val today = LocalDate.now()
    val dateFrom = today.minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE)
    val url = s"$baseUrl/$endpoint?access_key=$apiKey&symbols=$symbol&date_from=$dateFrom&limit=6"

    for {
      response     <- Http().singleRequest(HttpRequest(uri = url))
      responseJson <- Unmarshal(response.entity).to[String]
    } yield {
      val json = Json.parse(responseJson)
      println(s"JSON Response: $json") // Debug output

      val data = (json \ "data").asOpt[JsArray]
      val multiplier = symbol match {
        case "BTC" => 100000
        case "ETH" => 10000
        case _     => 1
      }

      data match {
        case Some(arr) if arr.value.nonEmpty =>
          val sortedPrices = arr.value.map { item =>
            val date = (item \ "date").as[String]
            val close = (item \ "close").asOpt[Double].getOrElse(0.5) * (1.09 / 10)
            (date, close)
          }.sortBy(_._1).map(_._2 * multiplier).toList

          sortedPrices.take(6)

        case _ =>
          println(s"No data found for symbol: $symbol, initializing with default values.")
          List.fill(6)(0.5 * multiplier)
      }
    }
  }



  // Exemple d'appel pour récupérer les 6 dernières données de marché pour un symbole
  def getSymbolLastPrices(symbol: String): Unit = {
    getLastMarketPrices(symbol).map { prices =>
      println(s"6 derniers prix de marché pour $symbol (du plus ancien au plus récent) : $prices")
    }.recover {
      case ex: Exception => println(s"Erreur lors de la récupération des données de marché pour $symbol : ${ex.getMessage}")
    }
  }

  // Méthode main pour tester
  def main(args: Array[String]): Unit = {
    println(" Démarrage du test de récupération des données de marché...")

    // Appeler la fonction pour récupérer les 6 derniers prix de marché pour DOGE
    getSymbolLastPrices("BTC")

    // Attendre quelques secondes pour laisser le temps à la requête de s'exécuter
    Thread.sleep(5000)

    // Arrêter l'ActorSystem après le test
    system.terminate()
  }
}
