package exercises.section3akkaactors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Section3ExercisesResolvedDaniel extends App {

  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])

  class Citizen extends Actor {
    override def receive: Receive = {
      case Vote(c) => context.become(voted(c))
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }

    def voted(candidate: String): Receive = {
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor {
    var stillWaiting : Set[ActorRef] = Set()
    var currentStats: Map[String, Int] = Map()

    override def receive: Receive = {
      case AggregateVotes(citizens) =>
        stillWaiting = citizens
        stillWaiting.foreach(citizenRef => citizenRef ! VoteStatusRequest)
      case VoteStatusReply(None) =>
        // a citizen hasn't voted yet
        sender() ! VoteStatusRequest // it's a retry and could cause an infinite loop (but thats not the case)
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStats.getOrElse(candidate, 0)
        currentStats = currentStats + (candidate -> (currentVotesOfCandidate + 1))
        if (newStillWaiting.isEmpty) {
          println(s"[aggregator] poll stats: $currentStats")
        } else {
          stillWaiting = newStillWaiting
        }
    }
  }

  val system = ActorSystem("votingSystem")

  val alice = system.actorOf(Props[Citizen], "alison")
  val bob = system.actorOf(Props[Citizen], "bob")
  val charlie = system.actorOf(Props[Citizen], "charlie")
  val daniel = system.actorOf(Props[Citizen], "daniel")

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator], "voteAggregator")
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))
}
