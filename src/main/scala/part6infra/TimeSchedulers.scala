package part6infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object TimeSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

//  system.log.info("Scheduling reminder for simple actor")

  implicit val executionContext = system.dispatcher

  /**
   * schedulers need an executionContext
   */
//  system.scheduler.scheduleOnce(1 second) {
//    simpleActor ! "reminder"
//  }
//
//
//  val routine: Cancellable = system.scheduler.schedule(1 second, 2 seconds) {
//    simpleActor ! "hearbeat"
//  }

  // cancelates routine task
//  system.scheduler.scheduleOnce(5 seconds) {
//    routine.cancel()
//  }

  /**
   * Implement a self-closing actor
   *
   * - if the actor receives a message (anything), you have 1 second to send it another message
   * - if the time window expires, the will stop itself
   * - if you send another message, the time window is reset
   */

//  class SelfClosingActor extends Actor with ActorLogging {
//
//    var schedule = createTimeoutWindow()
//
//    def createTimeoutWindow(): Cancellable = {
//      context.system.scheduler.scheduleOnce(1 second) {
//        self ! "timeout"
//      }
//    }
//
//    override def receive: Receive = {
//      case "timeout" =>
//        log.info("Stopping myself")
//        context.stop(self)
//      case message =>
//        log.info(s"Received $message, staying alive")
//        schedule.cancel()
//        schedule = createTimeoutWindow()
//    }
//  }
//
//  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
//  system.scheduler.scheduleOnce(250 millis) {
//    selfClosingActor ! "ping"
//  }
//
//  system.scheduler.scheduleOnce(2 seconds) {
//    system.log.info("sending pong to the self-closing actor")
//    selfClosingActor ! "pong"
//  }

  /**
   * Timer
   */
  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBasedHearbeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrap")
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I am alived")
      case Stop =>
        log.warning("Stopping!")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerBasedHearbeatActor = system.actorOf(Props[TimerBasedHearbeatActor], "timerBasedHeartbeatActor")
  system.scheduler.scheduleOnce(5 seconds) {
    timerBasedHearbeatActor ! Stop
  }
}
