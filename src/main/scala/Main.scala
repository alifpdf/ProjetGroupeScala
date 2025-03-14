import AkkaStream.{investments, updateInvestment, updateInvestmentByUsers}
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import slick.jdbc.PostgresProfile.api._
import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import java.sql.DriverManager
import scala.io.Source
import scala.sys.process._
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.duration.DurationInt





object Main extends App {


    val url = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=1234"

    // 📌 Exécuter le script SQL
    val script = Source.fromFile("C:/Users/CYTech Student/IdeaProjects/ProjetGroupeScala/src/main/script.sql").mkString
    val conn = DriverManager.getConnection(url)
    val statement = conn.createStatement()

    script.split(";").foreach { query =>
      if (query.trim.nonEmpty) {
        statement.execute(query.trim)
        println(s"✅ Exécuté : $query")
      }
    }

    // 📌 Démarrer Akka System
    implicit val system: ActorSystem = ActorSystem("MainSystem")
    implicit val ec: ExecutionContext = system.dispatcher
  implicit var timeout: Timeout = Timeout(5.seconds)
  // 📌 Connexion à la base de données via Slick
  // 📌 Création de la base de données Slick


  val db = Database.forConfig("slick.dbs.default.db")

  // 📌 Création du service de base de données
  val dbService = new DBUtilisateur(db) // ⚠️ Ajoute cette ligne
  val dbService1=new DBInvestment(db)
  val dbService2=new DBNotification(db)


  //A tester
  val utilisateurActor = system.actorOf(UtilisateurActor.props(dbService), "UtilisateurActor")
  val utilisateurActor2=system.actorOf(InvestmentActor.props(dbService1,utilisateurActor), "InvestementActor")
  val notificationActor = system.actorOf(SocketActor.props(dbService2), "SocketActor")


  println("resultat string")
  (utilisateurActor ? UtilisateurActor.GetStringUsers).onComplete {
    case scala.util.Success(result) => println(s"📌 Réponse reçue : $result")
    case scala.util.Failure(exception) => println(s"❌ Erreur : ${exception.getMessage}")
  }


  import scala.util.{Success, Failure}







  /*
  updateInvestment().flatMap { _ =>
    println("✅ Test réussi : Première mise à jour des investissements terminée.")
    println(s"📊 Nouveaux investissements après la première mise à jour: ${investments.get()}")

    // ✅ Lancer une deuxième mise à jour après la première et attendre son exécution
    updateInvestment()
  }.onComplete {
    case Success(_) =>
      println("✅ Test réussi : Deuxième mise à jour des investissements terminée.")
      println(s"📊 Nouveaux investissements après la deuxième mise à jour: ${investments.get()}")

    case Failure(ex) =>
      println(s"❌ Erreur lors de la mise à jour des investissements : ${ex.getMessage}")
  }


*/

  // Déclaration du timeout ici

  /*

  (utilisateurActor ? UtilisateurActor.GetUsers).mapTo[Seq[User]].onComplete {
    case Success(users) =>
      println(s"✅ Réponse reçue : ${users.size} utilisateurs")
    case Failure(ex) =>
      println(s"❌ Erreur lors de la récupération des utilisateurs : ${ex.getMessage}")
  }



    val response = (utilisateurActor ? UtilisateurActor.AddUtilisateur("Maco", "test@example.com", "password123", BigDecimal(0)))


    response.onComplete {
      case Success(_) => println("✅ Utilisateur ajouté avec succès !")
    }
    var response1 = (utilisateurActor ?UtilisateurActor.updateBalance("test@example.com",100))
    response1.onComplete {
      case Success(_) => println("✅ Utilisateur ajouté avec succès !")
    }
    var response2=(utilisateurActor ? UtilisateurActor.GetId("test@example.com")).mapTo[Int]



    response2.onComplete {
      case Success(id)=>{
        var response3=(utilisateurActor2? InvestmentActor.UpdateInvestment(id,"TechCorp",40))
        response3.onComplete {
          case Success(e)=>print(e)
        }
        response3=(utilisateurActor2?InvestmentActor.AddInvestment(id,"Bismillah kebab",40))
        response3.onComplete {
          case Success(e)=>print(e)
        }

      }
    }

  update1()*/


  /*
    val response = (utilisateurActor ? UtilisateurActor.AddUtilisateur("Maco", "test@example.com", "password123", BigDecimal(0)))

    response.onComplete {
      case Success(_) => println("✅ Utilisateur ajouté avec succès !")
    }
    var response1 = (utilisateurActor ?UtilisateurActor.updateBalance("test@example.com",100))
    response1.onComplete {
      case Success(_) => println("✅ Utilisateur ajouté avec succès !")
    }



    var response2 = (utilisateurActor ?UtilisateurActor.GetBalance("test@example.com"))
    response2.onComplete {
      case Success(balance) => println(s"📢 Balance récupérée : $balance")
    }

    var response3=(utilisateurActor ?UtilisateurActor.VerifierPassword("test@example.com", "password123"))
    response3.onComplete {
      case Success(true)=>println("code correct")
    }*/

  val server = new WebSocketServer()(system, ec)
    server.start()

  // ✅ Planification de l'envoi automatique après 15s

      println("📢 Test : Envoi de 'Bonsoir à tous !' à tous les utilisateurs...")
      notificationActor ? SocketActor.BroadcastMessage("🌙 Bonsoir à tous !")
    // ✅ Ajoute `ec` pour éviter les erreurs Akka

  // ✅ Planification du lancement du frontend après 20s

      startFrontend()



  // 📌 Fonction pour démarrer le frontend
  def startFrontend(): Unit = {
    val frontendPath = "C:/Users/CYTech Student/IdeaProjects/ProjetGroupeScala/frontend"
    val npmCommand = "cmd /c npm start" // Utilisation de cmd /c pour exécuter npm correctement

    println("🚀 Démarrage du frontend React...")


    val process = Process(npmCommand, new java.io.File(frontendPath)).run()



    process.exitValue() // Attendre que le processus se termine (optionnel)

  }
}
