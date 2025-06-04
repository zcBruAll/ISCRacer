package server

import cats.effect.{ExitCode, IO}
import com.comcast.ip4s.{Host, IpAddress, IpLiteralSyntax, Port, SocketAddress}
import fs2.io.net.{Datagram, Network, Socket}
import fs2.{Chunk, Stream}
import game.{Motor, PlayerInput}
import cats.effect.unsafe.implicits.global

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.concurrent.duration.DurationInt

object Server {
  case class CarState(uuid: UUID, x: Float, y: Float, vx: Float, vy: Float, direction: Float)
  case class PlayerState(uuid: UUID, ts: Long, segment: Int, segmentDist: Float, laps: Int, lapsDist : Float, lapTime: Long, totalTime: Long, bestLap: Long, lastLap: Long)

  val defaultUUID: UUID = UUID.randomUUID()

  //val socketRef: IO[Ref[IO, Option[Socket[IO]]]] = Ref.of[IO, Option[Socket[IO]]](None)
  var socketUnsafe: Option[Socket[IO]] = None
  var usernameUnsafe: String = "Player " + (100 + math.random() * 100).floor.toInt
  var readyUnsafe = false
  var lobbyUnsafe: String = "No lobby"

  object MsgType extends Enumeration {
    val Handshake: Byte = 0x01
    val ReadyUpdate: Byte = 0x02
    val LobbyState: Byte = 0x03
    val GameInit: Byte = 0x04
    val GameStart: Byte = 0x05
    val CarState: Byte = 0x11
    val PlayerInput: Byte = 0x12
    val PlayerState: Byte = 0x13
  }

  // Call whenever it will be needed to send a ready packet (working fine)
  def sendReady(socket: Socket[IO], uuid: UUID, isReady: Boolean, readyMode: Byte): IO[Unit] = {
    val payload = {
      val dataLength = 1 + 16 + 1
      val buf = ByteBuffer
        .allocate(2 + dataLength)
        .order(ByteOrder.BIG_ENDIAN)

      buf.putShort(dataLength.toShort)
      buf.put(readyMode)
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

      socket.reads.evalMap { datagram =>
        datagram.bytes.toArray.head match {
          case MsgType.CarState => val
            carStates = decodeCarState(datagram.bytes.toArray.tail)
            val personalCarState = carStates.getOrElse(Array.empty[CarState]).find(_.uuid.equals(defaultUUID))
            if (personalCarState.isDefined && Motor.kart != null) {
              val car = personalCarState.get
              Motor.kart.x = car.x
              Motor.kart.y = car.y
              Motor.kart.speedX = car.vx
              Motor.kart.speedY = car.vy
              Motor.kart.angle = car.direction
            }
            IO.println(s"[UDP] Car update received: ${carStates.getOrElse(Array.empty[CarState]).mkString(", ")}")
          case MsgType.PlayerState =>
            val playerStates = decodePlayerState(datagram.bytes.toArray.tail)
            val personalPlayerState = playerStates.getOrElse(Array.empty[PlayerState]).find(_.uuid.equals(defaultUUID))
            if (personalPlayerState.isDefined) Motor.player = personalPlayerState.get
            IO.println(s"[UDP] Player update received: ${playerStates.getOrElse(Array.empty[PlayerState]).mkString(", ")}")
        }
      }.concurrently {
          Stream.awakeEvery[IO](33.millis).evalMap { _ =>
            if (!Motor.startGame) IO.unit
            else {
              val inputsByte = encodeInputs(Motor.inputs.get.unsafeRunSync())
              val datagram = Datagram(serverAddr, inputsByte)
              socket.write(datagram)
            }
          }
      }
    }

  val carStateRecordSize: Int = 16 + 5 * 4
  def decodeCarState(bytes: Array[Byte]): Option[Array[CarState]] = {

    if (bytes.length < 2) return None

    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

    val count = bb.getShort

    if (count > 0 && bytes.length == 2 + count * carStateRecordSize) {
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

  val playerStateRecordSize: Int = 16 + 4 * 4 + 4 * 8
  def decodePlayerState(bytes: Array[Byte]): Option[Array[PlayerState]] = {
    if (bytes.length < 2) return None

    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

    val count = bb.getShort

    if (count > 0 && bytes.length == 2 + count * playerStateRecordSize) {
      val playerStates = new Array[PlayerState](count)

      for (i <- 0 until count) {
        val msb = bb.getLong
        val lsb = bb.getLong
        val uuid = new UUID(msb, lsb)

        val segment = bb.getInt
        val segmentDist = bb.getFloat
        val laps = bb.getInt
        val lapsDist = bb.getFloat
        val lapTime = bb.getLong
        val totalTime = bb.getLong
        val bestLap = bb.getLong
        val lastLap = bb.getLong

      playerStates(i) = PlayerState(uuid, System.currentTimeMillis(), segment, segmentDist, laps, lapsDist, lapTime, totalTime, bestLap, lastLap)
      }

      Some(playerStates)
    } else Some(Array.empty[PlayerState])
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
      case MsgType.GameInit =>
        val mapNameLength = bb.get()
        val mapBytes = new Array[Byte](mapNameLength)
        bb.get(mapBytes)
        val mapName = new String(mapBytes, StandardCharsets.UTF_8)

        val x0 = bb.getShort
        val y0 = bb.getShort
        val direction = bb.getFloat

        Motor.gameSettings = (mapName, x0, y0, direction)
        Motor.initGame = true

      case MsgType.GameStart =>
        val tmr = bb.getShort
        Motor.timer = if (tmr == 99) "WAITING..."
        else if (tmr == 0) {
          Motor.startGame = true
          "GO!"
        } else if (tmr == -1) "" else tmr.toString
      case _ =>
    }
  }

  def encodeInputs(inputs: PlayerInput): Chunk[Byte] = {
    // uuid / throttle / steer / drift
    val packetSize = 16 + 4 + 4 + 1
    val buf = ByteBuffer.allocate(packetSize).order(ByteOrder.BIG_ENDIAN)

    val throttle = math.min(1, math.max(-1, math.max(inputs.forwardKB, inputs.forwardC) + math.min(inputs.backwardKB, inputs.backwardC)))
    val steer = math.min(1, math.max(-1, math.max(inputs.steerRightKB, inputs.steerRightC) + math.min(inputs.steerLeftKB, inputs.steerLeftC)))
    val drift = inputs.driftKB || inputs.driftC

    buf.putLong(defaultUUID.getMostSignificantBits)
    buf.putLong(defaultUUID.getLeastSignificantBits)
    buf.putFloat(throttle)
    buf.putFloat(steer)
    buf.put(if (drift) 1.toByte else 0.toByte)

    buf.flip()

    Chunk.byteBuffer(buf)
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
