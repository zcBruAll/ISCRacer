package game

import utils.Conversion

class Player(track: Track, checkpointSkipLimit: Int = 2) {
  private var lastCheckpointIndex: Int = 0
  var lapsCompleted: Int = 0
  private var _lapTimes: LapTimes = LapTimes()
  private var _startOfLapTime: Long = 0L
  private var _startOfRun: Long = 0L

  def update(currentSegment: Int, kart: Kart): Unit = {
    if (lastCheckpointIndex + checkpointSkipLimit >= currentSegment) {
      if (lastCheckpointIndex != currentSegment && lastCheckpointIndex >= track.checkpoints.length - checkpointSkipLimit) {
        lapsCompleted += 1
        endLap()
        startLap()
      }

      lastCheckpointIndex = currentSegment
    } else {
      resetToLastCP(kart)
    }
  }

  private def resetToLastCP(kart: Kart): Unit = {
    val cp = track.checkpoints(lastCheckpointIndex)
    val nextCP = track.checkpoints((lastCheckpointIndex + 1) % (track.checkpoints.length - 1))
    kart.x = cp.x
    kart.y = cp.y
    kart.angle = math.tan((nextCP.y / cp.y) / (nextCP.x / cp.x)).toFloat
    kart.speed = 0f
  }

  def bestLap: String = {
    val ms = if (_lapTimes.best.isDefined) _lapTimes.best.get else 0
    Conversion.longToTimeString(ms)
  }

  def lastLap: String = {
    val ms = if (_lapTimes.currentLap.isDefined) _lapTimes.currentLap.get else 0
    Conversion.longToTimeString(ms)
  }

  def totalTime: String = {
    if (_startOfRun == 0) return ""
    val ms = System.currentTimeMillis() - _startOfRun
    Conversion.longToTimeString(ms)
  }

  def lapTime: String = {
    if (_startOfLapTime == 0) return ""
    val ms = System.currentTimeMillis() - _startOfLapTime
    Conversion.longToTimeString(ms)
  }

  def endLap(): Unit = {
    _lapTimes = _lapTimes.addLap(System.currentTimeMillis() - _startOfLapTime)
  }

  def startLap(): Unit = {
    _startOfLapTime = System.currentTimeMillis()
    if (_lapTimes.laps.isEmpty)
      _startOfRun = _startOfLapTime
  }
}

