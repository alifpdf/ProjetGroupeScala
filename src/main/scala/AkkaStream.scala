import Main.{dbService, ec, system, timeout, utilisateurActor, utilisateurActor2}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Merge, Sink, Source}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import play.api.libs.json.{JsNumber, JsObject, Json}

import scala.math.BigDecimal.RoundingMode
object AkkaStream {
  import scala.concurrent.{Future, ExecutionContext}
  import scala.util.Random
  import akka.pattern.ask
  import scala.concurrent.duration._

  var investments: Map[String, Int] = Map(
    "TechCorp" -> 25,
    "Google" -> 40,
    "Nasdaq" -> 60
  )
  def updateInvestmentByUsers(userId: Int, oldInvestments: Map[String, Int], updatedInvestments: Map[String, Int]): Future[Unit] = {
    println(s"🔄 Mise à jour des investissements pour l'utilisateur $userId...")

    (utilisateurActor2 ? InvestmentActor.GetInvestments(userId)).mapTo[Seq[Investment]].flatMap { investments =>
      println(s"📊 ${investments.size} investissements trouvés pour l'utilisateur $userId.")

      val updatedInvestmentFutures = investments.map { investment =>
        val oldPrice = BigDecimal(oldInvestments.getOrElse(investment.companyName, 1).toDouble)
        val newPrice = BigDecimal(updatedInvestments.getOrElse(investment.companyName, 1).toDouble)
        val amountInvested = BigDecimal(investment.amountInvested.toDouble)
        val newAmount = (amountInvested / oldPrice) * newPrice.setScale(2, RoundingMode.HALF_UP)

        println(s"📢 Mise à jour de ${investment.companyName} pour l'utilisateur $userId : $amountInvested → $newAmount")

        (utilisateurActor2 ? InvestmentActor.UpdateInvestment(userId, investment.companyName, newAmount)).mapTo[String]
      }

      Future.sequence(updatedInvestmentFutures).map(_ => ())
    }

  }

  def updateInvestment(): Future[String] = {
    println("📩 Démarrage de la mise à jour globale des investissements...")

    val oldInvestments = synchronized { investments }

    // Mettre à jour les investissements avec de nouveaux prix
    synchronized {
      investments = investments.map { case (company, price) =>
        val newPrice = Random.between(-30, 60) + price
        company -> newPrice
      }
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


  def generateUpdateSource()(implicit system: ActorSystem, ec: ExecutionContext): Source[Message, Any] = {
    Source.tick(1.second, 10.seconds, "").flatMapConcat { _ =>
      Source.future(
        updateInvestment().map { combinedJson =>
          TextMessage(combinedJson) // ✅ Envoie le JSON complet au frontend
        }
      )
    }
  }


  def generateRandomNumberSource()(implicit system: ActorSystem, ec: ExecutionContext): Source[Message, Any] = {
    Source.tick(1.second, 10.seconds, "").map { _ =>
      val jsonMessage = Json.obj(
        "type"  -> "random",
        "data"  -> JsNumber(BigDecimal(investments.getOrElse("TechCorp", 0).toString).toInt),  // ✅ Convertir en `Int`
      "data1" -> JsNumber(BigDecimal(investments.getOrElse("Google", 0).toString).toInt),
      "data2" -> JsNumber(BigDecimal(investments.getOrElse("Nasdaq", 0).toString).toInt)
      )

      TextMessage(jsonMessage.toString()) // ✅ Envoi sous forme de WebSocket Message
    }
  }


  def websocketFlow()(implicit system: ActorSystem, ec: ExecutionContext): Flow[Message, Message, Any] = {
    val combinedSource = Source.combine(
      generateRandomNumberSource(),
      generateUpdateSource()
    )(Merge(_)) // Utilisation ok si l'ordre n'est pas un problème
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
