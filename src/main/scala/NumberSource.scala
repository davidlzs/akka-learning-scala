import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{Attributes, Graph, Materializer, Outlet, SourceShape}
import akka.stream.stage.GraphStage
import akka.stream.stage.GraphStageLogic
import akka.stream.stage.OutHandler

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class NumbersSource extends GraphStage[SourceShape[Int]] {
  val out: Outlet[Int] = Outlet("NumbersSource")
  override val shape: SourceShape[Int] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      // All state MUST be inside the GraphStageLogic,
      // never inside the enclosing GraphStage.
      // This state is safe to access and modify from all the
      // callbacks that are provided by GraphStageLogic and the
      // registered handlers.
      private var counter = 1

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          push(out, counter)
          counter += 1
        }
      })
    }
}

object NumbersSourceMain extends App {
  val system: ActorSystem = ActorSystem("streams-test")
  implicit val materializer: Materializer = Materializer(system)
  // A GraphStage is a proper Graph, just like what GraphDSL.create would return
  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new NumbersSource

  // Create a Source from the Graph to access the DSL
  val mySource: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)

  // Returns 55
  val result1: Future[Int] = mySource
    .log(name = "myStream")
    .addAttributes(
      Attributes.logLevels(
        onElement = Attributes.LogLevels.Off,
        onFinish = Attributes.LogLevels.Info,
        onFailure = Attributes.LogLevels.Error))
    .take(10)
    .runFold(0)(_ + _)

  // The source is reusable. This returns 5050
  val result2: Future[Int] = mySource.take(100).runFold(0)(_ + _)

  val sum_1_to_10: Int = Await.result(result1, 3.seconds)
  val sum_1_to_100: Int = Await.result(result2, 3.seconds)

  println(sum_1_to_10)
  println(sum_1_to_100)

  system.terminate();
}