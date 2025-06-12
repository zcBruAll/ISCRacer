# ISCRacer

ISCRacer is a small retro-inspired racing game written in Scala.  
It uses the **gdx2d** library to build a light weight LibGDX desktop application and relies on a custom Mode7 renderer to turn a 2D track texture into a pseudo‑3D world.  
A network client based on **fs2** streams allows multiple players to race together.

## Installation

1. Install a JDK (version 17 or later is recommended) and [sbt](https://www.scala-sbt.org/).
2. Clone this repository and enter the project directory:
   ```bash
   git clone https://github.com/zcBruAll/ISCRacer.git
   cd ISCRacer
   ```
3. To run the game directly use:
   ```bash
   sbt run
   ```
   This compiles the sources and launches the game window.
4. To build a standalone fat jar you can run:
   ```bash
   sbt assembly
   ```
   The jar will be written to `target/scala-2.13/`.

## Gameplay

Upon launching the application a simple menu appears.  Select **Play** to join the lobby.

1. **Lobby:** enter a username and press **Connect** to join the server. When you are ready to race click **Ready**.  Once all players signal ready, the game will start.
2. **Controls:** during the race use the keyboard or the controller:
   - `W` / `S` or `R2` / `L2` - accelerate or brake
   - `A` / `D` or `Left joystick` - steer left or right
   - `Shift` or `L1` or `A button` - handbrake

   These mappings are defined in `Motor.scala`
4. A mini‑map, lap timer and simple leaderboard are displayed while racing. When the race finishes you will be returned to the lobby with the results.

Watch a brief a video of the project gameplay: [ISCRacer.mp4](https://github.com/zcBruAll/ISCRacer/blob/master/ISCRacer.mp4)

## Implementation Notes

### Mode7 Renderer

The core of the pseudo‑3D effect is implemented in `Mode7Renderer.scala` which loads a GLSL fragment shader and feeds it with camera parameters:
```scala
class Mode7Renderer(texture: Texture) {
  private val fragmentShader: String = Gdx.files.internal("src/main/assets/shaders/mode7.frag").readString()
  private val shader = new ShaderProgram(vertexShader, fragmentShader)
  def render(cameraX: Float, cameraY: Float, angle: Float, scale: Float, fov: Float, horizon: Float): Unit = {
    shader.setUniformf("u_cameraX", cameraX / texture.getWidth)
    shader.setUniformf("u_cameraY", cameraY / texture.getHeight)
    shader.setUniformf("u_angle", angle)
    shader.setUniformf("u_scale", scale)
    shader.setUniformf("u_fov", fov)
    shader.setUniformf("u_screenWidth", w.toFloat)
    shader.setUniformf("u_screenHeight", h.toFloat - horizon)
    shader.setUniformf("u_horizon", horizon)
    batch.draw(texture, -(w/2), -(h/2), w, h)
  }
}
```
[src/main/scala/game/Mode7Renderer.scala](https://github.com/zcBruAll/ISCRacer/blob/master/src/main/scala/game/Mode7Renderer.scala)

The shader (`mode7.frag`) computes for each pixel which texel on the track image should be displayed:
```glsl
float rowDistance = u_scale / row;
float offset = (fragCoord.x - u_screenWidth / 2.0) / (u_screenWidth / 2.0) * u_fov;
float sampleX = u_cameraX + rowDistance * cosA - offset * rowDistance * sinA;
float sampleY = u_cameraY + rowDistance * sinA + offset * rowDistance * cosA;
vec2 sampleUV = vec2(mod(sampleX, 1.0), mod(sampleY, 1.0));
```
[src/main/assets/shaders/mode7.frag](https://github.com/zcBruAll/ISCRacer/blob/master/src/main/assets/shaders/mode7.frag)  
A more detailed derivation of this projection can be found in [`src/main/assets/game/map/tracks/mathematical_theory_mode7.md`](https://github.com/zcBruAll/ISCRacer/blob/master/src/main/assets/game/map/tracks/mathematical_theory_mode7.md)

### Networking

`Server.scala` manages TCP and UDP communication with the game server using **fs2** streams. It handles handshake, lobby state, real‑time car updates and player inputs. The client can be started with `Server.init(username)` which spawns the networking streams.

---
Feel free to explore the source for more details or tweak the assets under `src/main/assets` to create new tracks or karts.
