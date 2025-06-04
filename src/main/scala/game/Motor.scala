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
import menu.Menu
import server.Server
import server.Server.{PlayerState, defaultUUID}
import utils.{Conversion, GraphicsUtils}

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

  private var _player: PlayerState = PlayerState(defaultUUID, System.currentTimeMillis(), 0, 0f, 0, 0f, 0L, 0L, 0L, 0L)
  def player: PlayerState = _player
  def player_=(value: PlayerState): Unit = _player = value

  private var debug: Boolean = true

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

  var gameSettings: (String, Int, Int, Float) = _

  def init(map: String, x0: Int, y0: Int, direction: Float): Unit = {
    _track = new Track(map)
    initKart(x0, y0, direction)
    _camera = new Camera()

    mode7Renderer = new Mode7Renderer(_track.mapTexture)

    initFonts()

    initiated = true

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
    g.drawString(10, 80, "TotDist: " + _player.totalTime)

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
