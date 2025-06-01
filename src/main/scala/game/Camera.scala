package game

class Camera {
  private var _x: Float = 0f
  private var _y: Float = 0f
  private var _angle: Float = 0f

  private var _fov: Float = 1.2f
  private var _scale: Float = 1.5f

  def x: Float = _x

  def x_=(value: Float): Unit = {
    _x = value
  }

  def y: Float = _y

  def y_=(value: Float): Unit = {
    _y = value
  }

  def angle: Float = _angle

  def angle_=(value: Float): Unit = {
    _angle = value
  }

  def fov: Float = _fov

  def fov_=(value: Float): Unit = {
    _fov = value
  }

  def scale: Float = _scale

  def scale_=(value: Float): Unit = {
    _scale = value
  }

  def update(kart: Kart): Unit = {
    x = kart.x
    y = kart.y
    angle = kart.angle
  }
}
