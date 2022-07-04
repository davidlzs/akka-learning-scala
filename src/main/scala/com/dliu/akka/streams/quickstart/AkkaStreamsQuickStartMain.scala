package com.dliu.akka.streams.quickstart

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString

import java.nio.file.{Paths, StandardOpenOption}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

object AkkaStreamsQuickStartMain extends App {
  implicit val system: ActorSystem = ActorSystem("quick-start")
  implicit val dispatcher: ExecutionContextExecutor = system.dispatcher

  val source: Source[Int, NotUsed] = Source(1 to 10)
  val done: Future[Done] = source.runForeach(println(_))
  //  done.onComplete(_ => system.terminate())

  private val sumSource: Source[Int, NotUsed] = source.fold(0)((acc, x) => acc + x)
  private val intDone: Future[Int] = sumSource.runWith(Sink.head)

  intDone.onComplete(t => println(t.getOrElse(null)))

  private val eventualInt: Future[Int] = source.scan(0)((acc, x) => acc + x).runWith(Sink.last)

  eventualInt.onComplete {
    case Success(i) => println(s"total is $i")
    case Failure(e) => throw e
  }

  //  eventualInt.onComplete(t => println("scan " + t.getOrElse(0)))

  private val fileDone: Future[IOResult] = source.scan(0)((acc, x) => acc + x).map(x => ByteString(s"$x\n")).runWith(FileIO.toPath(Paths.get("sums.txt"), Set(StandardOpenOption.APPEND, StandardOpenOption.CREATE)))
  fileDone.onComplete(r => {
    println(r.getOrElse(IOResult(-1)).count)
    system.terminate()
  })
}
