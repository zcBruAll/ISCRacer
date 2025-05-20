package menu.screens

import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import game.Motor
import menu.Menu

class GameScreen extends Screen {
  private var stage: Stage = _

  /**
   * Initializes the screen and adds its UI elements to the provided stage.
   *
   * @param stage The Stage used to render UI actors.
   * @param skin  The Skin used to style UI widgets.
   */
  override def init(stage: Stage, skin: Skin): Unit = {
    this.stage = stage

    Motor.init("map_1")
  }

  /**
   * Updates the screen's logic.
   * Called once per frame in the main render loop.
   */
  override def update(g: GdxGraphics): Unit = {
    Motor.render(g)
  }

  /**
   * Disposes of the screen by clearing or removing any added actors or resources.
   * Called when switching to a different screen.
   */
  override def dispose(): Unit = {
    stage.clear()

    Motor.dispose()
  }
}
