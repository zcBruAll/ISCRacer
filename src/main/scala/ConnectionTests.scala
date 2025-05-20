import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, IpAddress, IpLiteralSyntax, Port, SocketAddress}
import fs2.io.net.{Datagram, Network}
import fs2.{Chunk, Stream}

import java.net.InetAddress
import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt

object ConnectionTests extends IOApp {

  def tcpClient(serverHost: Host, serverPort: Port): Stream[IO, Unit] =
    Stream.resource(Network[IO].client(SocketAddress(serverHost, serverPort))).flatMap { socket =>
      socket.reads
        .through(fs2.text.utf8.decode)
        .evalMap(msg => IO.println(s"[TCP] Lobby update: $msg"))
        .handleErrorWith(e => Stream.eval(IO.println(s"[TCP] Error: ${e.getMessage}")))
    }

  def udpInputSender(serverHost: IpAddress, serverPort: Port, playerId: Int): Stream[IO, Unit] =
    Stream.resource(Network[IO].openDatagramSocket()).flatMap { socket =>
      val serverAddr = SocketAddress[IpAddress](serverHost, serverPort)

      // Simulate sending an input every second
      Stream.awakeEvery[IO](1.second).evalMap { _ =>
        val input = s"$playerId,moveForward"  // You can replace with actual game inputs
        val datagram = Datagram(serverAddr, Chunk.array(input.getBytes(StandardCharsets.UTF_8)))
        socket.write(datagram)
      }
    }

  def udpStateReceiver(bindPort: Port): Stream[IO, Unit] =
    Stream.resource(Network[IO].openDatagramSocket(port = Some(bindPort))).flatMap { socket =>
      socket.reads.evalMap { datagram =>
        val msg = new String(datagram.bytes.toArray, StandardCharsets.UTF_8)
        IO.println(s"[UDP] Car update received: $msg")
      }
    }

  def run(args: List[String]): IO[ExitCode] = {
    val serverHost = host"iscracer.allanbrunner.dev"
    val serverIp = ip"91.214.191.159"
    val tcpPort = port"9000"
    val udpInputPort = port"5555"
    val udpListenPort = port"6001"  // Change per client
    val playerId = 1  // Change per client

    val streams = Stream(
      tcpClient(serverIp, tcpPort),
      udpInputSender(serverIp, udpInputPort, playerId),
      udpStateReceiver(udpListenPort)
    ).parJoinUnbounded

    streams.compile.drain.as(ExitCode.Success)
  }
}
