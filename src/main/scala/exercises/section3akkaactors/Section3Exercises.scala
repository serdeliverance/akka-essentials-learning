package exercises.section3akkaactors

import akka.actor.{Actor, ActorSystem, Props}
import exercises.section3akkaactors.Section3Exercises.Counter.{Decrement, Increment, Print}

object Section3Exercises extends App {

  /**
   * Re create Counter Actor with context.become and NO MUTABLE STATE
   */

  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {

    override def receive: Receive = countReceive(0)

    /**
     * Conclusion/TIP: Si se quiere reescribir un actor para que sea INMUTABLE
     *
     * Se reescriben los var (el estado mutable) y pasan a ser parameter del receive
     * handler
     *
     *
     */
    def countReceive(currentCount: Int): Receive = {
      case Increment => context.become(countReceive(currentCount + 1))
      case Decrement => context.become(countReceive(currentCount - 1))
      case Print => println(s"[counter] my current count is $currentCount")
    }
    //    def incrementReceive: Receive = {
//      case Increment =>
//      case Decrement => context.become(decrementReceive, false)
//      case Print =>
//    }
//
//    def decrementReceive: Receive = {
//      case Increment => context.become(incrementReceive, false)
//      case Decrement =>
//      case Print =>
//    }
  }

  val system = ActorSystem("actorSystem")
  val counter = system.actorOf(Props[Counter], "counter")

  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print
}