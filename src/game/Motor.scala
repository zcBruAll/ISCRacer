package game

import ch.hevs.gdx2d.components.bitmaps.BitmapImage
import ch.hevs.gdx2d.desktop.PortableApplication
import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.{Color, Pixmap, Texture}
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.ScreenViewport

class Motor extends PortableApplication(1920, 1080, true) {
  /**
   * The main Scene2D stage for rendering UI elements.
   */
  private var stage: Stage = _

  /**
   * The skin used to style the UI components
   */
  private var skin: Skin = _

  // Camera position on the terrain map
  private var camX = 384f
  private var camY = 384f

  // Simulates the camera height above the ground — larger means seeing further
  var scale = 50f

  // Field of view — controls how wide the perspective fans out
  val fov = 1.2f

  // Camera direction in radians — 0 means facing right (+X axis)
  var angle: Float = math.Pi.toFloat / 2f

  private var mode7Renderer: Mode7Renderer = _
  private var mapTexture: Texture = _

  override def onInit(): Unit = {
    stage = new Stage(new ScreenViewport())

    Gdx.graphics.setResizable(false)

    Gdx.input.setInputProcessor(stage)

    skin = new Skin(Gdx.files.internal("assets/ui/uiskin.json"))

    // terrainPixmap = new Pixmap(Gdx.files.internal("assets/game/map/circuit/example_map.png"))
    mapTexture = new Texture(Gdx.files.internal("assets/game/map/circuit/example_map.png"))
    mode7Renderer = new Mode7Renderer(mapTexture)

    stage.clear()
  }

  override def onGraphicRender(g: GdxGraphics): Unit = {
    g.clear()

    val screenWidth = Gdx.graphics.getWidth
    val screenHeight = Gdx.graphics.getHeight

    camX += .5f
    camY += .5f

    angle += .1f / 90

    // Vertical position of the horizon on screen (in pixels)
    val horizon = screenHeight / 3

    // Fills the top of the screen with sky color (from top to horizon)
    g.setColor(Color.SKY)
    g.drawFilledRectangle(screenWidth / 2, screenHeight - (horizon / 2), screenWidth, horizon, 0)

    mode7Renderer.render(camX, camY, angle, scale, fov, horizon)

    g.drawFPS()

    // Update and render UI
    stage.act()
    stage.draw()
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
