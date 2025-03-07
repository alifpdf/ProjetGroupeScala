import Main.{dbService, ec, system, timeout, utilisateurActor, utilisateurActor2}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Merge, Sink, Source}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import play.api.libs.json.Json

import scala.math.BigDecimal.RoundingMode
object AkkaStream {
  import java.util.concurrent.atomic.AtomicReference
  import scala.concurrent.{Future, ExecutionContext}
  import scala.util.Random
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._

  var investments = new AtomicReference(Map(
    "TechCorp" -> 25,
    "Google" -> 40,
    "Nasdaq" -> 60
  ))

  def updateInvestmentByUsers(userId: Int, oldInvestments: Map[String, Int], updatedInvestments: Map[String, Int]): Future[Unit] = {
    println(s"🔄 Mise à jour des investissements pour l'utilisateur $userId...")
    (utilisateurActor2 ? InvestmentActor.GetInvestments(userId)).map {
      case investments: Seq[Investment] =>
        println(s"📊 ${investments.size} investissements trouvés pour l'utilisateur $userId.")
        val updatedInvestmentFutures = investments.map { investment =>
          val company = investment.companyName
          val oldPrice = BigDecimal(oldInvestments.getOrElse(company, 1).toDouble)
          val newPrice = BigDecimal(updatedInvestments.getOrElse(company, 1).toDouble)
          val amountInvested = BigDecimal(investment.amountInvested.toDouble)
          val newAmount = (amountInvested / oldPrice) * newPrice.setScale(2, RoundingMode.HALF_UP)

          println(s"📢 Mise à jour de $company pour l'utilisateur $userId : $amountInvested → $newAmount")
          utilisateurActor2 ? InvestmentActor.UpdateInvestment(userId, company, newAmount)
        }
        Future.sequence(updatedInvestmentFutures).map(_ => ())
    }
  }

  def updateInvestment(): Future[String] = {
    println("📩 Démarrage de la mise à jour globale des investissements...")

    val oldInvestments = investments.get()

    // Mettre à jour les investissements avec de nouveaux prix
    investments.updateAndGet { currentInvestments =>
      currentInvestments.map { case (company, price) =>
        val newPrice = Random.between(-30, 60) + price
        company -> newPrice
      }
    }

    // Récupérer tous les utilisateurs
    (utilisateurActor ? UtilisateurActor.GetUsers).mapTo[Seq[User]].flatMap { users =>
      println(s"✅ ${users.size} utilisateurs trouvés. Début de la mise à jour utilisateur...")

      val userUpdateFutures = users.flatMap { user =>
        user.id.map { userId =>  // 🔥 Extraction de l'ID seulement s'il est défini
          updateInvestmentByUsers(userId, oldInvestments, investments.get())
        }
      }


      // Attendre que toutes les mises à jour des utilisateurs soient terminées
      Future.sequence(userUpdateFutures).flatMap { _ =>
        println("✅ Mise à jour complète des investissements terminée.")

        // 🔥 Récupérer la liste des utilisateurs après la mise à jour
        (utilisateurActor ? UtilisateurActor.GetStringUsers).mapTo[String]
      }
    }
  }


  def generateUpdateSource()(implicit system: ActorSystem, ec: ExecutionContext): Source[Message, Any] = {
    Source.tick(1.second, 5.seconds, "").flatMapConcat { _ =>
      Source.future(updateInvestment().map(TextMessage(_)))
    }
  }
  def generateRandomNumberSource()(implicit system: ActorSystem, ec: ExecutionContext): Source[Message, Any] = {
    Source.tick(1.second, 5.seconds, "").map { _ =>
      val randomNumber = Random.nextInt(100) // Nombre aléatoire entre 0 et 99
      TextMessage(s"$randomNumber") // Envoie le nombre en WebSocket
    }
  }

  def websocketFlow()(implicit system: ActorSystem, ec: ExecutionContext): Flow[Message, Message, Any] = {
    val combinedSource = Source.combine(
      generateRandomNumberSource(),
      generateUpdateSource()
    )(Merge(_)) // Utilisation ok si l'ordre n'est pas un problème
    // Fusionne les deux flux en un seul

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
