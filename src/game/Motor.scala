package game

import ch.hevs.gdx2d.desktop.PortableApplication
import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.{Color, Pixmap, Texture}
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.ScreenViewport

class Motor extends PortableApplication(1920, 1080, false) {
  /**
   * The main Scene2D stage for rendering UI elements.
   */
  private var stage: Stage = _

  /**
   * The skin used to style the UI components
   */
  private var skin: Skin = _

  /**
   * Pixmap of the terrain to render in mode7
   */
  private var terrainPixmap: Pixmap = _

  override def onInit(): Unit = {
    stage = new Stage(new ScreenViewport())

    Gdx.graphics.setResizable(false)

    Gdx.input.setInputProcessor(stage)

    skin = new Skin(Gdx.files.internal("assets/ui/uiskin.json"))

    terrainPixmap = new Pixmap(Gdx.files.internal("assets/game/map/circuit/example_map.png"))

    stage.clear()
  }

  override def onGraphicRender(g: GdxGraphics): Unit = {
    g.clear()

    val screenWidth = Gdx.graphics.getWidth
    val screenHeight = Gdx.graphics.getHeight

    // Simulates the camera height above the ground — larger means seeing further
    val scale = 20000f

    // Field of view — controls how wide the perspective fans out
    val fov = 1.2f

    // Camera position on the terrain map
    val cameraX = 384f
    val cameraY = 384f

    // Camera direction in radians — 0 means facing right (+X axis)
    val angle = math.Pi / 2
    val cos = math.cos(angle).toFloat
    val sin = math.sin(angle).toFloat

    // Vertical position of the horizon on screen (in pixels)
    val horizon = screenHeight / 6

    // Fills the top of the screen with sky color (from top to horizon)
    g.setColor(Color.SKY)
    g.drawFilledRectangle(screenWidth / 2, screenHeight - (horizon / 2), screenWidth, horizon, 0)

    // Render the ground from bottom of screen to the horizon line
    for (y <- screenHeight - 1 to horizon by -2) {
      // Row index relative to bottom of screen — used to simulate distance
      val row = screenHeight - y

      // Perspective trick: as row increases, we’re looking further away
      val rowDistance = scale / row

      // Forward vector for the current row
      val dx = cos * rowDistance
      val dy = sin * rowDistance

      for (x <- 0 until screenWidth by 2) {
        // Maps screen X (left to right) into a range of [-fov, +fov]
        val lateralOffset = (x - screenWidth / 2).toFloat / (screenWidth / 2) * fov

        // Project screen pixel into world space:
        // - Move forward along viewing angle (dx/dy)
        // - Move sideways based on FOV and row distance
        val sampleX = cameraX + dx - sin * lateralOffset * rowDistance
        val sampleY = cameraY + dy + cos * lateralOffset * rowDistance

        // Wrap map coordinates to stay inside image bounds
        val wrappedX = ((sampleX % terrainPixmap.getWidth + terrainPixmap.getWidth) % terrainPixmap.getWidth).toInt
        val wrappedY = ((sampleY % terrainPixmap.getHeight + terrainPixmap.getHeight) % terrainPixmap.getHeight).toInt

        // Fetch color from the map image
        val color = new Color(terrainPixmap.getPixel(wrappedX, wrappedY))

        // Draw pixel-sized rectangle at screen position
        g.setColor(color)
        g.drawRectangle(x, y - horizon, 1, 2, 0)
      }
    }

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
  }
}
