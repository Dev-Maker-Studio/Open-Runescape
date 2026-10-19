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
        val inetSocketAddress = socketAddress
        if (proxy.type() == Type.HTTP) {
            var proxyAuthHeader: String? = null

            try {
                val authInfoClass = Class.forName("sun.net.www.protocol.http.AuthenticationInfo")
                val getProxyAuth = authInfoClass.getDeclaredMethod("getProxyAuth", String::class.java, Int::class.javaPrimitiveType)
                getProxyAuth.isAccessible = true
                val authInfo = getProxyAuth.invoke(null, inetSocketAddress.hostName, Integer.valueOf(inetSocketAddress.port))
                if (authInfo != null) {
                    val supportsPreemptiveAuth = authInfoClass.getDeclaredMethod("supportsPreemptiveAuthorization")
                    supportsPreemptiveAuth.isAccessible = true
                    if (supportsPreemptiveAuth.invoke(authInfo) as Boolean) {
                        val getHeaderName = authInfoClass.getDeclaredMethod("getHeaderName")
                        getHeaderName.isAccessible = true
                        val getHeaderValue = authInfoClass.getDeclaredMethod("getHeaderValue", URL::class.java, String::class.java)
                        getHeaderValue.isAccessible = true
                        val headerName = getHeaderName.invoke(authInfo) as String
                        val headerValue = getHeaderValue.invoke(authInfo, URL("https://$siteHostName/"), "https") as String
                        proxyAuthHeader = "$headerName: $headerValue"
                    }
                }
            } catch (ignored: Exception) {
                // NOOP
            }

            return proxySocketConnect(inetSocketAddress.hostName, inetSocketAddress.port, proxyAuthHeader)
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
        val statusLine = bufferedReader.readLine()
        if (statusLine != null) {
            if (statusLine.startsWith("HTTP/1.0 200") || statusLine.startsWith("HTTP/1.1 200")) {
                return socket
            }
            if (statusLine.startsWith("HTTP/1.0 407") || statusLine.startsWith("HTTP/1.1 407")) {
                var loops = 0
                val proxyAuthenticateHeader = "proxy-authenticate: "
                var line = bufferedReader.readLine()
                while (line != null && loops < 50) {
                    if (line.lowercase().startsWith(proxyAuthenticateHeader)) {
                        var authLine = line.substring(proxyAuthenticateHeader.length).trim()
                        val spaceIdx = authLine.indexOf(' ')
                        if (spaceIdx != -1) {
                            authLine = authLine.substring(0, spaceIdx)
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
        val useSystemProxies = java.lang.Boolean.parseBoolean(System.getProperty("java.net.useSystemProxies"))
        if (!useSystemProxies) {
            System.setProperty("java.net.useSystemProxies", "true")
        }
        val secureConnection = sitePort * 877634459 == 443
        val primaryProxies: List<Proxy>
        val fallbackProxies: List<Proxy>
        try {
            primaryProxies = proxySelector!!.select(URI((if (secureConnection) "https" else "http") + "://$siteHostName"))
            fallbackProxies = proxySelector.select(URI((if (secureConnection) "http" else "https") + "://$siteHostName"))
        } catch (ignored: URISyntaxException) {
            return getLobbySocket()
        }
        val combinedProxies = primaryProxies.toMutableList()
        combinedProxies.addAll(fallbackProxies)
        val proxyArray = combinedProxies.toTypedArray()
        var proxyException: ProxyIOException? = null
        val proxies = proxyArray
        for (i in proxies.indices) {
            val proxyEntry = proxies[i]
            val proxy = proxyEntry as Proxy
            try {
                val socket = getProxySocket(proxy)
                if (socket != null) {
                    return socket
                }
            } catch (ex: ProxyIOException) {
                proxyException = ex
            } catch (ignored: IOException) {
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
