import com.esotericsoftware.kryonet.{Client, Connection, Listener}

object ConnectionTests extends App {
  val c: Client = new Client()
  c.start()

  val kryo = c.getKryo
  kryo.register(classOf[SomeRequest])
  kryo.register(classOf[SomeResponse])

  c.connect(5000, "10.76.214.36", 7897, 7898)

  val r = new SomeRequest
  r.text = "Here is the request"
  c.sendTCP(r)

  while (true) {}

  c.addListener(new Listener {
    override def received(connection: Connection, o: Any): Unit = {
      o match {
        case r: SomeResponse =>
          System.out.println(r.text)
        case _ =>
      }
    }
  })
}

class SomeRequest {
  var text: String = null
}

class SomeResponse {
  var text: String = null
}