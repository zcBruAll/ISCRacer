package menu.screens

import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.scenes.scene2d.{Actor, InputEvent, Stage}
import com.badlogic.gdx.scenes.scene2d.ui.{Skin, TextButton}
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import menu.{MainMenu, Menu}

class SettingsScreen extends Screen {
  private var stage: Stage = _

  private var btnBack: TextButton = _

  private var addedActors: List[Actor] = List[Actor]()

  /**
   * Initializes the screen and adds its UI elements to the provided stage.
   *
   * @param stage The Stage used to render UI actors.
   * @param skin  The Skin used to style UI widgets.
   */
  override def init(stage: Stage, skin: Skin): Unit = {
    this.stage = stage

    btnBack = new TextButton("Back", skin)
    btnBack.setSize(200, 60)
    btnBack.setPosition(centerX(btnBack), centerY(btnBack))

    btnBack.addListener(new ClickListener {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
        Menu.screenManager.switchTo(MainMenu)
      }
    })

    this.stage.addActor(btnBack)
    addedActors ::= btnBack
  }

  /**
   * Updates the screen's logic.
   * Called once per frame in the main render loop.
   */
  override def update(g: GdxGraphics): Unit = {}

  /**
   * Disposes of the screen by clearing or removing any added actors or resources.
   * Called when switching to a different screen.
   */
  override def dispose(): Unit = {
    stage.clear()
  }
}
