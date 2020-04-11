package part7patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

/**
 * Stash => putting messages aside for later
 */
object StashDemo extends App {

  /**
   * ResourceActor
   * - open => it can receive read/write request to the resource
   * - otherwise it will postpone all read/write request until the state is open
   *
   * ResourceActor is closed
   * - Open => switch to the open state
   * - Read/Write messages are postponed
   *
   * ResourceActor is open
   * - Read/Write are handled
   * - Close => switch to the close state
   *
   * Ex:
   *    [Read, Open, Write]
   *
   *    - stash Read
   *      Stash: [Read]
   *    - open => switch to the open state
   *      Mailbox: [Read, Write]
   *    - read and write are handled
   */
  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  // step 1 - mix-in the Stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        // unstashAll when you switch the message handler
        unstashAll()    //
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the close state")
        // step 2 - stash away what you can't handle
        stash()
    }

    def open: Receive = {
      case Read =>
        // do some actual computation
        log.info(s"I have read $innerData")
      case Write(data) =>
        log.info(s"I'm writting $data")
        innerData = data
      case Close =>
        log.info("Closing resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the open state")
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Read // stashed
  resourceActor ! Open // switch to open: I have read
  resourceActor ! Open // stashed
  resourceActor ! Write("I love stash") // I'm writing I love stash
  resourceActor ! Close // switch to close; switch to open
  resourceActor ! Read  // I have read I love stash
}
