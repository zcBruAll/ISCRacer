package game

class PlayerCheckpointTracker(track: Track, checkpointSkipLimit: Int = 2) {
  private var lastCheckpointIndex: Int = 0
  var lapsCompleted: Int = 0

  def update(currentSegment: Int, kart: Kart): Unit = {
    if (lastCheckpointIndex + checkpointSkipLimit >= currentSegment) {
      if (lastCheckpointIndex != currentSegment && lastCheckpointIndex >= track.checkpoints.length - checkpointSkipLimit)
        lapsCompleted += 1

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
}

