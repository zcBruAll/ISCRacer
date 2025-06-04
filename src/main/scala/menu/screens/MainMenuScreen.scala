package menu.screens

import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.{Skin, TextButton}
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.{Actor, InputEvent, Stage}
import menu.{Game, Lobby, MainMenu, Menu, Settings}

class MainMenuScreen extends Screen {

  private var stage: Stage = _

  private var btnPlay: TextButton = _

  private var btnSettings: TextButton = _

  private var addedActors = List[Actor]()

  /**
   * Initializes the screen and adds its UI elements to the provided stage.
   *
   * @param stage The Stage used to render UI actors.
   * @param skin  The Skin used to style UI widgets.
   */
  override def init(stage: Stage, skin: Skin): Unit = {
    this.stage = stage

    btnPlay = new TextButton("Play", skin)
    btnSettings = new TextButton("Settings", skin)

    // Play button
    btnPlay.setSize(200, 60)
    btnPlay.setPosition(centerX(btnPlay), centerY(btnPlay) + 40)

    btnPlay.addListener(new ClickListener {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
        Menu.screenManager.switchTo(Lobby)
      }
    })

    // Settings button
    btnSettings.setSize(200, 60)
    btnSettings.setPosition(centerX(btnSettings), centerY(btnSettings) - 40)

    btnSettings.addListener(new ClickListener {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
        Menu.screenManager.switchTo(Settings)
      }
    })


    stage.addActor(btnPlay)
    stage.addActor(btnSettings)

    addedActors ::= btnPlay
    addedActors ::= btnSettings
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
