package part7patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

// step 1 - import the ask pattern
import akka.pattern.ask

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._
}

object AskSpec {

  // this code is somewhere else in your app
  case class Read(key: String)
  case class Write(key: String, value: String)
  class KVActor extends Actor with ActorLogging {

    override def receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key $key")
        sender() ! kv.get(key)
      case Write(key, value) =>
        log.info(s"Writing the vale $value for the key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess
  class AuthManager extends Actor with ActorLogging {

    // step 2 - infrastructure needed for ask pattern
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext = context.dispatcher

    private val authDB = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) => authDB ! Write(username, password)
      case Authenticate(username, password) =>

        // step 3 - ask the actor
        val future = authDB ? Read(username)

        // step 4 - handle the future e.g. with onComplete
        future.onComplete {
          case Success(None) => sender() ! AuthFailure("username not found")
          case Success(Some(dbPassword)) =>
            if (dbPassword == password) sender() ! AuthSuccess
            else sender() ! AuthFailure("password incorrect")
        }
    }
  }
}
