package menu

sealed trait ScreenState

case object MainMenu extends ScreenState

case object Settings extends ScreenState

case object Lobby extends ScreenState

case object Game extends ScreenState