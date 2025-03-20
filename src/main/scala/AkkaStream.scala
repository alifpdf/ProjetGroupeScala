import java.util.concurrent.atomic.AtomicReference
import Main.{ec, timeout, utilisateurActor, utilisateurActor2}
import OkxAPI.getPrice
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Merge, Sink, Source}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import play.api.libs.json.{JsNumber, Json}
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.math.BigDecimal.RoundingMode
import akka.pattern.ask

object AkkaStream {

  // ✅ investments thread-safe AtomicReference
  val investments: AtomicReference[Map[String, Double]] = new AtomicReference(Map(
    "BTC-USD" -> 4.0,
    "ETH-USD" -> 3.0,
    "DOGE-USD" -> 1.0
  ))

  val baseUrl = "https://www.okx.com/api/v5"

  def updateInvestmentByUsers(userId: Int, oldInvestments: Map[String, Double], updatedInvestments: Map[String, Double]): Future[Unit] = {

    println("old"+oldInvestments)
    println("updated"+updatedInvestments)
    println(s"🔄 Mise à jour des investissements pour l'utilisateur $userId...")

    (utilisateurActor2 ? InvestmentActor.GetInvestments(userId)).mapTo[Seq[Investment]].flatMap { investmentsList =>
      println(s"📊 ${investmentsList.size} investissements trouvés pour l'utilisateur $userId.")

      val updatedInvestmentFutures = investmentsList.flatMap { investment =>
        val companyKey = investment.companyName match {
          case "BTC" => "BTC-USD"
          case "ETH" => "ETH-USD"
          case "DOGE" => "DOGE-USD"
          case other =>
            println(s"⚠️ Entreprise inconnue ignorée : $other")
            ""
        }

        if (companyKey.nonEmpty) {
          val oldPrice = BigDecimal(oldInvestments.getOrElse(companyKey, 1.0))
          val newPrice = BigDecimal(updatedInvestments.getOrElse(companyKey, 1.0))
          val amountInvested = BigDecimal(investment.amountInvested.intValue)

          // 🔥 Calcul basé sur la quantité d'actions
          val numberOfShares = amountInvested / oldPrice
          val newAmount = (numberOfShares * newPrice).setScale(2, RoundingMode.HALF_UP)


          if (newAmount != amountInvested) {
            println(s"📢 Mise à jour de ${investment.companyName} pour l'utilisateur $userId : $amountInvested → $newAmount")
            Some((utilisateurActor2 ? InvestmentActor.UpdateInvestment(userId, investment.companyName, newAmount)).mapTo[String])
          } else {
            println(s"✅ Aucune variation sur ${investment.companyName}, skip.")
            None
          }
        } else None
      }

      Future.sequence(updatedInvestmentFutures).map(_ => ())
    }
  }

  def updateInvestment(): Future[String] = {
    println("📩 Démarrage de la mise à jour globale des investissements...")

    // ✅ Capture l'ancien snapshot AVANT d'appeler les nouveaux prix
    val oldInvestments = investments.get()

    for {
      btcPrice <- getPrice("BTC-USDT")
      ethPrice <- getPrice("ETH-USDT")
      dogePrice <- getPrice("DOGE-USDT")

      _ = {
        val updatedMap = oldInvestments.updated("BTC-USD", btcPrice)
          .updated("ETH-USD", ethPrice)
          .updated("DOGE-USD", dogePrice)
        investments.set(updatedMap)
        println(s"✅ Prix mis à jour : BTC: $btcPrice, ETH: $ethPrice, DOGE: $dogePrice")
      }

      users <- (utilisateurActor ? UtilisateurActor.GetUsers).mapTo[Seq[User]]
      _ = println(s"✅ ${users.size} utilisateurs trouvés. Début de la mise à jour utilisateur...")

      // ✅ Ici tu passes bien old et updated
      _ <- Future.sequence(users.flatMap { user =>
        user.id.map { userId =>
          updateInvestmentByUsers(userId, oldInvestments, investments.get())
        }
      })

      usersJson <- (utilisateurActor ? UtilisateurActor.GetStringUsers).mapTo[String]
      investmentsJson <- (utilisateurActor2 ? InvestmentActor.GetAllInvestmentsString).mapTo[String]
    } yield {
      val combinedJson =
        s"""
           |{
           |  "type": "update",
           |  "users": $usersJson,
           |  "investments": $investmentsJson
           |}
           |""".stripMargin
      println(s"📌 JSON combiné envoyé : $combinedJson")
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
      val current = investments.get()
      val jsonMessage = Json.obj(
        "type" -> "random",
        "data" -> JsNumber(BigDecimal(current.getOrElse("BTC-USD", 0.0)).setScale(2, RoundingMode.HALF_UP)),
        "data1" -> JsNumber(BigDecimal(current.getOrElse("ETH-USD", 0.0)).setScale(2, RoundingMode.HALF_UP)),
        "data2" -> JsNumber(BigDecimal(current.getOrElse("DOGE-USD", 0.0)).setScale(2, RoundingMode.HALF_UP))
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

  def update1()(): Unit = {
    implicit val system: ActorSystem = ActorSystem("InvestmentUpdater")
    val updateSource = Source.tick(0.seconds, 5.seconds, "update")
    val updateFlow = Flow[String].mapAsync(1) { _ =>
      println("🔄 Début de la mise à jour des investissements...")
      updateInvestment()
    }
    val runnableGraph = updateSource.via(updateFlow).to(Sink.ignore)
    runnableGraph.run()
  }
}