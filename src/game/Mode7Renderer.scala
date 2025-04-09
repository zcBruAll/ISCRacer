package game

import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.Gdx

class Mode7Renderer(texture: Texture) {
  private val vertexShader: String =
    """
      |attribute vec4 a_position;
      |attribute vec2 a_texCoord0;
      |varying vec2 v_texCoords;
      |void main() {
      |  v_texCoords = a_texCoord0;
      |  gl_Position = a_position;
      |}
      |""".stripMargin

  private val fragmentShader: String = Gdx.files.internal("assets/shaders/mode7.frag").readString()

  private val shader = new ShaderProgram(vertexShader, fragmentShader)
  if (!shader.isCompiled) throw new Exception("Shader compile error: " + shader.getLog)

  private val batch = new SpriteBatch(1, shader)

  def render(cameraX: Float, cameraY: Float, angle: Float, scale: Float, fov: Float, horizon: Float): Unit = {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    val w = Gdx.graphics.getWidth
    val h = Gdx.graphics.getHeight

    texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)

    shader.begin()
    shader.setUniformf("u_texture", 0)
    shader.setUniformf("u_cameraX", cameraX / texture.getWidth)
    shader.setUniformf("u_cameraY", cameraY / texture.getHeight)
    shader.setUniformf("u_angle", angle)
    shader.setUniformf("u_scale", scale)
    shader.setUniformf("u_fov", fov)
    shader.setUniformf("u_screenWidth", w.toFloat)
    shader.setUniformf("u_screenHeight", h.toFloat - horizon)
    shader.setUniformf("u_horizon", horizon)
    shader.end()

    batch.begin()
    batch.draw(texture, -(w/2), -(h/2), w, h)
    batch.end()
  }

  def dispose(): Unit = {
    shader.dispose()
    batch.dispose()
  }
}
