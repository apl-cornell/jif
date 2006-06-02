package jif.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import jif.lang.Label;

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
    public static void acceptConnections(Label lbl, ServerSocket ss, final SocketAcceptor a) {
        if (ss == null || a == null) return;
        while (true) {
            try {
                Socket s = ss.accept();  
                new Thread(new SocketAcceptorRunner(a,s)).run();   
                s.close();
            }
            catch (Exception e) {
                // recover silently
            }
        }
        
    }
    private static class SocketAcceptorRunner implements Runnable {
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
