import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, IpAddress, IpLiteralSyntax, Port, SocketAddress}
import fs2.io.net.{Datagram, Network}
import fs2.{Chunk, Stream}

import java.net.InetAddress
import java.nio.{ByteBuffer, ByteOrder}
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.concurrent.duration.DurationInt

object ConnectionTests extends IOApp {

  case class CarState(uuid: UUID, x: Double, y: Double, vx: Double, vy: Double, direction: Double)

  val defaultUUID = UUID.randomUUID()

  def tcpClient(serverHost: Host, serverPort: Port, uuid: UUID, username: String): Stream[IO, Unit] =
    Stream.resource(Network[IO].client(SocketAddress(serverHost, serverPort))).flatMap { socket =>
      // Send uuid and username to the server
      Stream.eval {
        val message = s"$uuid;$username"
        IO.println(s"Sending $message") *>
        socket.write(Chunk.array(message.getBytes(StandardCharsets.UTF_8)))
      } ++
      socket.reads
        .through(fs2.text.utf8.decode)
        .evalMap(msg => IO.println(s"[TCP] Lobby update: $msg"))
        .handleErrorWith(e => Stream.eval(IO.println(s"[TCP] Error: ${e.getMessage}")))
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
          val msg = decode(datagram.bytes.toArray)
          IO.println(s"[UDP] Car update received: ${msg.getOrElse(Array.empty[CarState]).mkString(", ")}")
        }
      }
    }

  val recordSize = 16 + 5 * 4

  def decode(bytes: Array[Byte]): Option[Array[CarState]] = {

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

  def run(args: List[String]): IO[ExitCode] = {
    var serverHost = host"iscracer.allanbrunner.dev"
    var serverIp = ip"91.214.191.159"
    if (args.head == "TEST") {
      serverHost = host"127.0.0.1"
      serverIp = ip"127.0.0.1"
    }
    val tcpPort = port"9000"
    val udpInputPort = port"5555"

    val streams = Stream(
      tcpClient(serverHost, tcpPort, defaultUUID, "Zeksax"),
      udpInputSender(serverIp, udpInputPort)
    ).parJoinUnbounded

    streams.compile.drain.as(ExitCode.Success)
  }
}
