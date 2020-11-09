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

import org.junit.Test;
import static org.junit.Assert.fail;
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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Creates actual HTTPS connections to real webservers to exercise the TLS stack.
 */
public class DefaultTlsFunctionalTest {

    private final static String[] TLS_HOSTS = {
            "https://www.cloudflare.com/",
            "https://www.google.com/",
            "https://services.gradle.org/", // Ensure that Gradle can be downloaded.
            "https://repo1.maven.org/" // Ensure that Maven artifacts can be resolved.
    };

    @Test
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String versionString = System.getProperty("java.version");
        System.out.println("Running on Java version: " + versionString);

        SSLContext sc = SSLContext.getDefault();

        int unreachableCounter = 0;
        HttpsURLConnection con = null;
        try {
            for (String host : TLS_HOSTS) {
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

        // Fail if we could not reach any host because this means we cannot say 
        // anything about the state of the TLS stack.
        if (unreachableCounter == TLS_HOSTS.length) {
            fail("Could not reach any host");  
        }
    }

    static class SSLSessionExaminingHandshakeListener implements HandshakeCompletedListener {

        private final String host;

        public SSLSessionExaminingHandshakeListener(String host) {
            this.host = host;
        }

        @Override
        public void handshakeCompleted(HandshakeCompletedEvent event) {
            System.out.println("Connected to: " + this.host);
            try {
                System.out.println(event.getPeerPrincipal().getName());
            } catch (SSLPeerUnverifiedException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Protocol: " + event.getSession().getProtocol());
            System.out.println("Supported cipher suites: " +
                    Arrays.toString(event.getSocket().getSupportedCipherSuites()));
            System.out.println("Enabled cipher suites: " + Arrays.toString(event.getSocket().getEnabledCipherSuites()));
            System.out.println("Selected cipher suite: " + event.getSession().getCipherSuite());
            System.out.println();
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