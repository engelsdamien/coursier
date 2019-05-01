package coursier.cache.protocol

import java.net.{InetAddress, Socket, URLStreamHandler, URLStreamHandlerFactory}

import com.squareup.okhttp.{OkHttpClient, OkUrlFactory}
import javax.net.ssl.SSLSocketFactory

final class DummySSLSocketFactory extends SSLSocketFactory {
  override def getDefaultCipherSuites = ???
  override def getSupportedCipherSuites = ???
  override def createSocket(socket: Socket, s: String, i: Int, b: Boolean) = ???
  override def createSocket(s: String, i: Int) = ???
  override def createSocket(s: String, i: Int, inetAddress: InetAddress, i1: Int) = ???
  override def createSocket(inetAddress: InetAddress, i: Int) = ???
  override def createSocket(inetAddress: InetAddress, i: Int, inetAddress1: InetAddress, i1: Int) = ???
}

object HttpHandler {
  lazy val okHttpClient = {
    val c = new OkHttpClient
    c.setSslSocketFactory(new DummySSLSocketFactory)
    c
  }
  lazy val okHttpFactory = new OkUrlFactory(okHttpClient)
}

class HttpHandler extends URLStreamHandlerFactory {
  def createURLStreamHandler(protocol: String): URLStreamHandler =
    HttpHandler.okHttpFactory.createURLStreamHandler(protocol)
}

class HttpsHandler extends HttpHandler
