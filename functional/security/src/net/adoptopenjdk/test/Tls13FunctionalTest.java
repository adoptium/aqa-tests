/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adoptopenjdk.test;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates actual HTTPS connections to real webservers to exercise the TLS stack. Connections that do not use TLSv1.3
 * are rejected.
 * <p>
 * TLSv1.3 was introduced with OpenJDK 11 (see https://openjdk.java.net/jeps/332) and backported to OpenJDK 8u272
 * (https://bugs.openjdk.java.net/browse/JDK-8245466). Tests are automatically skipped on versions that do not support
 * TLSv1.3.
 */
@Test(groups={ "level.extended" })
public class Tls13FunctionalTest {

    /**
     * Matches JDK version numbers that were used before JEP 223 came into effect (JDK 8 and earlier).
     */
    private static final Pattern PRE_223_PATTERN = Pattern.compile(
            "^(?<version>1\\.(?<major>[0-8]+)\\.0(_(?<update>[0-9]+)))(-(?<additional>.*)?)$"
    );

    private static Logger logger = Logger.getLogger(Tls13FunctionalTest.class);

    private final static String[] TLS_13_HOSTS = {
            "https://enabled.tls13.com/",
            "https://www.cloudflare.com/",
            "https://tls13.akamai.io/",
            "https://swifttls.org/",
            "https://www.mozilla.org/"
    };

    @Test
    public void testTls13() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        String versionString = System.getProperty("java.version");
        logger.info("Running on Java version: " + versionString);

        if (!isSupportedPlatform(versionString)) {
            return;
        }

        // Force TLSv1.3 connections. Fails if TLS 1.3 is not supported (OpenJDK 8u265 and earlier without additional
        // patches).
        SSLContext sc = SSLContext.getInstance("TLSv1.3");
        sc.init(null, null, null);

        int unreachableCounter = 0;
        HttpsURLConnection con = null;
        try {
            for (String host : TLS_13_HOSTS) {
                SSLSocketFactory sslSocketFactory = new DecoratedSSLSocketFactory(sc.getSocketFactory(),
                        new SSLSessionExaminingHandshakeListener(host));

                URL url = new URL(host);
                con = (HttpsURLConnection) url.openConnection();
                con.setSSLSocketFactory(sslSocketFactory);

                // Servers can be unreachable. Fail later if we cannot reach any.
                try {
                    con.connect();
                } catch (UnknownHostException | SocketTimeoutException ex) {
                    unreachableCounter += 1;
                    ex.printStackTrace(System.err);
                    System.err.println();
                    continue;
                }
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        // Fail if we could not reach any host because this means we cannot say anything about the state of the TLS
        // stack.
        if (unreachableCounter == TLS_13_HOSTS.length) {
            Assert.fail("Could not reach any host supporting TLSv1.3");
        }
    }

    /**
     * Determines if a platform is supported by looking at the version string.
     */
    private static boolean isSupportedPlatform(String versionString) {
        boolean isSupportedPlatform = true;
        Matcher pre223Matcher = PRE_223_PATTERN.matcher(versionString);
        if (pre223Matcher.matches()) {
            int majorVersion = Integer.parseInt(pre223Matcher.group("major"));
            int updateVersion = Integer.parseInt(pre223Matcher.group("update"));
            if (majorVersion != 8) {
                logger.warn("Skipping tests because JDK is unsupported: " + versionString);
                isSupportedPlatform = false;
            }
            if (updateVersion < 272) {
                logger.warn("Skipping tests because JDK is unsupported: " + versionString);
                isSupportedPlatform = false;
            }
        } else {
            int majorVersion = Integer.parseInt(versionString.substring(0, versionString.indexOf('.')));
            if (majorVersion < 11) {
                logger.warn("Skipping tests because JDK is unsupported: " + versionString);
                isSupportedPlatform = false;
            }
        }
        return isSupportedPlatform;
    }

    static class SSLSessionExaminingHandshakeListener implements HandshakeCompletedListener {

        private final String host;

        public SSLSessionExaminingHandshakeListener(String host) {
            this.host = host;
        }

        @Override
        public void handshakeCompleted(HandshakeCompletedEvent event) {
            logger.info("Connected to: " + this.host);
            try {
                logger.info(event.getPeerPrincipal().getName());
            } catch (SSLPeerUnverifiedException e) {
                throw new RuntimeException(e);
            }

            String protocol = event.getSession().getProtocol();
            if (!protocol.equalsIgnoreCase("TLSv1.3")) {
                throw new RuntimeException("Expected TLSv1.3 connection but was: " + protocol);
            }
            logger.info("Protocol: " + protocol);
            logger.info("Supported cipher suites: " +
                    Arrays.toString(event.getSocket().getSupportedCipherSuites()));
                    logger.info("Enabled cipher suites: " + Arrays.toString(event.getSocket().getEnabledCipherSuites()));
                    logger.info("Selected cipher suite: " + event.getSession().getCipherSuite());
        }
    }

    static class DecoratedSSLSocketFactory extends SSLSocketFactory {

        private final SSLSocketFactory delegate;

        private final HandshakeCompletedListener handshakeCompletedListener;

        DecoratedSSLSocketFactory(SSLSocketFactory delegate, HandshakeCompletedListener handshakeCompletedListener) {
            this.delegate = delegate;
            this.handshakeCompletedListener = handshakeCompletedListener;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return this.delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return this.delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            SSLSocket socket = (SSLSocket) this.delegate.createSocket(s, host, port, autoClose);
            socket.addHandshakeCompletedListener(this.handshakeCompletedListener);
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            SSLSocket socket = (SSLSocket) this.delegate.createSocket(host, port);
            socket.addHandshakeCompletedListener(this.handshakeCompletedListener);
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            SSLSocket socket = (SSLSocket) this.delegate.createSocket(host, port, localHost, localPort);
            socket.addHandshakeCompletedListener(this.handshakeCompletedListener);
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            SSLSocket socket = (SSLSocket) this.delegate.createSocket(host, port);
            socket.addHandshakeCompletedListener(this.handshakeCompletedListener);
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
                throws IOException {
            SSLSocket socket = (SSLSocket) this.delegate.createSocket(address, port, localAddress, localPort);
            socket.addHandshakeCompletedListener(this.handshakeCompletedListener);
            return socket;
        }
    }
}