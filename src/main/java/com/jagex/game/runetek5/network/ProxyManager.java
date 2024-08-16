package com.jagex.game.runetek5.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.*;
import java.net.Proxy.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProxyManager extends SocketManager {

    public final ProxySelector proxySelector = ProxySelector.getDefault();

    // Unused
    public Socket getProxySocket(Proxy proxy) throws IOException {
        if (proxy.type() == Type.DIRECT) {
            return getLobbySocket();
        }
        SocketAddress socketAddress = proxy.address();
        if (!(socketAddress instanceof InetSocketAddress internetSocketAddress)) {
            return null;
        }
        if (proxy.type() == Type.HTTP) {
            String string_15 = null;

            try {
                Class<?> clazz = Class.forName("sun.net.www.protocol.http.AuthenticationInfo");
                Method getProxyAuth = clazz.getDeclaredMethod("getProxyAuth", String.class, Integer.TYPE);
                getProxyAuth.setAccessible(true);
                Object proxyAuth = getProxyAuth.invoke(null, internetSocketAddress.getHostName(), Integer.valueOf(internetSocketAddress.getPort()));
                if (proxyAuth != null) {
                    Method supportsPreemptiveAuth = clazz.getDeclaredMethod("supportsPreemptiveAuthorization");
                    supportsPreemptiveAuth.setAccessible(true);
                    if (((Boolean) supportsPreemptiveAuth.invoke(proxyAuth, new Object[0])).booleanValue()) {
                        Method getHeaderName = clazz.getDeclaredMethod("getHeaderName");
                        getHeaderName.setAccessible(true);
                        Method getHeaderValue = clazz.getDeclaredMethod("getHeaderValue", URL.class, String.class);
                        getHeaderValue.setAccessible(true);
                        String headerName = (String) getHeaderName.invoke(proxyAuth, new Object[0]);
                        String headerValue = (String) getHeaderValue.invoke(proxyAuth, new Object[]{new URL("https://" + super.siteHostName + "/"), "https"});
                        string_15 = headerName + ": " + headerValue;
                    }
                }
            } catch (Exception ignored) {
            	// NOOP
            }

            return proxySocketConnect(internetSocketAddress.getHostName(), internetSocketAddress.getPort(), string_15);
        } else if (proxy.type() == Type.SOCKS) {
            Socket socket = new Socket(proxy);
            socket.connect(new InetSocketAddress(super.siteHostName, 877634459 * super.sitePort));
            return socket;
        } 
        return null;
    }

    // Unused
    public Socket proxySocketConnect(String host, int port, String customHeader) throws IOException {
        Socket socket = new Socket(host, port);
        socket.setSoTimeout(10000);
        OutputStream outputStream = socket.getOutputStream();
        if (customHeader == null) {
            outputStream.write(("CONNECT " + super.siteHostName + ":" + super.sitePort * 877634459 + " HTTP/1.0\n\n").getBytes(StandardCharsets.ISO_8859_1));
        } else {
            outputStream.write(("CONNECT " + super.siteHostName + ":" + 877634459 * super.sitePort + " HTTP/1.0\n" + customHeader + "\n\n").getBytes(StandardCharsets.ISO_8859_1));
        }
        outputStream.flush();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String httpHeader = bufferedReader.readLine();
        if (httpHeader != null) {
            if (httpHeader.startsWith("HTTP/1.0 200") || httpHeader.startsWith("HTTP/1.1 200")) {
                return socket;
            }
            if (httpHeader.startsWith("HTTP/1.0 407") || httpHeader.startsWith("HTTP/1.1 407")) {
                int loops = 0;
                String proxyAuth = "proxy-authenticate: ";
                for (String line = bufferedReader.readLine(); line != null && loops < 50; loops++) {
                    if (line.toLowerCase().startsWith(proxyAuth)) {
                        line = line.substring(proxyAuth.length()).trim();
                        int proxyAuthStart = line.indexOf(32);
                        if (proxyAuthStart != -1) {
                            line = line.substring(0, proxyAuthStart);
                        }
                        throw new ProxyIOException(line);
                    }
                    line = bufferedReader.readLine();
                }
                throw new ProxyIOException("");
            }
        }
        outputStream.close();
        bufferedReader.close();
        socket.close();
        return null;
    }

    // Unused
    @Override
    public Socket getProxySocket() throws IOException {
        boolean useProxies = Boolean.parseBoolean(System.getProperty("java.net.useSystemProxies"));
        if (!useProxies) {
            System.setProperty("java.net.useSystemProxies", "true");
        }
        boolean secureConnection = super.sitePort * 877634459 == 443;
        List<Proxy> proxiesA;
        List<Proxy> proxiesB;
        try {
            proxiesA = this.proxySelector.select(new URI((secureConnection ? "https" : "http") + "://" + super.siteHostName));
            proxiesB = this.proxySelector.select(new URI((secureConnection ? "http" : "https") + "://" + super.siteHostName));
        } catch (URISyntaxException uri) {
            return getLobbySocket();
        }
        proxiesA.addAll(proxiesB);
        Object[] proxiesAArray = proxiesA.toArray();
        ProxyIOException proxyException = null;
        Object[] proxiesACopy = proxiesAArray;
        for (int loop = 0; loop < proxiesACopy.length; loop++) {
            Object proxyObject = proxiesACopy[loop];
            Proxy proxy = (Proxy) proxyObject;
            try {
                Socket socket = getProxySocket(proxy);
                if (socket != null) {
                    return socket;
                }
            } catch (ProxyIOException pioe) {
                proxyException = pioe;
            } catch (IOException ioe) {
            	// NOOP
            }
        }
        if (proxyException != null) {
            throw proxyException;
        } else {
            return getLobbySocket();
        }
    }
}
