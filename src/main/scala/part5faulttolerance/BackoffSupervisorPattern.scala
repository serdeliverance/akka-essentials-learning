package part5faulttolerance

import java.io.File

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffSupervisor}

import scala.io.Source
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object BackoffSupervisorPattern extends App{

  case object ReadFile
  class FileBasePersistentActor extends Actor with ActorLogging {

    var dataSource: Source = null

    override def preStart(): Unit =
      log.info("Persistent actor starting")

    override def postStop(): Unit =
      log.warning("Persitent actor has stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.warning("Persistent actor restarted")

    override def receive: Receive = {
      case ReadFile =>
        if (dataSource == null)
          dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_xx.txt"))
        log.info("I've just read some important data: " + dataSource.getLines().toList)
    }
  }

  val system = ActorSystem("BackoffSupervisionDemo")
//  val simpleActor = system.actorOf(Props[FileBasePersistentActor], "simpleActor")
//  simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FileBasePersistentActor],
      "simpleBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    )
  )

  /**
   * simpleSupervisor:
   * - child called simpleBackoffActor with the props of FileBasePersistentActor
   * - supervision strategy is the default one (restarts everything)
   *    - first attempt after 3 seconds
   *    - second attempt after 2x the previous attempt
   */
  val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
  simpleBackoffSupervisor ! ReadFile


  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[FileBasePersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ). withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

//  val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
//  stopSupervisor !  ReadFile

  class EagerFBPActor extends FileBasePersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor starting")
      dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
    }
  }

  val repeatedSupervisionProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[EagerFBPActor],
      "eagerActor",
      1 second,
      30 seconds,
      0.1
    )
  )

  val repeatedSupervisor = system.actorOf(repeatedSupervisionProps, "eagerSupervisor")

  /**
   * eager supervisor
   *  - child eager actor
   *    - will die on start with ActorInitializationException
   *    - trigger the supervision strategy in eagerSupervision => STOP eagerActor
   *  - backoff will kick in after 1 second, 2s, 4s, 8s...
   */
}
