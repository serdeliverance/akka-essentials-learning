package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ActorCapabilities.Counter.{Decrement, Increment, Print}
import part2actors.ActorCapabilities.Person.LiveTheLife

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi" => context.sender() ! "Hi, there. How you doing?"     // reply to a message (responde con un mensaje, todo asincrono y non blocking)
      case message: String => println(s"[simple actor] I have receive $message")
      case number: Int => println(s"[simple actor] I have receive $number")
      case SpecialMessage(content) => println(s"[simple actor] I have receive $content")
      case SendMessageToYourself(content) =>
        println("a message sent to yourself")
        self ! SpecialMessage(content)
      case SayHiTo(ref) => ref ! "Hi"
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello, actor"

  // 1- messages can be of any type
  // a) messages must be INMUTABLE
  // b) messages must be SERIALIZABLE (para que la JVM pueda transformarlo en un bytestream y enviarlo a otra JVM, en caso de ser necesario)

  // en la practica: usar case classes and case objects
  simpleActor ! 42

  case class SpecialMessage(content: String)
  simpleActor ! SpecialMessage("some content")

  // 2- actor tienen informacion acerca de su context y de ellos mismos
  // context.self === `this` in OOP

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("a message sent by you")

  // 3- actor can reply to messages
  var alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  /**
   * Exercises
   *
   * 1. a Counter actor
   *  - Increment
   *  - Decrement
   *  - Print
   */

  class Counter extends Actor {
    import Counter._

    var count = 0

    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println(s"[counter actor] count=$count")
    }
  }

  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }


  val counterActor = system.actorOf(Props[Counter], "counterActor")

  counterActor ! Increment
  counterActor ! Increment
  counterActor ! Decrement
  counterActor ! Print

  /**
   * A Bank account that:
   *   receives
   *   - Deposit an amount
   *   - Withdraw an amount
   *   - Statement
   *   replies with
   *   - Success
   *   - Failure
   *
   *   Interact with some other kind of actor
   */

  // DOMAIN objects... it is a best practice
  object BankAccount {
    case class Deposit(amount: Double)
    case class Withdraw(amount: Double)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
  }

  class BankAccount extends Actor {

    import BankAccount._

    var funds = 0.0

    override def receive: Receive = {
      case Deposit(amount) =>
        if(amount < 0) sender() ! TransactionFailure("invalid deposit amount")
        else {
            funds += amount
            sender() ! TransactionSuccess(s"successfully deposited $amount")
          }
      case Withdraw(amount) =>
        if (amount < 0) sender() ! TransactionFailure(s"invalid withdraw $amount")
        else if (amount > funds) sender() ! TransactionFailure("insufficient funds")
        else {
          funds -= amount
          sender() ! TransactionSuccess(s"successfully withdraw $amount")
        }
      case Statement => sender() ! s"Your balance is $funds"
    }
  }

  object Person {
    case class LiveTheLife(account: ActorRef)
  }

  class Person extends Actor {
    import Person._
    import BankAccount._

    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(90000)
        account ! Withdraw(500)
        account ! Statement
      case message => println(message.toString)
    }
  }

  val account = system.actorOf(Props[BankAccount], "bankAccount")
  val person = system.actorOf(Props[Person], "billionaire")

  person ! LiveTheLife(account  )


}
