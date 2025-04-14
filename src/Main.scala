import ch.hevs.gdx2d.desktop.PortableApplication
import game.Motor

object Main extends App {
  PortableApplication.CreateLwjglApplication = false
  new Motor(1920, 1080, true)
}
