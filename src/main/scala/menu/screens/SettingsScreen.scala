package menu.screens

import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.scenes.scene2d.{Actor, InputEvent, Stage}
import com.badlogic.gdx.scenes.scene2d.ui.{Skin, TextButton}
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import menu.{MainMenu, Menu}

class SettingsScreen extends Screen {
  private var btnBack: TextButton = _

  /**
   * Initializes the screen and adds its UI elements to the provided stage.
   *
   * @param stage The Stage used to render UI actors.
   * @param skin  The Skin used to style UI widgets.
   */
  override def init(stage: Stage, skin: Skin): Unit = {
    super.init(stage, skin)

    btnBack = new TextButton("Back", skin)
    btnBack.setSize(200, 60)
    btnBack.setPosition(20, stage.getHeight - (20 + btnBack.getHeight))

    btnBack.addListener(new ClickListener {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
        Menu.screenManager.switchTo(MainMenu)
      }
    })

    addActor(btnBack)
  }

  /**
   * Updates the screen's logic.
   * Called once per frame in the main render loop.
   */
  override def update(g: GdxGraphics): Unit = {}
}
