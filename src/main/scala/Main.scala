import akka.actor.ActorSystem
import java.sql.DriverManager
import scala.io.Source

object Main {

  def main(args: Array[String]): Unit = {
    val url = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=1234"

    val script = Source.fromFile("H:/ING2/scalaProjet/projetScalaGR/src/main/script.sql").mkString
    val conn = DriverManager.getConnection(url)
    val statement = conn.createStatement()

    script.split(";").foreach { query =>
      if (query.trim.nonEmpty) {
        statement.execute(query.trim)
        println(s"✅ Exécuté : $query")
      }

    }
    implicit val system: ActorSystem = ActorSystem("MainSystem")
    // Lancer le serveur WebSocket
    val server = new WebSocketServer()(system)
    server.start()


  }
}