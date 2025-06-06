package menu.screens

import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.{Actor, Stage}
import com.badlogic.gdx.scenes.scene2d.ui.Skin

/**
 * Defines the base interface for all UI screens in the application.
 * Each screen handles its own initialization, update logic, and disposal.
 * Provides utility methods for centering actors within the screen.
 */
trait Screen {
  private var stage: Stage = _

  private var addedActors: List[Actor] = List[Actor]()

  def addActor(actor: Actor): Unit = {
    this.stage.addActor(actor)
    addedActors ::= actor
  }

  /**
   * Initializes the screen and adds its UI elements to the provided stage.
   *
   * @param stage The Stage used to render UI actors.
   * @param skin The Skin used to style UI widgets.
   */
  def init(stage: Stage, skin: Skin): Unit = {
    this.stage = stage
  }

  /**
   * Updates the screen's logic.
   * Called once per frame in the main render loop.
   */
  def update(g: GdxGraphics): Unit

  /**
   * Disposes of the screen by clearing or removing any added actors or resources.
   * Called when switching to a different screen.
   */
  def dispose(): Unit = {
    stage.clear()
  }

  /**
   * Calculates the X coordinate needed to horizontally center the given actor.
   *
   * @param actor The actor to center.
   * @return The X position that centers the actor.
   */
  def centerX(actor: Actor): Float =
    Gdx.graphics.getWidth / 2f - actor.getWidth / 2f

  /**
   * Calculates the Y coordinate needed to vertically center the given actor.
   *
   * @param actor The actor to center.
   * @return The Y position that centers the actor.
   */
  def centerY(actor: Actor): Float =
    Gdx.graphics.getHeight / 2f - actor . getHeight / 2f
}
