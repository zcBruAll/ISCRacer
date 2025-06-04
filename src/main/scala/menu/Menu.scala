package menu

import ch.hevs.gdx2d.desktop.{Game2D, GdxConfig, PortableApplication}
import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.{Gdx, InputMultiplexer, InputProcessor}
import com.badlogic.gdx.backends.lwjgl.{LwjglApplication, LwjglApplicationConfiguration}
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.ScreenViewport
import game.Motor

import java.awt.{GraphicsDevice, GraphicsEnvironment}

class Menu private(var width: Int, var height: Int, fullScreen: Boolean) extends PortableApplication(width, height, fullScreen) {

  // Graphic device used by the application
  val gd: GraphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice

  if (fullScreen) {
    width = gd.getDisplayMode.getWidth
    height = gd.getDisplayMode.getHeight
  }

  // Lwjgl application configuration
  private var _lwjglApp: LwjglApplication = _
  def lwjglApp: LwjglApplication = _lwjglApp

  private val config: LwjglApplicationConfiguration = GdxConfig.getLwjglConfig(width, height, fullScreen)
  createLwjglApplication()

  private var stage: Stage = _

  private var skin: Skin = _

  private var screenManager: ScreenManager = _

  private var consolasFontStyle: BitmapFont = _

  def consolasFont: BitmapFont = consolasFontStyle

  def initConsolasFont(): Unit = {
    val fontFile = Gdx.files.internal("src/main/assets/fonts/consola.ttf")
    val gen = new FreeTypeFontGenerator(fontFile)
    val param = new FreeTypeFontGenerator.FreeTypeFontParameter
    param.size = 18
    param.spaceX = 0
    param.spaceY = 0
    param.characters = FreeTypeFontGenerator.DEFAULT_CHARS
    consolasFontStyle = gen.generateFont(param)
    gen.dispose()
  }

  override def onInit(): Unit = {
    stage = new Stage(new ScreenViewport())

    val multiplexer = new InputMultiplexer(stage, _lwjglApp.getInput.getInputProcessor)
    Gdx.input.setInputProcessor(multiplexer)

    skin = new Skin(Gdx.files.internal("src/main/assets/ui/uiskin.json"))

    initConsolasFont()

    screenManager = new ScreenManager(stage, skin)
    screenManager.switchTo(MainMenu)
  }

  override def onGraphicRender(g: GdxGraphics): Unit = {
    g.clear()
    screenManager.update(g)
    stage.act()
    stage.draw()
  }

  override def onKeyDown(keycode: Int): Unit = {
    super.onKeyDown(keycode)

    Motor.onKeyDown(keycode)
  }

  override def onKeyUp(keycode: Int): Unit = {
    super.onKeyUp(keycode)

    Motor.onKeyUp(keycode)
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
    config.fullscreen = fullScreen

    val theGame = new Game2D(this)
    _lwjglApp = new LwjglApplication(theGame, config)
  }

  override def onDispose(): Unit = {
    stage.dispose()
    skin.dispose()
  }
}

object Menu {
  private var _menu: Menu = _

  def menu: Menu = _menu

  def createMenu(width: Int = 1920, height: Int = 1080, fullScreen: Boolean = true): Menu = {
    _menu = new Menu(width, height, fullScreen)
    _menu
  }

  def screenManager: ScreenManager = {
    if (_menu != null)
      _menu.screenManager
    else {
      _menu = createMenu()
      _menu.screenManager
    }
  }
}
