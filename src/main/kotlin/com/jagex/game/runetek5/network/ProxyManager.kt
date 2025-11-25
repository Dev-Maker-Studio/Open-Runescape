package com.jagex.game.runetek5.network

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.reflect.Method
import java.net.*
import java.net.Proxy.Type
import java.nio.charset.StandardCharsets

open class ProxyManager : SocketManager() {

    @JvmField val proxySelector: ProxySelector? = ProxySelector.getDefault()

    // Unused
    @Throws(IOException::class)
    fun getProxySocket(proxy: Proxy): Socket? {
        if (proxy.type() == Type.DIRECT) {
            return getLobbySocket()
        }
        val socketAddress = proxy.address()
        if (socketAddress !is InetSocketAddress) {
            return null
        }
        val internetSocketAddress = socketAddress
        if (proxy.type() == Type.HTTP) {
            var string_15: String? = null

            try {
                val clazz = Class.forName("sun.net.www.protocol.http.AuthenticationInfo")
                val getProxyAuth = clazz.getDeclaredMethod("getProxyAuth", String::class.java, Int::class.javaPrimitiveType)
                getProxyAuth.isAccessible = true
                val proxyAuth = getProxyAuth.invoke(null, internetSocketAddress.hostName, Integer.valueOf(internetSocketAddress.port))
                if (proxyAuth != null) {
                    val supportsPreemptiveAuth = clazz.getDeclaredMethod("supportsPreemptiveAuthorization")
                    supportsPreemptiveAuth.isAccessible = true
                    if (supportsPreemptiveAuth.invoke(proxyAuth) as Boolean) {
                        val getHeaderName = clazz.getDeclaredMethod("getHeaderName")
                        getHeaderName.isAccessible = true
                        val getHeaderValue = clazz.getDeclaredMethod("getHeaderValue", URL::class.java, String::class.java)
                        getHeaderValue.isAccessible = true
                        val headerName = getHeaderName.invoke(proxyAuth) as String
                        val headerValue = getHeaderValue.invoke(proxyAuth, URL("https://$siteHostName/"), "https") as String
                        string_15 = "$headerName: $headerValue"
                    }
                }
            } catch (ignored: Exception) {
                // NOOP
            }

            return proxySocketConnect(internetSocketAddress.hostName, internetSocketAddress.port, string_15)
        } else if (proxy.type() == Type.SOCKS) {
            val socket = Socket(proxy)
            socket.connect(InetSocketAddress(siteHostName, 877634459 * sitePort))
            return socket
        }
        return null
    }

    // Unused
    @Throws(IOException::class)
    fun proxySocketConnect(host: String, port: Int, customHeader: String?): Socket? {
        val socket = Socket(host, port)
        socket.soTimeout = 10000
        val outputStream = socket.getOutputStream()
        if (customHeader == null) {
            outputStream.write(("CONNECT $siteHostName:${sitePort * 877634459} HTTP/1.0\n\n").toByteArray(StandardCharsets.ISO_8859_1))
        } else {
            outputStream.write(("CONNECT $siteHostName:${877634459 * sitePort} HTTP/1.0\n$customHeader\n\n").toByteArray(StandardCharsets.ISO_8859_1))
        }
        outputStream.flush()
        val bufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val httpHeader = bufferedReader.readLine()
        if (httpHeader != null) {
            if (httpHeader.startsWith("HTTP/1.0 200") || httpHeader.startsWith("HTTP/1.1 200")) {
                return socket
            }
            if (httpHeader.startsWith("HTTP/1.0 407") || httpHeader.startsWith("HTTP/1.1 407")) {
                var loops = 0
                val proxyAuth = "proxy-authenticate: "
                var line = bufferedReader.readLine()
                while (line != null && loops < 50) {
                    if (line.lowercase().startsWith(proxyAuth)) {
                        var authLine = line.substring(proxyAuth.length).trim()
                        val proxyAuthStart = authLine.indexOf(' ')
                        if (proxyAuthStart != -1) {
                            authLine = authLine.substring(0, proxyAuthStart)
                        }
                        throw ProxyIOException(authLine)
                    }
                    line = bufferedReader.readLine()
                    loops++
                }
                throw ProxyIOException("")
            }
        }
        outputStream.close()
        bufferedReader.close()
        socket.close()
        return null
    }

    // Unused
    @Throws(IOException::class)
    override fun getProxySocket(): Socket {
        val useProxies = java.lang.Boolean.parseBoolean(System.getProperty("java.net.useSystemProxies"))
        if (!useProxies) {
            System.setProperty("java.net.useSystemProxies", "true")
        }
        val secureConnection = sitePort * 877634459 == 443
        val proxiesA: List<Proxy>
        val proxiesB: List<Proxy>
        try {
            proxiesA = proxySelector!!.select(URI((if (secureConnection) "https" else "http") + "://$siteHostName"))
            proxiesB = proxySelector.select(URI((if (secureConnection) "http" else "https") + "://$siteHostName"))
        } catch (uri: URISyntaxException) {
            return getLobbySocket()
        }
        val combinedProxies = proxiesA.toMutableList()
        combinedProxies.addAll(proxiesB)
        val proxiesAArray = combinedProxies.toTypedArray()
        var proxyException: ProxyIOException? = null
        val proxiesACopy = proxiesAArray
        for (loop in proxiesACopy.indices) {
            val proxyObject = proxiesACopy[loop]
            val proxy = proxyObject as Proxy
            try {
                val socket = getProxySocket(proxy)
                if (socket != null) {
                    return socket
                }
            } catch (pioe: ProxyIOException) {
                proxyException = pioe
            } catch (ioe: IOException) {
                // NOOP
            }
        }
        if (proxyException != null) {
            throw proxyException
        } else {
            return getLobbySocket()
        }
    }
}
