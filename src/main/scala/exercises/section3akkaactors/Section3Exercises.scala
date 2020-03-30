package exercises.section3akkaactors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import exercises.section3akkaactors.Section3Exercises.Citizen.Vote
import exercises.section3akkaactors.Section3Exercises.Counter.{Decrement, Increment, Print}
import exercises.section3akkaactors.Section3Exercises.VoteAggregator.AggregateVotes

object Section3Exercises extends App {

  /**
   * 1. Re create Counter Actor with context.become and NO MUTABLE STATE
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
  }

  val system = ActorSystem("actorSystem")
  val counter = system.actorOf(Props[Counter], "counter")

  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  /**
   * 2 - Voting system
   */


  object Citizen {
    case class Vote(candidate: String)
    case object VoteStatusRequest
    case class VoteStatusReply(citizen: ActorRef, candidate: Option[String])
  }

  class Citizen extends Actor {
    import Citizen._

    override def receive: Receive = voteReceive(None)

    def voteReceive(candidate: Option[String]): Receive = {
      case Vote(voteCandidate) => context.become(voteReceive(Some(voteCandidate)))
      case VoteStatusRequest => sender() ! VoteStatusReply(self, candidate)
    }
  }


  object VoteAggregator {
    case class AggregateVotes(citizens: Set[ActorRef])
  }

  class VoteAggregator extends Actor {
    import Citizen._
    import VoteAggregator._

    override def receive: Receive = {
      case AggregateVotes(citizens) =>
        for (c <- citizens) yield c ! VoteStatusRequest
      case VoteStatusReply(citizen, voteReply) =>
        val voteResult = voteReply.getOrElse("NOBODY")
        println(s"[$self] vote for: $voteResult")
    }
  }

  val bob = system.actorOf(Props[Citizen], "bob")
  val alison = system.actorOf(Props[Citizen], "alison")
  val robert = system.actorOf(Props[Citizen], "robert")

  bob ! Vote("macri")
  alison ! Vote("albert")
  alison ! Vote("ricky fort")


  val voteAggregator = system.actorOf(Props[VoteAggregator], "voteAggregator")

  voteAggregator ! AggregateVotes(Set(bob, alison, robert))
}