package menu

sealed trait ScreenState

case object MainMenu extends ScreenState

case object Settings extends ScreenState

case object Game extends ScreenState