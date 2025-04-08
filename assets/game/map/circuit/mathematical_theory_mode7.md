# Goal: Given a screen pixel (x, y), figure out what point on the map/terrain it should represent.
When imagining that pixel lies on a 3D ground plane, we're asking:
> "If I'm standing at `cameraX, cameraY` and looking at `angle`, what point on the ground 
> would appear at screen pixel `x, y`?"
## Step-by-step breakdown
### Step 0: Understanding `angle`
The angle must be treated as a **rotation in 2D space**, just like in trigonometry.

#### Coordinate system (Mode7-style / math-style)
- X increases **to the right**
- Y increases **upward**

#### Angle meanings (in radians):
|   `angle`   | `cos(angle)` | `sin(angle)` |    facing    |
|:-----------:|:------------:|:------------:|:------------:|
|     $0$     |     $1$      |     $0$      | Right (east) |
|  $\pi / 2$  |     $0$      |     $1$      |  Up (north)  |
|    $\pi$    |     $-1$     |     $0$      | Left (west)  |
| $3 \pi / 2$ |     $0$      |     $-1$     | Down (south) |

`cos`/`sin` are used to get the **"look direction"** in the world

### Step 1: Simulate looking forward
```scala
val dx = math.cos(angle) * rowDistance
val dy = math.sin(angle) * rowDistance
```
This gives the forward direction, scaled by `rowDistance`, which simulates how far forward you're looking 
at that scanline (row).  
So `cameraX + dx`, `cameraY + dy` gives a point *straight ahead* from the camera at that depth.
---
### Step 2: Apply lateral shift (sideways offset)
Now we need to simulate **left/right** distortion - the fan-out of perspective across the screen.  
First:
```scala
val lateralOffset = (x - screenWidth / 2).toFloat / (screenWidth / 2)
```
This maps the pixel X from `[0 -> screenWidth]` into:
```css
[-1.0 <- center -> +1.0]
```
Example:
- Left edge of screen: `x = 0` -> `laterallOffset = -1`
- Center: `x = screenWidth/2` -> `lateralOffset = 0`
- Right edge: `x = screenWidth` -> `lateralOffset = +1`

Then we multiply by `fov` and `rowDistance`:
```scala
lateralOffset * rowDistance
```
Why? Because farther rows (y closer to horizon) = tighter spacing, so the side offset should shrink 
as `rowDistance` shrinks. That's where the perspective effect comes from.
---
### Step 3: Convert lateral shift into world space (perpendicular to view)
We want to go **perpendicular** to the angle you're facing:
- If you're looking straight up (angle = $\pi$/2), left is (-1, 0)
- If you're looking right (angle = 0), left is (0, -1)

That's why we use:
```scala
-sin(angle) -> X offset
  
+cos(angle) -> Y offset
```
So the total lateral vector becomes:
```scala
(-sin  * lateralOffset * rowDistance, +cos * lateralOffset * rowDistance)
```
---
### Final sampleX/SampleY formula
Putting it together:
```scala
val sampleX = cameraX + dx - sin * lateralOffset * rowDistance
val sampleY = cameraY + dy + cos * lateralOffset * rowDistance
```
> "Start at camera. Go forward (`dx`, `dy`). Then go left/right perpendicular to viewing angle."

That gives you the precise coordinate on the terrain image that this screen pixel corresponds to.