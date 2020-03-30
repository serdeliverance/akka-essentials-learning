package exercises.section3akkaactors

import akka.actor.{Actor, ActorRef, Props}

object ChildActorExercise extends App {

  object WordCounterMaster {
    case class Initialize(nChild: Int)
    case class WordCountTask(taskId: Int, text: String)
    case class WordCountReply(taskId: Int, count: Int)
  }

  class WordCounterMaster extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChild) =>
        val childrenRefs = for (i <- 1 to nChild) yield context.actorOf(Props[WordCounterWorker], s"wcw_$i")
        context.become(withChildren(childrenRefs, 0, 0, Map()))
    }

    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] I have receive: $text - I will send it to child: $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskId, text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
        val nextTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childrenRefs, nextChildIndex, nextTaskId, newRequestMap))
      case WordCountReply(taskId, count) =>
        println(s"[master] I have received a reply for task id $taskId with count $count")
        val originalSender = requestMap(taskId)
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - taskId))
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(taskId, text) =>
        println(s"[${self.path}] I have receive task:$taskId with text: $text")
        sender() ! WordCountReply(taskId, text.split(" ").length)
    }
  }

  /**
   * create workcounterMaster (wcm)
   * send initialize(10) to wcm
   * send "Akka is awesome" to wcm
   *    wcm will send a WordCountTask(...) to one of its children (using a Round Robin
   *    strategy)
   *      child replies with a WordCountReply(3) to wcm
   *     master replies with 3 to the sender
   *
   * req -> wcm -> wcw
   * req <- wcm <- wcw
   *
   */
}
