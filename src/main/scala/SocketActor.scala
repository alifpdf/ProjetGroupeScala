import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.ws.TextMessage
  // Assure-toi que User est bien importé depuis DatabaseService

object SocketActor {
  def props(): Props = Props[SocketActor]

  // Messages
  case class Connect(user: User, client: ActorRef)
  case class Disconnect(user: User)
  case class BroadcastMessage(msg: String)
}

class SocketActor extends Actor {
  import SocketActor._


  var clients: Map[User, ActorRef] = Map()

  def receive: Receive = {
    case Connect(user, client) =>
      clients += (user -> client)
      println(s"${user.name} s'est connecté.")

    case Disconnect(user) =>
      clients -= user
      println(s"${user.name} s'est déconnecté.")



    case BroadcastMessage(msg) =>
      clients.values.foreach(_ ! TextMessage(s"Annonce: $msg"))
  }
}
