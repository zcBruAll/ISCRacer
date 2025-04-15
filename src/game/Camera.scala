package game

class Camera(fps: Int) {
  private var _x: Float = 0f
  private var _y: Float = 0f
  private var _angle: Float = 0f

  private var _fov: Float = 1.2f
  private var _scale: Float = 1.5f

  private var _offsetX: Float = 0f
  private var _offsetY: Float = 0f
  private var _targetOffsetX: Float = 0f
  private var _targetOffsetY: Float = 0f

  private var _baseFov: Float = 1.2f
  private var _targetFov: Float = 1.2f

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

  def offsetX: Float = _offsetX

  def offsetX_=(value: Float): Unit = {
    _offsetX = value
  }

  def offsetY: Float = _offsetY

  def offsetY_=(value: Float): Unit = {
    _offsetY = value
  }

  def targetOffsetX: Float = _targetOffsetX

  def targetOffsetX_=(value: Float) {
    _targetOffsetX = value
  }

  def targetOffsetY: Float = _targetOffsetY

  def targetOffsetY_=(value: Float) {
    _targetOffsetY = value
  }

  def baseFov: Float = _baseFov

  def baseFov_=(value: Float): Unit = {
    _baseFov = value
  }

  def targetFov: Float = _targetFov

  def targetFov_=(value: Float): Unit = {
    _targetFov = value
  }

  def update(kart: Kart): Unit = {
    x = kart.x
    y = kart.y
    angle = kart.angle

    val baseDistance = 20f
    val speedFactor = kart.speed / kart.maxSpeed

    targetOffsetX = math.cos(angle).toFloat * -baseDistance * speedFactor
    targetOffsetY = math.sin(angle).toFloat * -baseDistance * speedFactor

    val lerp = .1f
    offsetX = (targetOffsetX - offsetX) * lerp
    offsetY = (targetOffsetY - offsetY) * lerp

    val fovBoost = .6f
    targetFov = baseFov + math.abs(speedFactor) * fovBoost
    fov += (targetFov - fov) * .01f
  }
}
