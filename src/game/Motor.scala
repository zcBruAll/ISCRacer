package game

import ch.hevs.gdx2d.desktop.{Game2D, GdxConfig, PortableApplication}
import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.backends.lwjgl.{LwjglApplication, LwjglApplicationConfiguration}
import com.badlogic.gdx.graphics.{Color, Texture}
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.ScreenViewport
import utils.GraphicsUtils

import java.awt.{GraphicsDevice, GraphicsEnvironment}

class Motor(var width: Int, var height: Int, fullScreen: Boolean) extends PortableApplication(width, height, fullScreen) {

  // Graphic device used by the application
  private val gd: GraphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice

  if (fullScreen) {
    width = gd.getDisplayMode.getWidth
    height = gd.getDisplayMode.getHeight
  }

  // Lwjgl application configuration
  private val config: LwjglApplicationConfiguration = GdxConfig.getLwjglConfig(width, height, fullScreen)
  createLwjglApplication()

  // The main Scene2D stage for rendering UI elements.
  private var stage: Stage = _

  // The skin used to style the UI components
  private var skin: Skin = _

  // Simulates the camera height above the ground — larger means seeing further
  var scale = 3f

  // Field of view — controls how wide the perspective fans out
  var fov = 1.2f

  // Camera direction in radians — 0 means facing right (+X axis)
  var angle: Float = math.Pi.toFloat / 2f

  // Vertical position of the horizon on screen (in pixels)
  val horizon: Int = height / 3

  private var mode7Renderer: Mode7Renderer = _

  private var kart: Kart = _
  private var camera: Camera = _
  private var track: Track = _
  private var playerCheckpointTracker: PlayerCheckpointTracker = _

  private var forward: Boolean = false
  private var backward: Boolean = false
  private var left: Boolean = false
  private var right: Boolean = false
  private var drift: Boolean = false

  override def onInit(): Unit = {
    stage = new Stage(new ScreenViewport())

    Gdx.graphics.setResizable(false)

    skin = new Skin(Gdx.files.internal("assets/ui/uiskin.json"))

    track = new Track("map_1")
    initKart()
    camera = new Camera()
    playerCheckpointTracker = new PlayerCheckpointTracker(track)

    mode7Renderer = new Mode7Renderer(track.mapTexture)

    stage.clear()
  }

  override def onGraphicRender(g: GdxGraphics): Unit = {
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

    playerCheckpointTracker.update(segmentInfo._1, kart)

    camera.update(kart)

    mode7Renderer.render(camera.x + camera.offsetX, camera.y + camera.offsetY, camera.angle, camera.scale, camera.fov, horizon)

    g.drawTransformedPicture(width / 2, height / 4, 0, 3, kart.texture)

    g.drawString(10, 20, "Laps: " + playerCheckpointTracker.lapsCompleted)
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

    GraphicsUtils.drawFPS(g, Color.WHITE, 5f, height - 10)

    // Update and render UI
    stage.act()
    stage.draw()
  }

  private def initKart(): Unit = {
    kart = new Kart()
    kart.fps = gd.getDisplayMode.getRefreshRate
    if (track.checkpoints.nonEmpty) {
      val c0 = track.checkpoints.head
      val c1 = track.checkpoints(1)
      kart.x = c0.x
      kart.y = c0.y
      kart.angle = math.tan((c1.y - c0.y) / (c1.x - c0.x)).toFloat
    }
  }

  private def createLwjglApplication(): Unit = {
    Thread.currentThread.setPriority(10)

    config.foregroundFPS = gd.getDisplayMode.getRefreshRate
    config.backgroundFPS = 15
    config.vSyncEnabled = true
    config.samples = 4
    config.depth = 24
    config.stencil = 8
    config.useGL30 = true
    config.gles30ContextMajorVersion = 3
    config.gles30ContextMinorVersion = 2
    config.title = "ISCRacer"

    val theGame = new Game2D(this)
    new LwjglApplication(theGame, config)
  }

  override def onKeyDown(keycode: Int): Unit = {
    super.onKeyDown(keycode)

    keycode match {
      case Keys.W => forward = true
      case Keys.S => backward = true
      case Keys.A => left = true
      case Keys.D => right = true
      case Keys.SHIFT_LEFT => drift = true
      case _ =>
    }
  }

  override def onKeyUp(keycode: Int): Unit = {
    super.onKeyUp(keycode)

    keycode match {
      case Keys.W => forward = false
      case Keys.S => backward = false
      case Keys.A => left = false
      case Keys.D => right = false
      case Keys.SHIFT_LEFT => drift = false
      case _ =>
    }
  }

  /**
   * Disposes resources on application exit.
   */
  override def onDispose(): Unit = {
    super.onDispose()
    stage.dispose()
    skin.dispose()
    mode7Renderer.dispose()
    kart.dispose()
    track.dispose()
  }
}
