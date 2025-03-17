import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.json._
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object MarketstackDataFetcher {

  implicit val system: ActorSystem = ActorSystem("MarketstackDataFetcher")
  implicit val ec: ExecutionContext = system.dispatcher

  val apiKey = "09a5355c357bc1e9b85c6a90ae687786"
  val baseUrl = "http://api.marketstack.com/v1"

  // Fonction pour récupérer les 6 dernières données de marché pour un symbole
  def getLastMarketPrices(symbol: String): Future[List[Double]] = {
    val endpoint = "eod"
    val today = LocalDate.now()
    val dateFrom = today.minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE) // Adjust as needed
    val url = s"$baseUrl/$endpoint?access_key=$apiKey&symbols=$symbol&date_from=$dateFrom&limit=6"

    Http().singleRequest(HttpRequest(uri = url)).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }.map { responseJson =>
      val json = Json.parse(responseJson)
      println(s"JSON Response: $json") // Print the JSON response for inspection
      val data = (json \ "data").asOpt[JsArray]

      data match {
        case Some(arr) if arr.value.nonEmpty =>
          val sortedPrices = arr.value.map { item =>
            val date = (item \ "date").as[String]
            val close = (item \ "close").asOpt[Double].getOrElse(0.5)/10 // Default to 0.5 if not available
            (date, close)
          }.sortBy(_._1).map(_._2).toList // Sort by date and extract prices

          sortedPrices.take(6) // Ensure only 6 prices are returned
        case _ =>
          println(s"No data found for symbol: $symbol, initializing with default values.")
          List.fill(6)(0.5) // Initialize with default values
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
    println("📩 Démarrage du test de récupération des données de marché...")

    // Appeler la fonction pour récupérer les 6 derniers prix de marché pour DOGE
    getSymbolLastPrices("DOGE")

    // Attendre quelques secondes pour laisser le temps à la requête de s'exécuter
    Thread.sleep(5000)

    // Arrêter l'ActorSystem après le test
    system.terminate()
  }
}
