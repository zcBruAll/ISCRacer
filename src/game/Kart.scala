package game

class Kart(fps: Int) {
  private var _speed: Float = 0
  private val maxSpeed: Float = 2f
  private val minSpeed: Float = -.5f

  private var _x: Float = 3386f
  private var _y: Float = 1467f
  private var _angle: Float = math.Pi.toFloat / 2

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

  def speed: Float = _speed

  def speed_=(value: Float): Unit = {
    _speed = math.max(minSpeed, math.min(value, maxSpeed))
  }

  def move(): Unit = {
    x += math.cos(angle).toFloat * speed
    y += math.sin(angle).toFloat * speed
  }

  def accelerate(forward: Boolean = true): Unit = {
    speed += (if (forward) 1 else -1) * .2f / fps
  }

  def rotate(right: Boolean): Unit = {
    angle += (if (right) 1 else -1) * .2f / fps
  }

  def drift (right: Boolean): Unit = {
    angle += (if (right) 1 else -1) * .5f / fps
  }
}
