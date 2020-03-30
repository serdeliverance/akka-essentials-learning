package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChildActors.Parent.{CreateChild, TellChild}

object ChildActors extends App {

  // actor can create other actors

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }

  class Parent extends Actor {
    import Parent._


    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"[${self.path}] creating child")
        // creating child
        val childRef = context.actorOf(Props[Child], name)
        // cambiamos el message handler, para que, a partir de ahora sea este... el cual, hace un forward al child
        context.become(withChild(childRef))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) => childRef forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }

  val system = ActorSystem("childActorDemo")
  val parent = system.actorOf(Props[Parent], "parent")

  parent ! CreateChild("joe")
  parent ! TellChild("do your homework")

  // que un actor pueda crear otros nos permite tener:
  //    actor hierarchies
  //    parent -> child -> grandChild

  // la jerarquia puede ser tan grande como quisieramos, pero.. quien es padre de parent?
  // para eso existen los Guardian Actors
  // Guardian Actors (top-level)

  // cada actor system tiene 3 guardian actors, los cuales son:
  // 1- /system = system guardian
  //        cada actor system tiene actors para manejar cuestiones cross, ej: Loggin.
  //  /system gestiona dichos actors
  // 2- /user = user level actor
  // cada actor que creamos mediante actorOf es gestionado por este guardian actor (podemos ver esto cuando
  // hacemos self.path de uno de dichos actores.
  // 3- / = root guardia
  // este actor maneja a system y a user guardian.

  /**
   * Actor selection
   */
  val childSelection = system.actorSelection("/user/parent/child2")
  childSelection ! "I found you"  // actorSelection returns an actorSelection which is a wrapper around a possible
  // actor ref. If ref is empty, message will be sent to dead letters actor.

  /**
   * Danger!
   *
   * NEVER PASS ACTOR MUTABLE STATE, OR THE `THIS` REFERENCE, TO CHILD ACTORS.
   *
   * Dado que esto rompe el principio de encapsulamiento que nos dan los actores (porque, en dicho caso,
   * el child tiene acceso al estado del padre)
   *
   * Usar la referencia `this` expone a nuestro codigo a concurrency issues. Por ejemplo... si un child se crea
   * haciendo una referencia `this` al padre, entonces el child puede llamar directamente al metodo del padre,
   * esto viola el principio de encapsulamiento de actores (que dice que tenemos que comunicarnos via mensajes,
   * no via invocacion directa de metodos). Ademas de esto, los actores son estructuras de datos que se ejecutan
   * en diferentes threads (dado que encapsulan su estado). Si yo, desde el child, llamo directamente al parent
   * invocacion de un metodo, podria llamar al parent y modificar su estado desde otro thread, violando el encapsulamiento
   * ... es decir.. tendria RACE CONDITIONS.. por lo cual, esto es una mala práctica.
   *
   * Esto lleva a otra conclusion:
   *
   * NUNCA INVOCAR METODOS DE ACTORES DIRECTAMENTE, SIEMPRE COMUNICARSE CON ACTORES VIA MESSAGES.
   *
   * Esta mala practica de pasar `this` a los childs, se conoce como CLOSING OVER, y es algo que el compilador
   * no puede evitar, sino que corre por nuestra parte no llevarla a cabo.
   */
}
