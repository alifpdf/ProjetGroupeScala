import Main.{dbService, ec, system, timeout, utilisateurActor, utilisateurActor2}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.model.ws.{Message, TextMessage}

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

    // 🔄 Étape 1 : Récupérer les investissements actuels de l'utilisateur
    (utilisateurActor2 ? InvestmentActor.GetInvestments(userId)).flatMap {
      case investments: Seq[Investment] =>
        println(s"📊 ${investments.size} investissements trouvés pour l'utilisateur $userId.")

        // 🔄 Étape 2 : Recalculer et mettre à jour chaque investissement
        val updatedInvestmentFutures = investments.map { investment =>
          val company = investment.companyName
          val oldPrice = BigDecimal(oldInvestments.getOrElse(company, 1).toDouble)
          val newPrice = BigDecimal(updatedInvestments.getOrElse(company, 1).toDouble)
          val amountInvested = BigDecimal(investment.amountInvested.toDouble)

          val newAmount = (amountInvested / oldPrice) * newPrice
            .setScale(2, RoundingMode.HALF_UP) // Arrondi à 2 décimales

          println(s"📢 Mise à jour de $company pour l'utilisateur $userId : $amountInvested → $newAmount")

          utilisateurActor2 ? InvestmentActor.UpdateInvestment(userId, company, newAmount)
        }

        Future.sequence(updatedInvestmentFutures).map(_ => ())

    }
  }

  def updateInvestment(): Future[Unit] = {
    println("📩 Démarrage de la mise à jour globale des investissements...")

    // 🔄 Étape 1 : Sauvegarde des anciens investissements avant mise à jour
    val oldInvestments = investments.get()

    // 🔄 Étape 2 : Mettre à jour les investissements en mémoire
    investments.updateAndGet { currentInvestments =>
      currentInvestments.map { case (company, price) =>
        val newPrice = Random.between(-30, 60) + price
        company -> newPrice
      }
    }

    // 🔄 Étape 3 : Récupérer tous les utilisateurs via `utilisateurActor`
    (utilisateurActor ? UtilisateurActor.GetUsers).mapTo[Seq[User]].flatMap {
      users=>
        println(s"✅ ${users.size} utilisateurs trouvés. Début de la mise à jour utilisateur...")

        // 🔄 Étape 4 : Mise à jour des investissements pour chaque utilisateur via un Stream
        val userUpdateSource = Source(users.flatMap(_.id))

        userUpdateSource.mapAsync(1) { userId =>
          updateInvestmentByUsers(userId, oldInvestments, investments.get())
        }.runWith(Sink.ignore).map { _ =>
          println("✅ Mise à jour complète des investissements terminée.")
        }
    }
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


  def generateRandomNumberSource()(implicit system: ActorSystem, ec: ExecutionContext): Source[Message, Any] = {
    Source.tick(1.second, 5.seconds, "").map { _ =>
      val randomNumber = Random.nextInt(100) // Nombre aléatoire entre 0 et 99
      TextMessage(s"$randomNumber") // Envoie le nombre en WebSocket
    }
  }

  def websocketFlow()(implicit system: ActorSystem, ec: ExecutionContext): Flow[Message, Message, Any] = {
    Flow.fromSinkAndSource(Sink.ignore, generateRandomNumberSource())
  }
}
