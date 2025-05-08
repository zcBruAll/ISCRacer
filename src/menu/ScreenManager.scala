package menu

import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import menu.screens._

class ScreenManager(stage: Stage, skin: Skin) {
  private var currentScreen: Screen = _

  private var currentState: ScreenState = _

  def switchTo(state: ScreenState): Unit = {
    if (currentScreen != null)
      currentScreen.dispose()

    currentScreen = state match {
      case MainMenu => new MainMenuScreen()
      case Settings => new SettingsScreen()
      case Game => new GameScreen()
    }

    currentState = state
    currentScreen.init(stage, skin)
  }

  def update(g: GdxGraphics): Unit = {
    if (currentScreen != null)
      currentScreen.update(g)
  }
}
