import ch.hevs.gdx2d.desktop.PortableApplication
import menu.Menu

object Main extends App {
  PortableApplication.CreateLwjglApplication = false
  Menu.createMenu()
}
