package coursier

import java.io.File

import scala.collection.mutable.ListBuffer

sealed abstract class FileError(
  val `type`: String,
  val message: String,
  val causeOpt: Option[Throwable] = None
) extends Product with Serializable {
  def describe: String = {
    val b = new ListBuffer[String]
    b += s"${`type`}: $message"

    def addEx(t: Throwable): Unit =
      if (t != null) {
        b += t.toString
        for (l <- t.getStackTrace)
          b += "  " + l
        addEx(t.getCause)
      }

    causeOpt.foreach(addEx)

    b.mkString("\n")
  }

  final def notFound: Boolean = this match {
    case _: FileError.NotFound => true
    case _ => false
  }
}

object FileError {

  final case class DownloadError(reason: String, causeOpt0: Option[Throwable] = None) extends FileError(
    "download error",
    reason,
    causeOpt0
  )

  final case class NotFound(
    file: String,
    permanent: Option[Boolean] = None
  ) extends FileError(
    "not found",
    file
  )

  final case class Unauthorized(
    file: String,
    realm: Option[String]
  ) extends FileError(
    "unauthorized",
    file + realm.fold("")(" (" + _ + ")")
  )

  final case class ChecksumNotFound(
    sumType: String,
    file: String
  ) extends FileError(
    "checksum not found",
    file
  )

  final case class ChecksumFormatError(
    sumType: String,
    file: String
  ) extends FileError(
    "checksum format error",
    file
  )

  final case class WrongChecksum(
    sumType: String,
    got: String,
    expected: String,
    file: String,
    sumFile: String
  ) extends FileError(
    "wrong checksum",
    file
  )

  sealed abstract class Recoverable(
    `type`: String,
    message: String
  ) extends FileError(`type`, message)
  final case class Locked(file: File) extends Recoverable(
    "locked",
    file.toString
  )
  final case class ConcurrentDownload(url: String) extends Recoverable(
    "concurrent download",
    url
  )

}
