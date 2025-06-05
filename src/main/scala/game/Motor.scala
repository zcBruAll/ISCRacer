package game

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.unsafe.implicits.global
import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import menu.Menu
import server.Server
import server.Server.{PlayerState, defaultUUID}
import utils.Conversion.formatTime
import utils.{Conversion, GraphicsUtils}

import java.util.UUID

case class PlayerInput(forwardKB: Float = 0, backwardKB: Float = 0, steerLeftKB: Float = 0, steerRightKB: Float = 0, driftKB: Boolean = false,
                       forwardC: Float = 0, backwardC: Float = 0, steerLeftC: Float = 0, steerRightC: Float = 0, driftC: Boolean = false)
import utils.GraphicsUtils
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.scenes.scene2d.ui.Slider

import javax.naming.ldap.Control

object Motor {
  val inputs: Ref[IO, PlayerInput] = Ref.of[IO, PlayerInput](PlayerInput()).unsafeRunSync()

  private var mode7Renderer: Mode7Renderer = _

  private var _kart: Kart = _
  def kart: Kart = _kart

  private var _camera: Camera = _
  def camera: Camera = _camera

  private var _track: Track = _
  def track: Track = _track

  private var _player: PlayerState = PlayerState(defaultUUID, "Player 000", System.currentTimeMillis(), 0, 0f, 0, 0f, 0L, 0L, 0L, 0L)
  def player: PlayerState = _player
  def player_=(value: PlayerState): Unit = _player = value

  private var _players: Map[UUID, PlayerState] = Map.empty[UUID, PlayerState]
  def players: Map[UUID, PlayerState] = _players
  def players_=(value: Map[UUID, PlayerState]): Unit = _players = value

  private var debug: Boolean = true

  private var personalTimeFont: BitmapFont = _
  private var totalTimeFont: BitmapFont = _
  private var lapTimeFont: BitmapFont = _

  private val menu: Menu = Menu.menu

  private val width: Int = menu.width
  private val height: Int = menu.height
  private val horizon: Int = height / 3

  var initGame: Boolean = false
  private var initiated: Boolean = false
  var timer: String = ""
  var startGame: Boolean = false
  var endGame: Boolean = false

  var gameSettings: (String, Int, Int, Float) = _

  var results: Map[UUID, (String, Long, Long)] = Map.empty[UUID, (String, Long, Long)]
  def resultsFormatted: String = {
    if (results.isEmpty) return ""
    "Best lap times:\n" +
    {
      val temp = results.toIndexedSeq.sortBy(p => p._2._2)
      val selfIndex = temp.map(_._1).indexOf(defaultUUID)

      var top3 = ""
      for (i <- 0 until math.min(3, temp.length)) {
        val currentResult = temp(i)
        if (selfIndex == i) top3 += "YOU "
        top3 += s"${i+1}) ${currentResult._2._1}: ${formatTime(currentResult._2._2)}\n"
      }

      if (selfIndex > 3) top3 + s"YOU ${selfIndex+1}) ${temp(selfIndex)._2._1}: ${formatTime(temp(selfIndex)._2._2)}\n" else top3
    } +
    "\nBest total time:\n" +
    {
      val temp = results.toIndexedSeq.sortBy(p => p._2._3)
      val selfIndex = temp.map(_._1).indexOf(defaultUUID)

      var top3 = ""
      for (i <- 0 until math.min(3, temp.length)) {
        val currentResult = temp(i)
        if (selfIndex == i) top3 += "YOU "
        top3 += s"${i+1}) ${currentResult._2._1}: ${formatTime(currentResult._2._3)}\n"
      }

      if (selfIndex > 3) top3 + s"YOU ${selfIndex+1}) ${temp(selfIndex)._2._1}: ${formatTime(temp(selfIndex)._2._3)}\n" else top3
    }
  }

  def init(map: String, x0: Int, y0: Int, direction: Float): Unit = {
    _track = new Track(map)
    initKart(x0, y0, direction)
    _camera = new Camera()

    mode7Renderer = new Mode7Renderer(_track.mapTexture)

    initFonts()

    initiated = true
    endGame = false

    Server.sendReady(Server.socketUnsafe.get, Server.defaultUUID, isReady = true, Server.MsgType.GameStart).unsafeRunAndForget()
  }

  def render(g: GdxGraphics): Unit = {
    g.clear()

    // Fills the top of the screen with sky color (from top to horizon)
    g.setColor(Color.SKY)
    g.drawFilledRectangle(width / 2, height - (horizon / 2), width, horizon, 0)

    _camera.update(_kart)

    mode7Renderer.render(_camera.x, _camera.y, _camera.angle, _camera.scale, _camera.fov, horizon)

    g.drawTransformedPicture(width / 2, height / 4, 0, 3, _kart.texture)

    g.drawStringCentered(150, Conversion.longToTimeString(_player.lapTime), lapTimeFont)
    g.drawStringCentered(80, Conversion.longToTimeString(_player.totalTime), totalTimeFont)
    if (timer != "") g.drawStringCentered(540, timer, lapTimeFont)

    if (debug) displayDebug(g)
    displayLeaderboard(g)
    GraphicsUtils.drawFPS(g, Color.WHITE, 5f, height - 10)
  }

  private def initKart(x0: Int, y0: Int, direction: Float): Unit = {
    _kart = new Kart()
    _kart.x = x0
    _kart.y = y0
    _kart.angle = direction
  }
  def onControllerConnected(controller: Controller): Unit = {}

  def onControllerDisconnected(controller: Controller){}

  def onControllerAxisMoved(controller: Controller, axisCode: Int, value: Float): Unit = {

    // Gauche Droite
    var vDirection = controller.getAxis(0)
    if(vDirection > 0.07) inputs.update(p => p.copy(steerRightC = vDirection)).unsafeRunSync()
    else if(vDirection < -0.07) inputs.update(p => p.copy(steerLeftC = vDirection)).unsafeRunSync()

    // Avancer Reculer
    val vForw = controller.getAxis(4)
    println(s"a/r = $vForw, g/d = $vDirection")
    if(vForw >= 1.0) {
      inputs.update(p => p.copy(forwardC = 0)).unsafeRunSync()
      inputs.update(p => p.copy(backwardC = 0)).unsafeRunSync()
    }
    else if(vForw >= 0.4 && vForw < 1.0) inputs.update(p => p.copy(backwardC = -vForw)).unsafeRunSync()
    else if(vForw <= -0.4) inputs.update(p => p.copy(forwardC = -vForw)).unsafeRunSync()
    else {
      inputs.update(p => p.copy(forwardC = 0)).unsafeRunSync()
      inputs.update(p => p.copy(backwardC = 0)).unsafeRunSync()
    }
  }

  def onControllerKeyDown(controller: Controller, buttonCode: Int): Unit = {
    buttonCode match{
      case 0 => inputs.update(p => p.copy(driftKB = true)).unsafeRunSync()
      case 4 => inputs.update(p => p.copy(driftKB = true)).unsafeRunSync()
      case _ =>
      // 0 et 4 por drifter (l1 et A)
      }
  }

  def onControllerKeyUp(controller: Controller, buttonCode: Int): Unit = {
    buttonCode match{
      case 0 => inputs.update(p => p.copy(driftKB = false)).unsafeRunSync()
      case 4 => inputs.update(p => p.copy(driftKB = false)).unsafeRunSync()
      case _ =>
    }
  }

  def onKeyDown(keycode: Int): Unit = {
    if (!initiated) return
    keycode match {
      case Keys.W => inputs.update(p => p.copy(forwardKB = 1f)).unsafeRunSync()
      case Keys.S => inputs.update(p => p.copy(backwardKB = -1f)).unsafeRunSync()
      case Keys.A => inputs.update(p => p.copy(steerLeftKB = -1f)).unsafeRunSync()
      case Keys.D => inputs.update(p => p.copy(steerRightKB = 1f)).unsafeRunSync()
      case Keys.SHIFT_LEFT => inputs.update(p => p.copy(driftKB = true)).unsafeRunSync()
      case _ =>
    }
  }

  def onKeyUp(keycode: Int): Unit = {
    if (!initiated) return
    keycode match {
      case Keys.W => inputs.update(p => p.copy(forwardKB = 0f)).unsafeRunSync()
      case Keys.S => inputs.update(p => p.copy(backwardKB = 0f)).unsafeRunSync()
      case Keys.A => inputs.update(p => p.copy(steerLeftKB = 0f)).unsafeRunSync()
      case Keys.D => inputs.update(p => p.copy(steerRightKB = 0f)).unsafeRunSync()
      case Keys.SHIFT_LEFT => inputs.update(p => p.copy(driftKB = false)).unsafeRunSync()
      case _ =>
    }
  }

  def initFonts(): Unit = {
    val consola = Gdx.files.internal("src/main/assets/fonts/consola.ttf")
    val generator = new FreeTypeFontGenerator(consola)

    val paramTotalTime = new FreeTypeFontGenerator.FreeTypeFontParameter
    paramTotalTime.color = Color.WHITE
    paramTotalTime.size = generator.scaleForPixelHeight(36)
    paramTotalTime.hinting = FreeTypeFontGenerator.Hinting.Full
    totalTimeFont = generator.generateFont(paramTotalTime)

    val paramPersonalTime = new FreeTypeFontGenerator.FreeTypeFontParameter
    paramTotalTime.color = Color.LIME
    paramTotalTime.size = generator.scaleForPixelHeight(36)
    paramTotalTime.hinting = FreeTypeFontGenerator.Hinting.Full
    personalTimeFont = generator.generateFont(paramTotalTime)

    val paramLapTime = new FreeTypeFontGenerator.FreeTypeFontParameter
    paramLapTime.color = Color.WHITE
    paramLapTime.size = generator.scaleForPixelHeight(72)
    paramLapTime.hinting = FreeTypeFontGenerator.Hinting.Full
    lapTimeFont = generator.generateFont(paramLapTime)
  }

  def displayDebug(g: GdxGraphics): Unit = {
    g.drawString(10, 20, "Laps: " + _player.laps)
    g.drawString(10, 40, "Segment: " + _player.segment)
    g.drawString(10, 60, "SegDist: " + _player.segmentDist)
    g.drawString(10, 80, "TotDist: " + _player.lapsDist)

    g.drawString(10, 220, "Speed: " + _kart.speedX.toString)
    g.drawString(10, 240, "X: " + _kart.x.toString)
    g.drawString(10, 260, "Y: " + _kart.y.toString)
    g.drawString(10, 280, "Angle: " + _kart.angle.toString)

    g.drawString(10, 320, "X: " + _camera.x.toString)
    g.drawString(10, 340, "Y: " + _camera.y.toString)
    g.drawString(10, 360, "Angle: " + _camera.angle.toString)

    g.drawString(10, 400, "Best Lap: " + Conversion.longToTimeString(_player.bestLap))
    g.drawString(10, 420, "Last Lap: " + Conversion.longToTimeString(_player.lastLap))
  }

  def displayLeaderboard(g: GdxGraphics): Unit = {
    // 1) Sort all players by progress: (laps DESC, segment DESC, segmentDist DESC)
    val sortedPlayers: IndexedSeq[PlayerState] =
      players.values.toIndexedSeq
        .filter(_.bestLap != 0).sortBy(p => p.bestLap)

    // 2) Find the index (0-based) of local player in that sorted list
    val selfRankIndex = sortedPlayers.indexWhere(_.uuid == defaultUUID)
    //   If selfRankIndex == -1, the local player isn't in the map; assume no draw.

    // 3) Decide base coordinates and vertical spacing:
    val xPos     = 1900            // same X you used for "Leaderboard" heading
    val yStart   = 450             // Y for "1st" entry
    val ySpacing = 50              // pixels between each line

    // 4) Draw the title (you already do this above, but ensure it’s here or before)
    g.drawString(1900, 500, "Leaderboard", totalTimeFont, Align.right)

    // 5) Draw positions 1 → 3 (if they exist)
    for (i <- 0 until math.min(3, sortedPlayers.length)) {
      val p = sortedPlayers(i)
      val text = s"${i + 1}. ${p.username}  ${formatTime(p.bestLap)}"
      g.drawString(xPos, yStart - i * ySpacing, text, if (i == selfRankIndex) personalTimeFont else totalTimeFont, Align.right)
    }

    // 6) If self is exactly index 3 (i.e. 4th place), draw them at 4th:
    if (selfRankIndex == 3) {
      val p4 = sortedPlayers(3)
      val text4 = s"4. ${p4.username}  ${formatTime(p4.bestLap)}"
      g.drawString(xPos, yStart - 3 * ySpacing, text4, personalTimeFont, Align.right)
    }
    // 7) If self is beyond index 3 (>3), draw ellipsis + neighbours
    else if (selfRankIndex > 3) {
      // 7a) Draw "..." at the 4th‐place slot (index 3)
      g.drawString(xPos, yStart - 3 * ySpacing, "...", totalTimeFont, Align.right)

      // 7b) Draw the player immediately above self (rank = selfRankIndex)
      val above = sortedPlayers(selfRankIndex - 1)
      val textAbove = s"${selfRankIndex}. ${above.username}  ${formatTime(above.bestLap)}"
      g.drawString(xPos, yStart - 4 * ySpacing, textAbove, totalTimeFont, Align.right)

      // 7c) Draw self (rank = selfRankIndex + 1)
      val selfP = sortedPlayers(selfRankIndex)
      val textSelf = s"${selfRankIndex + 1}. ${selfP.username}  ${formatTime(selfP.bestLap)}"
      g.drawString(xPos, yStart - 5 * ySpacing, textSelf, personalTimeFont, Align.right)

      // 7d) If there is a player immediately behind self, draw them too
      if (selfRankIndex + 1 < sortedPlayers.length) {
        val below = sortedPlayers(selfRankIndex + 1)
        val textBelow = s"${selfRankIndex + 2}. ${below.username}  ${formatTime(below.bestLap)}"
        g.drawString(xPos, yStart - 6 * ySpacing, textBelow, totalTimeFont, Align.right)
      }
    }
  }

  /**
   * Disposes resources on application exit.
   */
  def dispose(): Unit = {
    mode7Renderer.dispose()
    _kart.dispose()
    _track.dispose()
    initiated = false
  }
}
