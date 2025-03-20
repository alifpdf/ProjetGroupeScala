import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import slick.jdbc.PostgresProfile.api._


import scala.concurrent.{ExecutionContext, Future}
import java.sql.DriverManager
import scala.io.Source
import scala.sys.process._

import scala.util.{Failure, Success}
import scala.concurrent.duration.DurationInt






object Main extends {


  val url = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=1234"

  // Exécuter le script SQL
  val script = Source.fromFile("./src/main/script.sql").mkString
  val conn = DriverManager.getConnection(url)
  val statement = conn.createStatement()

  script.split(";").foreach { query =>
    if (query.trim.nonEmpty) {
      statement.execute(query.trim)
      println(s"Exécuté : $query")
    }
  }

  // Démarrer Akka System
  implicit val system: ActorSystem = ActorSystem("MainSystem")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit var timeout: Timeout = Timeout(5.seconds)
  //  Connexion à la base de données via Slick
  //  Création de la base de données Slick


  val db = Database.forConfig("slick.dbs.default.db")

  //  Création du service de base de données
  val dbService = new DBUtilisateur(db)
  val dbService1=new DBInvestment(db)
  val dbService2=new DBNotification(db)
  val dbService3=new DBProducts(db)


  //
  val utilisateurActor = system.actorOf(UtilisateurActor.props(dbService), "UtilisateurActor")
  val utilisateurActor2=system.actorOf(InvestmentActor.props(dbService1,utilisateurActor), "InvestementActor")
  val notificationActor = system.actorOf(SocketActor.props(dbService2), "SocketActor")
  val productsActor = system.actorOf(ProductsActor.props(dbService3), "ProductsActor")


  // Fonction pour démarrer le frontend
  def startFrontend(): Unit = {
    val frontendPath = "./frontend"
    val npmCommand = "cmd /c npm start" // Utilisation de cmd /c pour exécuter npm correctement

    println("Démarrage du frontend React...")

    val process = Process(npmCommand, new java.io.File(frontendPath)).run()

    process.exitValue() // Attendre que le processus se termine (optionnel)

  }

  def main(args: Array[String]): Unit = {

    val server = new WebSocketServer()(system, ec)
    server.start()


    println("Test : Envoi de 'Bonsoir à tous !' à tous les utilisateurs...")
    notificationActor ? SocketActor.BroadcastMessage("Bonsoir à tous !")



    startFrontend()
  }

}