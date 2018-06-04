package fssi.interpreter.util

import java.nio.file.Paths
import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet}

import org.h2.server.TcpServer
import org.h2.server.web.WebServer
import org.h2.tools._
import org.slf4j.LoggerFactory

trait SnapshotDB {
  private val logger = LoggerFactory.getLogger("snapshot")

  val h2: Once[Connection]    = Once.empty[Connection]
  val tcpServer: Once[Server] = Once.empty[Server]
  val webServer: Once[Server] = Once.empty[Server]

  def initOnDemand(dbBaseDir: String, startWebConsole: Boolean = false): Unit = {
    h2 := {
      Paths.get(dbBaseDir).toFile.mkdirs()


      // 以server模式打开
      System.setProperty("h2.bindAddress", "localhost")
      val tcpArgs    = Seq("-tcp", "-tcpPort", "28080", "-baseDir", s"$dbBaseDir")
      val _tcpServer = {
        val server = new Server()
        server.runTool(tcpArgs: _*)
        server
      }

      logger.info(_tcpServer.getStatus)
      tcpServer := _tcpServer

      if (startWebConsole) {
        val webArgs = Seq("-web","-webPort", "28081", "-baseDir", s"$dbBaseDir")
        val _webServer  = {
          val server = new Server()
          server.runTool(webArgs: _*)
          server
        }

        logger.info(_webServer.getStatus)
        webServer := _webServer
      }

      // add auto close hook
      Runtime.getRuntime.addShutdownHook(new Thread(() => {
        shutdown()
      }))

      DriverManager.getConnection(s"jdbc:h2:tcp://localhost:28080/snapshot.db")
    }
  }

  def shutdown(): Unit = {
    h2.foreach(_.close())
    webServer.foreach(_.shutdown())
    tcpServer.foreach(_.shutdown())

    h2.reset()
    webServer.reset()
    tcpServer.reset()
  }

  def commit(): Unit = {
    val statement = h2.unsafe().createStatement()
    statement.execute("COMMIT")
    statement.close()
  }

  def executeCommand(sql: String, params: Any*): Int = {
    val ps = params
      .foldLeft((h2.unsafe().prepareStatement(sql), 1)) { (acc, n) =>
        acc._1.setObject(acc._2, n)
        (acc._1, acc._2 + 1)
      }
      ._1

    _ret(ps.executeUpdate()) {
      ps.close()
    }
  }

  def executeQuery[A](sql: String, params: Any*)(implicit orm: SnapshotDB.ORM[A]): Vector[A] = {
    val ps = params
      .foldLeft((h2.unsafe().prepareStatement(sql), 1)) { (acc, n) =>
        acc._1.setObject(acc._2, n)
        (acc._1, acc._2 + 1)
      }
      ._1

    val rs = ps.executeQuery()

    def transit(rs: ResultSet, acc: Vector[A]): Vector[A] = {
      if (rs.next()) transit(rs, acc :+ orm.to(rs))
      else acc
    }

    _ret(transit(rs, Vector.empty[A])) {
      rs.close()
      ps.close()
    }
  }

  private def _ret[A](a: => A)(f: => Unit): A = {
    //scala.util.Try {f}
    // if exception happened, let it explodes
    val s = a
    f
    s
  }
}

object SnapshotDB extends SnapshotDB {
  trait ORM[A] {
    def to(result: ResultSet): A
  }
  object ORM {
    def apply[A](implicit O: ORM[A]): ORM[A] = O
    def summon[A](f: ResultSet => A): ORM[A] = (result: ResultSet) => f(result)
  }
}
