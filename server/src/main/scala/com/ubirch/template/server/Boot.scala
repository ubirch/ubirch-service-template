package com.ubirch.template.server

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.template.config.TemplateServiceConfig
import com.ubirch.template.server.route.MainRoute

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-03-22
  */
object Boot extends App with StrictLogging {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val timeout = Timeout(TemplateServiceConfig.actorTimeout seconds)

  val bindingFuture = start()
  registerShutdownHooks()

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    }
  })

  def start(): Future[ServerBinding] = {

    val interface = TemplateServiceConfig.interface
    val port = TemplateServiceConfig.port
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)

    logger.info(s"start http server on $interface:$port")
    Http().bindAndHandle((new MainRoute).myRoute, interface, port)

  }

  private def registerShutdownHooks() = {

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        bindingFuture
          .flatMap(_.unbind())
          .onComplete(_ => system.terminate())
      }
    })

  }

}
