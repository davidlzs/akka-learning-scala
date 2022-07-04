package com.dliu.akka.streams.quickstart

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.{ExecutionContextExecutor, Future}

case class Author(name: String)
case class Hashtag(name: String)
case class Tweet(author: Author, timestamp: Long, body: String) {
  def hashtags(): Set[Hashtag] = {
    body.split(" ")
      .collect {
        case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
      }.toSet
  }
}

object TwitterSource extends App {
  implicit val system : ActorSystem = ActorSystem("tweet-system")
  implicit val dispatcher: ExecutionContextExecutor = system.dispatcher
  val done: Future[Done] = Source(
    Tweet(Author("John"), System.currentTimeMillis(), "#akka") ::
      Tweet(Author("Jenny"), System.currentTimeMillis(), "#akka rocks") ::
      Tweet(Author("Josh"), System.currentTimeMillis(), "#apple rocks") ::
      Nil
  ).map(_.hashtags)
    .filterNot(_.contains(Hashtag("#akka")))
    .reduce(_ ++ _)
    .mapConcat(identity)
    .map(_.name.toUpperCase)
    .runWith(Sink.foreach(println))
  done.onComplete(_ => system.terminate())
}
