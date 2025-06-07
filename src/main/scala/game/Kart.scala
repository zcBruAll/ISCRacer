package game

import ch.hevs.gdx2d.components.bitmaps.BitmapImage

class Kart {
  private var _speedX: Float = 0
  private var _speedY: Float = 0

  private var _x: Float = 0
  private var _y: Float = 0
  private var _angle: Float = math.Pi.toFloat / 2

  val texture: BitmapImage = new BitmapImage("src/main/assets/game/karts/example_kart/example_kart.png")

  def x: Float = _x

  def x_= (value: Float): Unit = {
    _x = value
  }

  def y: Float = _y

  def y_= (value: Float): Unit = {
    _y = value
  }

  def angle: Float = _angle

  def angle_=(value: Float): Unit = {
    _angle = value
  }

  def speedX: Float = _speedX

  def speedX_=(value: Float): Unit = {
    _speedX = value
  }

  def speedY: Float = _speedY

  def speedY_=(value: Float): Unit = {
    _speedY = value
  }

  def dispose(): Unit = {
    texture.dispose()
  }
}
