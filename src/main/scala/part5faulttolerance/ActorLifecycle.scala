package part5faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifecycle extends App {

  val system = ActorSystem("LifecycleDemo")

  /**
   * Example 1
   */
  case object StartChild

  class LifecycleActor extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("I am started")

    override def postStop(): Unit = log.info("I have stopped")

    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifecycleActor], "child")
    }
  }

  //  val parent = system.actorOf(Props[LifecycleActor], "parent")
  //  parent ! StartChild
  //  parent ! PoisonPill

  /**
   * Example 2 - reestart
   */
  case object Fail

  case object FailChild

  case object CheckChild

  case object Check

  class Parent extends Actor {
    val child = context.actorOf(Props[Child], "supervisedChild")

    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check
    }
  }

  class Child extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("supervised child started")

    override def postStop(): Unit = log.info("supervised child stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info(s"supervised actor restarting because of ${reason.getMessage}")

    override def postRestart(reason: Throwable): Unit =
      log.info("supervised actor restarted")

    override def receive: Receive = {
      case Fail =>
        log.warning("child will fail now")
        throw new RuntimeException("I failed")
      case Check => log.info("alive and kicking")
    }
  }

  val supervisor = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild    // as consequence of that... child fails throwing an exception then:
                            // child prerestarted --> child instance removed
                            // ---> new child instance --> new child postrestarted

  supervisor ! CheckChild   // we can see that child still receives message, it is beacuse of default supervision strategy

  /**
   * default supervision strategy: if an actor fails given a message (in these case: Fail), then these kind of messages
   * are removed for the mailbox and not putting into again, and the actor continues receiving messages.
   */
}