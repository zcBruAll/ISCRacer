package game

import ch.hevs.gdx2d.components.bitmaps.BitmapImage
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2

import scala.io.Source

class Track(val name: String) {
  private val baseTrackDir: String = "src/main/assets/game/map/tracks/" + name + "/"
  val checkpoints: List[Checkpoint] = loadCheckpoints(baseTrackDir + "points.json")

  val mapTexture = new Texture(Gdx.files.internal(baseTrackDir + "texture.png"))
  val minimapTexture = new BitmapImage(baseTrackDir + "minimap.png")

  val segmentLengths: Array[Float] = checkpoints
      .sliding(2)
      .map { case List(a, b) =>
        val dx = b.x - a.x
        val dy = b.y - a.y
        math.sqrt(dx * dx + dy * dy).toFloat
      }.toArray

  val cumulativeDistances: Array[Float] = segmentLengths.scanLeft(0f)(_ + _)

  def closestSegmentAndProgress(px: Float, py: Float): (Int, Float, Float, Float) = {
    val p = new Vector2(px, py)
    var bestIndex = 0
    var bestT = 0f
    var minDist = Float.MaxValue

    for (i <- 0 until checkpoints.length - 1) {
      val a = new Vector2(checkpoints(i).x, checkpoints(i).y)
      val b = new Vector2(checkpoints(i + 1).x, checkpoints(i + 1).y)
      val ab = new Vector2(b).sub(a)
      val ap = new Vector2(p).sub(a)

      val abLenSq = ab.len2()
      val t = ap.dot(ab) / abLenSq

      val clampedT = t.max(0f).min(1f)
      val projection = new Vector2(a).add(ab.scl(clampedT))
      val dist = p.dst2(projection)

      if (dist < minDist) {
        minDist = dist
        bestIndex = i
        bestT = clampedT
      }
    }

    val distanceSoFar = cumulativeDistances(bestIndex) + segmentLengths(bestIndex) * bestT
    val progressRatio = distanceSoFar / cumulativeDistances.last

    (bestIndex, bestT, distanceSoFar, progressRatio)
  }

  private def loadCheckpoints(filepath: String): List[Checkpoint] = {
    val source = Source.fromFile(filepath)
    val content = try source.mkString finally source.close()

    val pointsPattern = "\\{\\s*\"x\"\\s*:\\s*(\\d+(\\.\\d+)?),\\s*\"y\"\\s*:\\s*(\\d+(\\.\\d+)?)\\s*}".r

    val points = pointsPattern.findAllIn(content).matchData.map { m =>
      val x = m.group(1).toFloat
      val y = m.group(3).toFloat
      Checkpoint(x, y)
    }.toList

    if (points.nonEmpty) points :+ points.head else points
  }

  def dispose(): Unit = {
    mapTexture.dispose()
    minimapTexture.dispose()
  }
}
