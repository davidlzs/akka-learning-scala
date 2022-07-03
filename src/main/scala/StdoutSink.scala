import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.stream.{Attributes, Inlet, Materializer, SinkShape}

import java.util.concurrent.CountDownLatch
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}

/**
 * extends GraphStageWithMaterializedValue[SinkShape[Int], Future[Done]]
 * will make the Sink materialize value as Future[Done], which will be a Promise[Done]
 * completed when preStart done.
 */
class StdoutSink extends GraphStageWithMaterializedValue[SinkShape[Int], Future[Done]] {
  val in: Inlet[Int] = Inlet("StdoutSink")
  override val shape: SinkShape[Int] = SinkShape(in)

  //    new GraphStageLogic(shape) {
  //
  //      // This requests one element at the Sink startup.
  //      override def preStart(): Unit = pull(in)
  //
  //      setHandler(in, new InHandler {
  //        override def onPush(): Unit = {
  //          println(grab(in))
  //          pull(in)
  //        }
  //      })
  //    }
  val preStartDone = Promise[Done]()

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    (new GraphStageLogic(shape) {

      // This requests one element at the Sink startup.
      override def preStart(): Unit = {
        pull(in)
        preStartDone.success(Done)
      }

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          println(grab(in))
          pull(in)
        }
      })
    }, preStartDone.future)
  }
}

object StdoutSinkMain extends App {
  val system: ActorSystem = ActorSystem("StdoutSinkSystem")
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = Materializer(system)

  val eventualDone: Future[Done] = Source(List(0, 1, 2))
    .toMat(Sink.fromGraph(new StdoutSink))(Keep.right)
    .run()

  val countDownLatch = new CountDownLatch(1)

  eventualDone.onComplete { _ =>
    println("pre is done")
    countDownLatch.countDown()
  }

  countDownLatch.await()
  materializer.shutdown()
  system.terminate()
}