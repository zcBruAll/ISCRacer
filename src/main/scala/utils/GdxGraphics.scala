package utils

import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.{Color, OrthographicCamera}
import com.badlogic.gdx.graphics.g2d.{BitmapFont, SpriteBatch}

object GraphicsUtils {
  def drawFPS(g: GdxGraphics, textColor: Color, x: Float = 5f, y: Float = 15f): Unit = {
    val fieldFont = classOf[GdxGraphics].getDeclaredField("font")
    fieldFont.setAccessible(true)
    val font = fieldFont.get(g).asInstanceOf[BitmapFont]

    val fieldSB = classOf[GdxGraphics].getDeclaredField("spriteBatch")
    fieldSB.setAccessible(true)
    val spriteBatch = fieldSB.get(g).asInstanceOf[SpriteBatch]

    val fieldFixedCam = classOf[GdxGraphics].getDeclaredField("fixedCamera")
    fieldFixedCam.setAccessible(true)
    val fixedCamera = fieldFixedCam.get(g).asInstanceOf[OrthographicCamera]

    val fieldCam = classOf[GdxGraphics].getDeclaredField("camera")
    fieldCam.setAccessible(true)
    val camera = fieldCam.get(g).asInstanceOf[OrthographicCamera]

    spriteBatch.setProjectionMatrix(fixedCamera.combined)

    val oldColor = font.getColor
    font.setColor(textColor)
    font.draw(spriteBatch, s"FPS: ${Gdx.graphics.getFramesPerSecond}", x, y)
    font.setColor(oldColor)

    spriteBatch.setProjectionMatrix(camera.combined)
  }
}
