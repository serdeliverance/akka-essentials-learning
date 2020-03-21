package part1recap

object GeneralRecap extends App {

  var pairs = for {
    num <- List(1,2,3,4)
    char <- List('a','b','c','d')

  } yield num + "-" +char

  pairs.foreach(p => println(p))
}
