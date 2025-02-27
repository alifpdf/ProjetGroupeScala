import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

object AkkaStream {

  def generateRandomNumberSource()(implicit system: ActorSystem, ec: ExecutionContext): Source[Message, Any] = {
    Source.tick(1.second, 2.seconds, "").map { _ =>
      val randomNumber = Random.nextInt(100) // Nombre aléatoire entre 0 et 99
      TextMessage(s"$randomNumber") // Envoie le nombre en WebSocket
    }
  }

  def websocketFlow()(implicit system: ActorSystem, ec: ExecutionContext): Flow[Message, Message, Any] = {
    Flow.fromSinkAndSource(Sink.ignore, generateRandomNumberSource())
  }
}
