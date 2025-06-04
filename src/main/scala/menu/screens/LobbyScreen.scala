package menu.screens

import cats.effect.unsafe.implicits.global
import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.{Label, Skin, TextButton, TextField}
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.{Actor, InputEvent, Stage}
import com.badlogic.gdx.utils.Align
import game.Motor
import menu.{Game, MainMenu, Menu}
import server.Server

class LobbyScreen extends Screen {
  private var stage: Stage = _

  private var lblLobby: Label = _
  private var txtUsername: TextField = _
  private var btnConnect: TextButton = _
  private var btnBack: TextButton = _
  private var btnReady: TextButton = _

  private var addedActors: List[Actor] = List[Actor]()

  /**
   * Initializes the screen and adds its UI elements to the provided stage.
   *
   * @param stage The Stage used to render UI actors.
   * @param skin  The Skin used to style UI widgets.
   */
  override def init(stage: Stage, skin: Skin): Unit = {
    this.stage = stage

    val labelStyle = new Label.LabelStyle(Menu.menu.consolasFont, Color.WHITE)
    lblLobby = new Label("", labelStyle)
    lblLobby.setAlignment(Align.left)
    lblLobby.setSize(300, 500)
    lblLobby.setPosition(20, centerY(lblLobby))

    txtUsername = new TextField(Server.usernameUnsafe, skin)
    txtUsername.setSize(200, 60)
    txtUsername.setPosition(centerX(txtUsername), centerY(txtUsername) - 40)
    txtUsername.setMaxLength(16)

    btnConnect = new TextButton("Connect", skin)
    btnConnect.setSize(200, 60)
    btnConnect.setPosition(centerX(btnConnect), centerY(btnConnect) + 40)

    btnConnect.addListener(new ClickListener {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
        if (!txtUsername.getText.isBlank) {
          Server.init(txtUsername.getText, "PROD").unsafeRunAndForget()
          txtUsername.setDisabled(true)
          btnConnect.setVisible(false)
          btnReady.setVisible(true)
        }
      }
    })

    btnReady = new TextButton("Ready", skin)
    btnReady.setSize(200, 60)
    btnReady.setPosition(centerX(btnReady), centerY(btnReady) + 40)
    btnReady.addListener(new ClickListener {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
        if (Server.socketUnsafe.isDefined) {
          Server.sendReady(Server.socketUnsafe.get, Server.defaultUUID, !Server.readyUnsafe, Server.MsgType.ReadyUpdate).unsafeRunAndForget()
        }
      }
    })
    btnReady.setVisible(false)

    if (Server.socketUnsafe.isDefined) {
      btnConnect.setVisible(false)
      btnReady.setVisible(true)
    }

    btnBack = new TextButton("Back", skin)
    btnBack.setSize(200, 60)
    btnBack.setPosition(20, stage.getHeight - (20 + btnBack.getHeight))

    btnBack.addListener(new ClickListener {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
        Menu.screenManager.switchTo(MainMenu)
      }
    })

    this.stage.addActor(txtUsername)
    this.stage.addActor(btnConnect)
    this.stage.addActor(btnBack)
    this.stage.addActor(btnReady)
    this.stage.addActor(lblLobby)
    addedActors ::= txtUsername
    addedActors ::= btnConnect
    addedActors ::= btnBack
    addedActors ::= btnReady
    addedActors ::= lblLobby
  }

  def updateBtnReady(): Unit = {
    var txt = "Ready"
    var c = Color.LIME
    if (!Server.readyUnsafe) {
      txt = "Not Ready"
      c = Color.SCARLET
    }
    btnReady.setText(txt)
    btnReady.setColor(c)
  }

  /**
   * Updates the screen's logic.
   * Called once per frame in the main render loop.
   */
  override def update(g: GdxGraphics): Unit = {
    if (Motor.initGame) Menu.screenManager.switchTo(Game)
    lblLobby.setText(Server.lobbyUnsafe)
    updateBtnReady()
  }

  /**
   * Disposes of the screen by clearing or removing any added actors or resources.
   * Called when switching to a different screen.
   */
  override def dispose(): Unit = {
    stage.clear()
  }
}
