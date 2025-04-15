package game

class Kart(fps: Int) {
  private var _speed: Float = 0
  private val maxSpeed: Float = 2.5f
  private val minSpeed: Float = -.5f

  private val accelerationRate: Float = 1.5f
  private val friction: Float = .5f
  private val brakeForce: Float = 2.5f

  private var _x: Float = 3386f
  private var _y: Float = 1467f
  private var _angle: Float = math.Pi.toFloat / 2

  private var _movingAngle: Float = angle
  private val traction: Float = .1f       // 0 = very slippery, 1 = sharp control

  def x: Float = _x

  def x_= (value: Float): Unit = {
    _x = value % 4096
  }

  def y: Float = _y

  def y_= (value: Float): Unit = {
    _y = value % 4096
  }

  def angle: Float = _angle

  def angle_=(value: Float): Unit = {
    _angle = value
  }

  def speed: Float = _speed

  def speed_=(value: Float): Unit = {
    _speed = math.max(minSpeed, math.min(value, maxSpeed))
  }

  def movingAngle: Float = _movingAngle

  def movingAngle_=(value: Float): Unit = {
    _movingAngle = value
  }

  def normalizeAngle(angle: Float): Float = {
    var a = angle
    while (a > math.Pi) a -= (2 * math.Pi).toFloat
    while (a < -math.Pi) a += (2 * math.Pi).toFloat
    a
  }

  def move(): Unit = {
    val angleDiff = normalizeAngle(angle - movingAngle)
    movingAngle += angleDiff * traction
    x += math.cos(movingAngle).toFloat * speed
    y += math.sin(movingAngle).toFloat * speed
  }

  def accelerate(forward: Boolean = true): Unit = {
    val accel = (if (forward) 1 else -1) * accelerationRate * (1f / fps)
    speed += accel
  }

  def applyFriction(): Unit = {
    if (speed > 0) {
      speed = math.max(0, speed - friction * (1f / fps))
    } else if (speed < 0) {
      speed = math.min(0, speed + friction * (1f / fps))
    }
  }

  def brake(): Unit = {
    if (speed > 0) {
      speed = math.max(0, speed - brakeForce * (1f / fps))
    } else if (speed < 0) {
      speed = math.min(0, speed + brakeForce * (1f / fps))
    }
  }

  def rotate(right: Boolean): Unit = {
    angle += (if (right) 1 else -1) * .4f / 180
  }

  def drift (right: Boolean): Unit = {
    angle += (if (right) 1 else -1) * .8f / 180
    speed *= .99f
  }
}
