package part6infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/**
 * Dispatchers controls how messages are send and handled.
 *
 * It is useful to define thread-pools, throughput of an actor, etc.
 *
 */
object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    var count = 0

    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] $message")
    }
  }

  val system = ActorSystem("DispatcherDemo") // ConfigFactory.load().getConfig("dispatcherDemo")

  // method #1 - programmatic in code
//  val actors = for (i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")
//  val r = new Random()
//  for (i <- 1 to 1000) {
//    actors(r.nextInt(10)) ! i
//  }

  // method #2 - from config
  val rtjvmActor = system.actorOf(Props[Counter], "rtjvm")

  /**
   * Dispatchers implements the ExecutionContext trait
   *
   * ExecutionContext => it can schedule actions and running it in threads
   *
   * In this case, Dispatchers can execute Futures.
   */

  class DBActor extends Actor with ActorLogging {

    implicit val executionContext: ExecutionContext = context.dispatcher

    // solution 1:
    // implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dedidacted-blocking-dispatcher")

    override def receive: Receive = {

      /**
       * Doing the following, we are running futures on top of actors which is discouraged.
       *
       * In that particular case, the Future is being block by a long IO, which starves the contextDispatcher
       * of running Threads which make it not able to handle messages.
       */
      case message => Future {
        // wait on a resource
        Thread.sleep(5000)
        log.info(s"Success: $message")
      }
    }
  }

  val dbActor = system.actorOf(Props[DBActor])
//  dbActor ! "the meaning of life is 42"

  val nonblockingActor = system.actorOf(Props[Counter])

  for (i <- 1 to 1000) {
    val message = s"important message $i"
    dbActor ! message
    nonblockingActor ! message    // we see that dbActor blocks the dispatcher
  }

  /**
   * TIP/Soluctions:
   *
   * Solution #1
   * Use a dedicated dispatcher for blocking running blocking code (ej: DB communication)
   *
   * Ej: context.system.dispatchers.lookup("my-dedidacted-blocking-dispatcher")
   *
   * Solution #2
   * Using a router
   */
}
