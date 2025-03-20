import Main.{ec, timeout, utilisateurActor, utilisateurActor2}
import OkxAPI.getPrice
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Merge, Sink, Source}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import play.api.libs.json.{JsNumber, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.math.BigDecimal.{RoundingMode, double2bigDecimal}
import akka.pattern.ask

object AkkaStream {

  //Remplacement de AtomicReference par une Map mutable
  var investments: Map[String, Double] = Map(
    "BTC-USD" -> 4.0,
    "ETH-USD" -> 3.0,
    "DOGE-USD" -> 1.0
  )

  val baseUrl = "https://www.okx.com/api/v5"

  def updateInvestmentByUsers(userId: Int, oldInvestments: Map[String, Double], updatedInvestments: Map[String, Double]): Future[Unit] = {
    println("old " + oldInvestments)
    println("updated " + updatedInvestments)
    println(s"Mise à jour des investissements pour l'utilisateur $userId...")

    (utilisateurActor2 ? InvestmentActor.GetInvestments(userId)).mapTo[Seq[Investment]].flatMap { investmentsList =>
      println(s"${investmentsList.size} investissements trouvés pour l'utilisateur $userId.")

      val updatedInvestmentFutures = investmentsList.flatMap { investment =>
        val companyKey = investment.companyName match {
          case "BTC"  => "BTC-USD"
          case "ETH"  => "ETH-USD"
          case "DOGE" => "DOGE-USD"
          case other =>
            println(s"Entreprise inconnue ignorée : $other")
            ""
        }

        if (companyKey.nonEmpty) {
          val oldPrice = oldInvestments.getOrElse(companyKey, 1.0)
          val newPrice = updatedInvestments.getOrElse(companyKey, 1.0)
          val amountInvested = investment.amountInvested.intValue

          val numberOfShares = amountInvested / oldPrice
          Some((utilisateurActor2 ? InvestmentActor.UpdateInvestment(
            userId, investment.companyName, (numberOfShares * newPrice).setScale(2, RoundingMode.HALF_UP))
            ).mapTo[String])
        } else None
      }

      Future.sequence(updatedInvestmentFutures).map(_ => ())
    }
  }

  def updateInvestment(): Future[String] = {
    println("Démarrage de la mise à jour globale des investissements...")

    // Snapshot de la map
    val oldInvestments = investments

    for {
      btcPrice <- getPrice("BTC-USDT")
      ethPrice <- getPrice("ETH-USDT")
      dogePrice <- getPrice("DOGE-USDT")

      _ = {
        // Mise à jour de la map directement
        investments = investments.updated("BTC-USD", btcPrice)
          .updated("ETH-USD", ethPrice)
          .updated("DOGE-USD", dogePrice)
        println(s"Prix mis à jour : BTC: $btcPrice, ETH: $ethPrice, DOGE: $dogePrice")
      }

      users <- (utilisateurActor ? UtilisateurActor.GetUsers).mapTo[Seq[User]]
      _ = println(s"${users.size} utilisateurs trouvés. Début de la mise à jour utilisateur...")

      _ <- Future.sequence(users.flatMap { user =>
        user.id.map { userId =>
          updateInvestmentByUsers(userId, oldInvestments, investments)
        }
      })

      usersJson <- (utilisateurActor ? UtilisateurActor.GetStringUsers).mapTo[String]
      investmentsJson <- (utilisateurActor2 ? InvestmentActor.GetAllInvestmentsString).mapTo[String]
    } yield {
      val combinedJson = s"""{"type":"update","users":$usersJson,"investments":$investmentsJson}"""

      println(s"JSON combiné envoyé : $combinedJson")
      combinedJson
    }
  }

  def generateUpdateSource()(implicit system: ActorSystem, ec: ExecutionContext): Source[Message, Any] = {
    Source.tick(1.second, 10.seconds, "").flatMapConcat { _ =>
      Source.future(
        updateInvestment().map { combinedJson =>
          TextMessage(combinedJson)
        }
      )
    }
  }

  def generateRandomNumberSource()(implicit system: ActorSystem, ec: ExecutionContext): Source[Message, Any] = {
    Source.tick(1.second, 10.seconds, "").map { _ =>
      val current = investments
      val jsonMessage = Json.obj(
        "type" -> "random",
        "data" -> JsNumber(BigDecimal(current.getOrElse("BTC-USD", 0.0))),
        "data1" -> JsNumber(BigDecimal(current.getOrElse("ETH-USD", 0.0))),
        "data2" -> JsNumber(BigDecimal(current.getOrElse("DOGE-USD", 0.0)))
      )
      TextMessage(jsonMessage.toString())
    }
  }

  def websocketFlow()(implicit system: ActorSystem, ec: ExecutionContext): Flow[Message, Message, Any] = {
    val combinedSource = Source.combine(
      generateRandomNumberSource(),
      generateUpdateSource()
    )(Merge(_))
    Flow.fromSinkAndSource(Sink.ignore, combinedSource)
  }

}
