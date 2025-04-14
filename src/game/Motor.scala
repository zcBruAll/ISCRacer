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

  // Camera position on the terrain map
  private var camX = 3386f
  private var camY = 1467f

  // Simulates the camera height above the ground — larger means seeing further
  var scale = 3f

  // Field of view — controls how wide the perspective fans out
  val fov = 1.2f

  // Camera direction in radians — 0 means facing right (+X axis)
  var angle: Float = math.Pi.toFloat / 2f

  // Vertical position of the horizon on screen (in pixels)
  val horizon: Int = height / 3

  private var mode7Renderer: Mode7Renderer = _
  private var mapTexture: Texture = _

  private var kart: Kart = _
  private var forward: Boolean = false
  private var backward: Boolean = false
  private var left: Boolean = false
  private var right: Boolean = false
  private var drift: Boolean = false

  override def onInit(): Unit = {
    stage = new Stage(new ScreenViewport())

    Gdx.graphics.setResizable(false)

    skin = new Skin(Gdx.files.internal("assets/ui/uiskin.json"))

    // terrainPixmap = new Pixmap(Gdx.files.internal("assets/game/map/circuit/example_map.png"))
    mapTexture = new Texture(Gdx.files.internal("assets/game/map/tracks/basic_track.png"))
    mode7Renderer = new Mode7Renderer(mapTexture)

    kart = new Kart(gd.getDisplayMode.getRefreshRate)

    stage.clear()
  }

  override def onGraphicRender(g: GdxGraphics): Unit = {
    g.clear()

    // Fills the top of the screen with sky color (from top to horizon)
    g.setColor(Color.SKY)
    g.drawFilledRectangle(width / 2, height - (horizon / 2), width, horizon, 0)

    if (forward || backward)
      kart.accelerate(forward)

    if (drift && (left || right))
      kart.drift(right)
    else if (left || right)
      kart.rotate(right)

    kart.move()

    mode7Renderer.render(kart.x, kart.y, kart.angle, scale, fov, horizon)

    g.drawString(10, 100, "Speed: " + kart.speed.toString)
    g.drawString(10, 120, "X: " + kart.x.toString)
    g.drawString(10, 140, "Y: " + kart.y.toString)
    g.drawString(10, 160, "Angle: " + kart.angle.toString)
    g.drawFPS()

    // Update and render UI
    stage.act()
    stage.draw()
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
    mapTexture.dispose()
  }
}
