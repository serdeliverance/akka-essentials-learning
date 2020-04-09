package part6infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing._
import com.typesafe.config.ConfigFactory

/**
 * Routers are a way than an actor can deliver messages to its children, which are
 * registered as routees.
 *
 * Routing its a technique to delegate messages in between n identical actors
 *
 * There are different options for routing logic:
 * - Round Robin
 * - Random
 * - Smallest mailbox
 * - Broadcast
 * - Scatter-gather-first     => send message to all, take the first reply and discard the rest
 * - tail-chopping            => forward the message to each routee until the first child replies and discards the rest of the replies
 * - consistent hashing       => all the message with the same hash will be sent to the same actor
 *
 */
object Routers extends App {

  /**
   *  #1 - manual router
   */
  class Master extends Actor {
    // step 1 - create routees
    // 5 actor routees based off Slave actors
    private val slaves = for(i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }

    // step 2 - define router
    private var router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {

      // step 4 - handle the termination/lifecycle of the routees
      case Terminated(ref) =>
        router = router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router = router.addRoutee(newSlave)
      // step 3 - route the messages
      case message => router.route(message, sender())
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  // ConfigFactory... is needed for 2.2 and 3.2 which are routers created from configuration
  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))
//  val master = system.actorOf(Props[Master])
//
//  for (i <- 1 to 10) {
//    master ! s"[$i] Hello from the world!"
//  }

  /**
   * Method #2 - a router actor with its own children
   * POOL router
   *
   * 2.1 - this is programmatically
   */
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
//  for (i <- 1 to 10) {
//    poolMaster ! s"[$i] Hello from the world"
//  }

  // 2.2 from configuration   (application.conf)
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  for (i <- 1 to 10) {
    poolMaster ! s"[$i] Hello from the world"
  }

  /**
   * Method #3 - router with actors created elsewhere
   * GROUP router
   *
   * This method is util when the routees are created elsewhere in the application
   */
  //... imagine that in another part of my application there are...
  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_$i")).toList

  // for using them I need their paths
  val slavePaths = slaveList.map(slaveRef => slaveRef.path.toString)

  // 3.1 in the code
  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
//  for (i <- 1 to 10) {
//    groupMaster ! s"[$i] Hello from the world"
//  }

  // 3.2 from configuration     (application.conf)
  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2")
  for (i <- 1 to 10) {
    groupMaster2 ! s"[$i] Hello from the world"
  }

  /**
   * Special messages
   */
  groupMaster2 ! Broadcast("hello, everyone")

  // PoisonPill and Kill are not routed, there are handled by the routing actor
  // AddRoutee, Remove, GetRoutee are handled only by the routing actor
}
