package server

import cats.effect.{ExitCode, IO}
import com.comcast.ip4s.{Host, IpAddress, IpLiteralSyntax, Port, SocketAddress}
import fs2.io.net.{Datagram, Network, Socket}
import fs2.{Chunk, Stream}

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.concurrent.duration.DurationInt

object Server {
  case class CarState(uuid: UUID, x: Double, y: Double, vx: Double, vy: Double, direction: Double)

  val defaultUUID: UUID = UUID.randomUUID()

  //val socketRef: IO[Ref[IO, Option[Socket[IO]]]] = Ref.of[IO, Option[Socket[IO]]](None)
  var socketUnsafe: Option[Socket[IO]] = None
  var usernameUnsafe: String = "User" + (100 + math.random() * 100).floor.toInt
  var readyUnsafe = false
  var lobbyUnsafe: String = "No lobby"

  object MsgType extends Enumeration {
    val Handshake: Byte = 0x01
    val ReadyUpdate: Byte = 0x02
    val LobbyState: Byte = 0x03
    val GameStart: Byte = 0x04
  }

  // Call whenever it will be needed to send a ready packet (working fine)
  def sendReady(socket: Socket[IO], uuid: UUID, isReady: Boolean): IO[Unit] = {
    val payload = {
      val dataLength = 1 + 16 + 1
      val buf = ByteBuffer
        .allocate(2 + dataLength)
        .order(ByteOrder.BIG_ENDIAN)

      buf.putShort(dataLength.toShort)
      buf.put(MsgType.ReadyUpdate)
      buf.putLong(uuid.getMostSignificantBits)
      buf.putLong(uuid.getLeastSignificantBits)
      buf.put(if (isReady) 1.toByte else 0.toByte)
      buf.flip()

      Chunk.byteBuffer(buf)
    }
    readyUnsafe = isReady
    socket.write(payload).void
  }

  def tcpClient(serverHost: Host, serverPort: Port, uuid: UUID, username: String): Stream[IO, Unit] =
    Stream.resource(Network[IO].client(SocketAddress(serverHost, serverPort))).flatMap { socket =>
      socketUnsafe = Some(socket)
      // Send uuid and username to the server
      Stream.eval {
        val usernameLength = username.length
        val dataLength = 1 + 16 + 1 + usernameLength
        val buf = ByteBuffer.allocate(2 + dataLength).order(ByteOrder.BIG_ENDIAN)
        buf.putShort(dataLength.toShort)
        buf.put(MsgType.Handshake)
        buf.putLong(uuid.getMostSignificantBits)
        buf.putLong(uuid.getLeastSignificantBits)
        buf.put(usernameLength.toByte)
        buf.put(username.getBytes(StandardCharsets.UTF_8))
        buf.flip()
        socket.write(Chunk.ByteBuffer(buf))
      } ++
        Stream.repeatEval {
          socket.readN(2).map(_.toArray).flatMap { header =>
            val len = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN).getShort
            socket.readN(len).map(_.toArray)
          }
        }.evalMap { payload =>
          decodeTCP(payload)
          IO.unit
        }
    }

  def udpInputSender(serverHost: IpAddress, serverPort: Port): Stream[IO, Unit] =
    Stream.resource(Network[IO].openDatagramSocket()).flatMap { socket =>
      val serverAddr = SocketAddress[IpAddress](serverHost, serverPort)

      // Simulate sending an input every second
      Stream.awakeEvery[IO](1.second).evalMap { _ =>
        val input = s"${defaultUUID}${-0.4}${0.8}${true}"
        val datagram = Datagram(serverAddr, Chunk.array(input.getBytes(StandardCharsets.UTF_8)))
        socket.write(datagram)
      }.concurrently {
        socket.reads.evalMap { datagram =>
          val msg = decodeCarState(datagram.bytes.toArray)
          IO.println(s"[UDP] Car update received: ${msg.getOrElse(Array.empty[CarState]).mkString(", ")}")
        }
      }
    }

  val recordSize: Int = 16 + 5 * 4

  def decodeCarState(bytes: Array[Byte]): Option[Array[CarState]] = {

    if (bytes.length < 2) return None

    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

    val count = bb.getShort

    if (count > 0 && bytes.length == 2 + count * recordSize) {
      val carStates = new Array[CarState](count)

      for (i <- 0 until count) {
        val msb = bb.getLong
        val lsb = bb.getLong
        val uuid = new UUID(msb, lsb)

        val x = bb.getFloat
        val y = bb.getFloat

        val vx = bb.getFloat
        val vy = bb.getFloat

        val dir = bb.getFloat

        carStates(i) = CarState(uuid, x, y, vx, vy, dir)
      }

      Some(carStates)
    } else Some(Array.empty[CarState])
  }

  def decodeTCP(bytes: Array[Byte]): Unit = {
    if (bytes.length < 1) return

    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

    bb.get() match {
      case MsgType.LobbyState =>
        val nbPlayers = bb.getShort
        val nbReady = bb.getShort
        val timeBeforeStart = bb.getShort match {
          case 99 => "Waiting..."
          case 0 => "NOW !"
          case seconds => s"Starts in $seconds"
        }

        val userlistString = for (_ <- 0 until nbPlayers) yield {
          val usernameLength = bb.get()
          val usernameBytes = new Array[Byte](usernameLength)
          bb.get(usernameBytes)
          val username = new String(usernameBytes, StandardCharsets.UTF_8)
          val isReady = bb.get() != 0
          s"${if (isReady) "READY    " else "NOT READY"} - $username"
        }

        lobbyUnsafe = s"$nbReady/$nbPlayers player${if (nbReady > 1) "s" else ""}\n$timeBeforeStart\n${userlistString.mkString("\n")}"
      case _ => println("Unknown TCP code")
    }
  }

  def init(username: String, mode: String = "PROD"): IO[ExitCode] = {
    var serverHost = host"iscracer.allanbrunner.dev"
    var serverIp = ip"91.214.191.159"
    if (mode == "TEST") {
      serverHost = host"127.0.0.1"
      serverIp = ip"127.0.0.1"
    }
    val tcpPort = port"9000"
    val udpInputPort = port"5555"

    usernameUnsafe = username

    val streams = Stream(
      tcpClient(serverHost, tcpPort, defaultUUID, username),
      udpInputSender(serverIp, udpInputPort)
    ).parJoinUnbounded

    streams.compile.drain.as(ExitCode.Success)
  }
}
