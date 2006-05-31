package jif.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class provides some useful utilities for Jif programs using Java's
 * sockets.
 */
public class SocketUtil {
    private SocketUtil() { }

    /**
     * Listen to the ServerSocket, and pass any new connections to the 
     * SocketAcceptor, in a new thread.
     * @param ss
     * @param a
     */
    public void acceptConnections(ServerSocket ss, final SocketAcceptor a) {
        if (ss == null || a == null) return;
        while (true) {
            try {
                Socket s = ss.accept();                
                new Thread(new SocketAcceptorRunner(a,s)).run();                               
            }
            catch (Exception e) {
                // recover silently
            }
        }
        
    }
    private class SocketAcceptorRunner implements Runnable {
        private final SocketAcceptor a;
        private final Socket s;
        SocketAcceptorRunner(SocketAcceptor a, Socket s) {
            this.a = a;
            this.s = s;
        }
        public void run() {
            try {
                a.accept(s.getInputStream(), s.getOutputStream());
            }
            catch (Exception e) {
                // just fail silently
            }
        }
        
    }
}
