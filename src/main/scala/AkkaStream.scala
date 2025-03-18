import Main.{ ec, timeout, utilisateurActor, utilisateurActor2}
import OkxAPI.getPrice
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Merge, Sink, Source}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import play.api.libs.json.{JsNumber, Json}


import scala.math.BigDecimal.RoundingMode


object AkkaStream {
  import scala.concurrent.{Future, ExecutionContext}
  import akka.pattern.ask
  import scala.concurrent.duration._

  var investments: Map[String, Double] = Map(
    "BTC-USD" -> 4,
    "ETH-USD" -> 3,
    "DOGE-USD" -> 1
  )

  val baseUrl = "https://www.okx.com/api/v5"



  // Mise à jour des investissements avec les prix récupérés de l'API
  def updateInvestmentByUsers(userId: Int, oldInvestments: Map[String, Double], updatedInvestments: Map[String, Double]): Future[Unit] = {
    println(s"🔄 Mise à jour des investissements pour l'utilisateur $userId...")

    (utilisateurActor2 ? InvestmentActor.GetInvestments(userId)).mapTo[Seq[Investment]].flatMap { investments =>
      println(s"📊 ${investments.size} investissements trouvés pour l'utilisateur $userId.")

      val updatedInvestmentFutures = investments.map { investment =>
        val oldPrice = BigDecimal(oldInvestments.getOrElse(investment.companyName, 1.0).toDouble)
        val newPrice = BigDecimal(updatedInvestments.getOrElse(investment.companyName, 1.0).toDouble)
        val amountInvested = BigDecimal(investment.amountInvested.toDouble)
        val newAmount = (amountInvested / oldPrice) * newPrice.setScale(2, RoundingMode.HALF_UP)

        println(s"📢 Mise à jour de ${investment.companyName} pour l'utilisateur $userId : $amountInvested → $newAmount")

        (utilisateurActor2 ? InvestmentActor.UpdateInvestment(userId, investment.companyName, newAmount)).mapTo[String]
      }

      Future.sequence(updatedInvestmentFutures).map(_ => ())
    }

  }

  // Mise à jour des investissements avec les prix actuels des cryptomonnaies
  def updateInvestment(): Future[String] = {
    println("📩 Démarrage de la mise à jour globale des investissements...")

    // Récupérer les anciens investissements de manière sécurisée
    val oldInvestments = synchronized { investments }

    // Mettre à jour les investissements avec les nouveaux prix
    // Mettre à jour les investissements avec les nouveaux prix via les getters
    for {
      btcPrice <- getPrice("BTC-USDT")
      ethPrice <- getPrice("ETH-USDT")
      dogePrice <- getPrice("DOGE-USDT")
    } yield {
      synchronized {
        investments = investments.map {
          case ("BTC-USD", price) => "BTC-USD" -> btcPrice/10000.0
          case ("ETH-USD", price) => "ETH-USD" -> ethPrice/1000.0
          case ("DOGE-USD", price) => "DOGE-USD" -> dogePrice
          case (company, price) => company -> price // Garder l'ancien prix pour les autres entreprises si nécessaire
        }
      }

      println(s"Prix mis à jour : BTC: $btcPrice, ETH: $ethPrice, DOGE: $dogePrice")
    }
   


    // Récupérer tous les utilisateurs
    (utilisateurActor ? UtilisateurActor.GetUsers).mapTo[Seq[User]].flatMap { users =>
      println(s"✅ ${users.size} utilisateurs trouvés. Début de la mise à jour utilisateur...")

      val userUpdateFutures = users.flatMap { user =>
        user.id.map { userId =>
          updateInvestmentByUsers(userId, oldInvestments, synchronized { investments })
        }
      }

      // Attendre que toutes les mises à jour des utilisateurs soient terminées
      Future.sequence(userUpdateFutures).flatMap { _ =>
        println("✅ Mise à jour complète des investissements terminée.")
        for {
          usersJson <- (utilisateurActor ? UtilisateurActor.GetStringUsers).mapTo[String]
          investmentsJson <- (utilisateurActor2 ? InvestmentActor.GetAllInvestmentsString).mapTo[String]
        } yield {
          println(s"📢 JSON final des investissements avant envoi : $investmentsJson")
          // 🔹 Combiner les deux JSON en un seul
          val combinedJson = s"""
          {
            "type": "update",
            "users": $usersJson,
            "investments": $investmentsJson
          }
        """
          println(s"📌 JSON combiné envoyé : $combinedJson") // Debug
          combinedJson
        }
      }
    }
  }

  // Source pour générer les mises à jour
  def generateUpdateSource()(implicit system: ActorSystem, ec: ExecutionContext): Source[Message, Any] = {
    Source.tick(1.second, 10.seconds, "").flatMapConcat { _ =>
      Source.future(
        updateInvestment().map { combinedJson =>
          TextMessage(combinedJson) // ✅ Envoie le JSON complet au frontend
        }
      )
    }
  }

  // Source pour les messages WebSocket
  def generateRandomNumberSource()(implicit system: ActorSystem, ec: ExecutionContext): Source[Message, Any] = {
    Source.tick(1.second, 10.seconds, "").map { _ =>
      val jsonMessage = Json.obj(
        "type"  -> "random",
        "data" -> JsNumber(BigDecimal(investments.getOrElse("BTC-USD", 0.0)).setScale(2, BigDecimal.RoundingMode.HALF_UP)),
        "data1" -> JsNumber(BigDecimal(investments.getOrElse("ETH-USD", 0.0)).setScale(2, BigDecimal.RoundingMode.HALF_UP)),
        "data2" -> JsNumber(BigDecimal(investments.getOrElse("DOGE-USD", 0.0)).setScale(2, BigDecimal.RoundingMode.HALF_UP))
      )

      TextMessage(jsonMessage.toString()) // ✅ Envoi sous forme de WebSocket Message
    }
  }

  // Créer un flow WebSocket combiné
  def websocketFlow()(implicit system: ActorSystem, ec: ExecutionContext): Flow[Message, Message, Any] = {
    val combinedSource = Source.combine(
      generateRandomNumberSource(),
      generateUpdateSource()
    )(Merge(_)) // Utilisation ok si l'ordre n'est pas un problème
    Flow.fromSinkAndSource(Sink.ignore, combinedSource)
  }

  // Fonction pour mettre à jour les investissements à intervalle régulier
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
