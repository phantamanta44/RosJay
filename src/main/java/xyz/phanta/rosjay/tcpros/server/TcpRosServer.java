package xyz.phanta.rosjay.tcpros.server;

import org.slf4j.Logger;
import xyz.phanta.rosjay.node.RosNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.ServerSocket;

public class TcpRosServer {

    private final RosNode rosNode;
    private final Logger internalLogger;

    @Nullable
    private ServerInstance instance;

    public TcpRosServer(RosNode rosNode) {
        this.rosNode = rosNode;
        this.internalLogger = rosNode.getChildInternalLogger("tcpros_server");
    }

    public void serve(int port) throws IOException {
        if (instance != null) {
            throw new IllegalStateException("TCPROS server is already running!");
        }

        internalLogger.trace("Opening server socket...");
        ServerSocket socket = new ServerSocket(port);

        internalLogger.trace("Starting server thread...");
        TcpRosServerThread serverThread = new TcpRosServerThread(rosNode, socket, internalLogger);
        serverThread.start();
        instance = new ServerInstance(socket, serverThread);
    }

    public int getActivePort() {
        if (instance == null) {
            throw new IllegalStateException("TCPROS server is not running!");
        }
        return instance.getPort();
    }

    public void kill() throws IOException {
        if (instance == null) {
            throw new IllegalStateException("TCPROS server is not running!");
        }
        instance.kill();
        instance = null;
    }

    private class ServerInstance {

        private final ServerSocket socket;
        private final TcpRosServerThread serverThread;

        ServerInstance(ServerSocket socket, TcpRosServerThread serverThread) {
            this.socket = socket;
            this.serverThread = serverThread;
        }

        int getPort() {
            return socket.getLocalPort();
        }

        void kill() throws IOException {
            internalLogger.trace("Closing server socket...");
            socket.close();

            try {
                internalLogger.debug("Waiting for server thread to terminate...");
                serverThread.join();
            } catch (InterruptedException e) {
                internalLogger.error("Interrupted while waiting for TCPROS server to clean up!", e);
            }
        }

    }

}
