package coursier.core

import coursier.util.Xml
import java.util.regex.Pattern.quote

import scala.collection.JavaConverters._
import scala.xml.{Attribute, MetaData, Null}
import org.jsoup.Jsoup

package object compatibility {

  lazy val reflectionAllowed: Boolean =
    sys.props
      .get("coursier.reflection")
      .forall(s => s != "false" && s != "0")

  private val XML: scala.xml.factory.XMLLoader[scala.xml.Elem] =
    if (reflectionAllowed)
      scala.xml.XML
    else
      // not using scala.xml.XML.withSAXParser, which makes parser a val rather than a def
      new scala.xml.factory.XMLLoader[scala.xml.Elem] {
        override def parser = {
          // The default - involves reflection, doesn't work on graalvm.
          // val f = javax.xml.parsers.SAXParserFactory.newInstance()
          val f = new com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl
          f.setNamespaceAware(false)
          f.newSAXParser()
        }
      }

  implicit class RichChar(val c: Char) extends AnyVal {
    def letterOrDigit = c.isLetterOrDigit
    def letter = c.isLetter
  }

  private val entityPattern = (quote("&") + "[a-zA-Z]+" + quote(";")).r

  private val utf8Bom = "\ufeff"

  def xmlParse(s: String): Either[String, Xml.Node] = {

    val content =
      if (entityPattern.findFirstIn(s).isEmpty)
        s
      else
        Entities.entities.foldLeft(s) {
          case (s0, (target, replacement)) =>
            s0.replace(target, replacement)
        }

    def parse =
      try Right(XML.loadString(content.stripPrefix(utf8Bom)))
      catch { case e: Exception => Left(e.toString + Option(e.getMessage).fold("")(" (" + _ + ")")) }

    def fromNode(node: scala.xml.Node): Xml.Node =
      new Xml.Node {
        assert(node != null)
        lazy val attributes = {
          def helper(m: MetaData): Stream[(String, String, String)] =
            m match {
              case Null => Stream.empty
              case attr =>
                val pre = attr match {
                  case a: Attribute => Option(node.getNamespace(a.pre)).getOrElse("")
                  case _ => ""
                }

                val value = attr.value.collect {
                  case scala.xml.Text(t) => t
                }.mkString("")

                (pre, attr.key, value) #:: helper(m.next)
            }

          helper(node.attributes).toVector
        }
        def label = node.label
        def children = node.child.map(fromNode)
        def isText = node match { case _: scala.xml.Text => true; case _ => false }
        def textContent = node.text
        def isElement = node match { case _: scala.xml.Elem => true; case _ => false }

        override def toString = node.toString
      }

    parse
      .right
      .map { n =>
        assert(n != null)
        n
      }
      .right
      .map(fromNode)
  }

  def encodeURIComponent(s: String): String =
    new java.net.URI(null, null, null, -1, s, null, null) .toASCIIString

  def listWebPageRawElements(page: String): Seq[String] =
    Jsoup.parse(page)
      .select("a")
      .asScala
      .toVector
      .map(_.attr("href"))

  def regexLookbehind: String = "<="

}
