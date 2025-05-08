package game

import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.Color
import menu.Menu
import utils.GraphicsUtils

object Motor {
  private var mode7Renderer: Mode7Renderer = _

  private var kart: Kart = _
  private var camera: Camera = _
  private var track: Track = _
  private var player: Player = _

  private var forward: Boolean = false
  private var backward: Boolean = false
  private var left: Boolean = false
  private var right: Boolean = false
  private var drift: Boolean = false

  private var debug: Boolean = true

  private var totalTimeFont: BitmapFont = _
  private var lapTimeFont: BitmapFont = _

  private val menu: Menu = Menu.menu

  private val width: Int = menu.width
  private val height: Int = menu.height
  private val horizon: Int = height / 3

  private var initiated: Boolean = false

  def init(map: String): Unit = {
    track = new Track(map)
    initKart()
    camera = new Camera()
    player = new Player(track)

    mode7Renderer = new Mode7Renderer(track.mapTexture)

    initFonts()

    initiated = true
  }

  def render(g: GdxGraphics): Unit = {
    g.clear()

    // Fills the top of the screen with sky color (from top to horizon)
    g.setColor(Color.SKY)
    g.drawFilledRectangle(width / 2, height - (horizon / 2), width, horizon, 0)

    if (forward)
      kart.accelerate()
    else if (backward && kart.speed > 0)
      kart.brake()
    else if (backward)
      kart.accelerate(false)
    else
      kart.applyFriction()

    if (drift && (left || right))
      kart.drift(right)
    else if (left || right)
      kart.rotate(right)

    kart.update()
    kart.move()

    val segmentInfo = track.closestSegmentAndProgress(kart.x, kart.y)

    player.update(segmentInfo._1, kart)

    camera.update(kart)

    mode7Renderer.render(camera.x + camera.offsetX, camera.y + camera.offsetY, camera.angle, camera.scale, camera.fov, horizon)

    g.drawTransformedPicture(width / 2, height / 4, 0, 3, kart.texture)

    g.drawStringCentered(150, player.lapTime, lapTimeFont)
    g.drawStringCentered(80, player.totalTime, totalTimeFont)

    if (debug) displayDebug(g, segmentInfo)
    GraphicsUtils.drawFPS(g, Color.WHITE, 5f, height - 10)
  }

  private def initKart(): Unit = {
    kart = new Kart()
    kart.fps = menu.gd.getDisplayMode.getRefreshRate
    if (track.checkpoints.nonEmpty) {
      val c0 = track.checkpoints.head
      val c1 = track.checkpoints(1)
      kart.x = c0.x
      kart.y = c0.y
      kart.angle = math.tan((c1.y - c0.y) / (c1.x - c0.x)).toFloat
    }
  }

  def onKeyDown(keycode: Int): Unit = {
    if (!initiated) return
    keycode match {
      case Keys.W => forward = true
      case Keys.S => backward = true
      case Keys.A => left = true
      case Keys.D => right = true
      case Keys.SHIFT_LEFT => drift = true
      case Keys.ENTER => player.startLap()
      case _ =>
    }
  }

  def onKeyUp(keycode: Int): Unit = {
    if (!initiated) return
    keycode match {
      case Keys.W => forward = false
      case Keys.S => backward = false
      case Keys.A => left = false
      case Keys.D => right = false
      case Keys.SHIFT_LEFT => drift = false
      case _ =>
    }
  }

  def initFonts(): Unit = {
    val consola = Gdx.files.internal("assets/fonts/consola.ttf")
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

  def displayDebug(g: GdxGraphics, segmentInfo: (Int, Float, Float, Float)): Unit = {
    g.drawString(10, 20, "Laps: " + player.lapsCompleted)
    g.drawString(10, 40, "Segment: " + segmentInfo._1)
    g.drawString(10, 60, "SegDist: " + segmentInfo._2)
    g.drawString(10, 80, "TotDist: " + segmentInfo._3)
    g.drawString(10, 100, "DistPer: " + segmentInfo._4)

    g.drawString(10, 220, "Speed: " + kart.speed.toString)
    g.drawString(10, 240, "X: " + kart.x.toString)
    g.drawString(10, 260, "Y: " + kart.y.toString)
    g.drawString(10, 280, "Angle: " + kart.angle.toString)

    g.drawString(10, 320, "X: " + camera.x.toString)
    g.drawString(10, 340, "Y: " + camera.y.toString)
    g.drawString(10, 360, "Angle: " + camera.angle.toString)

    g.drawString(10, 400, "Best Lap: " + player.bestLap)
    g.drawString(10, 420, "Last Lap: " + player.lastLap)
  }

  /**
   * Disposes resources on application exit.
   */
  def dispose(): Unit = {
    mode7Renderer.dispose()
    kart.dispose()
    track.dispose()
    initiated = false
  }
}
