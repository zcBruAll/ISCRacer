package menu.screens

import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import game.Motor
import menu.{Lobby, Menu}

class GameScreen extends Screen {
  /**
   * Initializes the screen and adds its UI elements to the provided stage.
   *
   * @param stage The Stage used to render UI actors.
   * @param skin  The Skin used to style UI widgets.
   */
  override def init(stage: Stage, skin: Skin): Unit = {
    super.init(stage, skin)

    val settings = Motor.gameSettings
    Motor.init(settings._1, settings._2, settings._3, settings._4)
    Motor.endGame = false
  }

  /**
   * Updates the screen's logic.
   * Called once per frame in the main render loop.
   */
  override def update(g: GdxGraphics): Unit = {
    if (Motor.endGame) {
      Menu.screenManager.switchTo(Lobby)
    } else Motor.render(g)
  }

  /**
   * Disposes of the screen by clearing or removing any added actors or resources.
   * Called when switching to a different screen.
   */
  override def dispose(): Unit = {
    super.dispose()

    Motor.dispose()
  }
}
