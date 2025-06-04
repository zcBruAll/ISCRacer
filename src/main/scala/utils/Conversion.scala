package utils

object Conversion {
  def longToTimeString(ms: Long): String = {
    val hours   = ms / 3_600_000
    val minutes = (ms % 3_600_000) / 60_000
    val seconds = (ms % 60_000) / 1_000
    val millis  = ms % 1_000

    if (hours > 0)
      f"$hours%d:$minutes%02d:$seconds%02d.$millis%03d"
    else if (minutes > 0)
      f"$minutes%02d:$seconds%02d.$millis%03d"
    else
      f"$seconds%d.$millis%03d"
  }

  def formatTime(ms: Long): String = {
    val minutes = ms / 60000
    val seconds = (ms % 60000) / 1000
    val millis  = ms % 1000
    f"$minutes%02d:$seconds%02d.${millis}%03d"
  }
}
