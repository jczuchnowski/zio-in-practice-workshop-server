package org.github.jczuchnowski.zio.workshop.server

import java.nio.channels.{ CancelledKeyException, SocketChannel => JSocketChannel }

import zio._
import zio.clock._
import zio.console._
import zio.duration._
import zio.nio.channels.AsynchronousServerSocketChannel
import zio.nio.core.channels._
import zio.nio.core.channels.SelectionKey.Operation
import zio.nio.core._
import zio.stream._

import scala.language.postfixOps
import zio.nio.channels.AsynchronousSocketChannel
import java.io.IOException
import java.util.concurrent.TimeUnit

object PokemonServer extends App {

  def run(args: List[String]): ZIO[Clock with Console, Nothing, Int] =
    Server.startAsync()
      .foldM(e => putStrLn("Error: " + e.getMessage) *> ZIO.succeed(1), _ => ZIO.succeed(0))
}

object Server {

  val addressIo = SocketAddress.inetSocketAddress("0.0.0.0", 9002)

  def bytesToString(bytes: Chunk[Byte]): String =
    bytes.map(_.toChar).mkString

  def safeStatusCheck(statusCheck: IO[CancelledKeyException, Boolean]): ZIO[Clock with Console, Nothing, Boolean] =
    statusCheck.either.map(_.getOrElse(false))

  def handle(client: SocketChannel, reqCount: Ref[Int]): ZIO[Clock with Console, Exception, Unit] = 
    for {
      buffer <- Buffer.byte(256)
      _      <- client.read(buffer)
      _      <- buffer.flip
      bytes  <- buffer.getChunk()
      array  =  bytes.toArray
      string =  new String(array).trim()
      text   =  bytesToString(bytes)
      c      <- reqCount.update(_ + 1)
      _      <- putStrLn("request num: " + c)
      _      <- putStrLn("request as string: \n" + string)
      num    =  java.nio.ByteBuffer.allocate(4).putInt(PokemonService.pokemons.length).array()
      _      <- client.write(Chunk.fromArray(num))
      _      <- ZIO.foreach(PokemonService.pokemons) { pokemon =>
                  client.write(PokemonEncoder.toBytes(pokemon))
                }
      // b      <- Buffer.byte(PokemonEncoder.toBytes(PokemonService.pokemons.head))
      // _      <- client.write(b)
//      _      <- client.close
    } yield ()

  def server(addr: InetSocketAddress, selector: Selector): IO[IOException, ServerSocketChannel] = 
    for {
      channel  <- ServerSocketChannel.open
      _        <- channel.bind(addr)
      _        <- channel.configureBlocking(false)
      ops      <- channel.validOps
      _        <- channel.register(selector, ops)
    } yield channel

  def startAsync(): ZIO[Clock with Console, Exception, Unit] = {
    def serverLoop(
      selector: Selector,
      channel: ServerSocketChannel,
      reqCount: Ref[Int]
    ): ZIO[Clock with Console, Exception, Unit] = {
      
      def whenIsAcceptable(key: SelectionKey): ZIO[Clock with Console, IOException, Unit] = 
        ZIO.whenM(safeStatusCheck(key.isAcceptable)) {
          for {
            clientOpt <- channel.accept
            client    =  clientOpt.get
            _         <- client.configureBlocking(false)
            _         <- client.register(selector, Operation.Read)
            _         <- putStrLn("Connection accepted")
          } yield ()
        }

      def whenIsReadable(key: SelectionKey): ZIO[Clock with Console, Exception, Unit] = 
        ZIO.whenM(safeStatusCheck(key.isReadable)) {
          for {
            _       <- putStrLn("isReadable")
            sClient <- key.channel
            _       <- Managed.make(IO.effectTotal(new SocketChannel(sClient.asInstanceOf[JSocketChannel])))(_.close.orDie).use { client =>
                         for {
                           _ <- handle(client, reqCount)
                         } yield ()
                       }
          } yield ()
        }

      for {
        _            <- putStrLn("waiting for connection...")
        _            <- selector.select
        selectedKeys <- selector.selectedKeys
        _            <- ZIO.foreach_(selectedKeys) { key =>
                          whenIsAcceptable(key) *> 
                          whenIsReadable(key) *>
                          selector.removeKey(key)
                        }
      } yield ()
    }

    for {
      reqCount <- Ref.make(0)
      address  <- addressIo
      selector <- Selector.make
      channel  <- server(address, selector)
      _        <- serverLoop(selector, channel, reqCount).forever
    } yield ()

    // val startServer: Managed[Exception, (Selector, ServerSocketChannel, Ref[Int])] = for {
    //   reqCount <- Ref.make(0).toManaged_
    //   address  <- addressIo.toManaged_
    //   selector <- Managed.make(Selector.make)(_.close.orDie)
    //   channel  <- Managed.make(server(address, selector))(_.close.orDie)
    // } yield (selector, channel, reqCount)

  }
}
