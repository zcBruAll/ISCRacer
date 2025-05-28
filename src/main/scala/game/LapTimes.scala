package game

case class LapTimes(laps: Vector[Long] = Vector.empty,
                    bestTime: Option[Long] = None) {
  def addLap(timeMs: Long): LapTimes = {
    val newBest = bestTime match {
      case Some(bt) if bt < timeMs => bt
      case _ => timeMs
    }
    copy(laps = laps :+ timeMs, bestTime = Some(newBest))
  }

  def best: Option[Long] = bestTime

  def currentLap: Option[Long] = laps.lastOption

  def allLapTimes: Vector[Long] = laps
}

