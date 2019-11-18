package xyz.phanta.rosjay.tcpros.server;

import org.slf4j.Logger;
import xyz.phanta.rosjay.node.RosNode;
import xyz.phanta.rosjay.tcpros.TcpRosHeader;
import xyz.phanta.rosjay.tcpros.stator.ExpectHeaderDatagram;
import xyz.phanta.rosjay.tcpros.stator.TcpStateMachine;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.transport.spec.DataTypeSpecification;
import xyz.phanta.rosjay.util.id.RosId;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class TcpRosServerThread extends Thread {

    private final RosNode rosNode;
    private final ServerSocket serverSocket;
    private final Logger internalLogger;
    private final Set<Socket> clientSockets = new HashSet<>();
    private final ExecutorService clientHandlerService = Executors.newCachedThreadPool();

    TcpRosServerThread(RosNode rosNode, ServerSocket serverSocket, Logger internalLogger) {
        super("TCPROS Server: " + rosNode.getId());
        this.rosNode = rosNode;
        this.serverSocket = serverSocket;
        this.internalLogger = internalLogger;
    }

    private boolean isServerAlive() {
        return !serverSocket.isClosed();
    }

    @Override
    public void run() {
        internalLogger.trace("Server thread initialized.");
        while (isServerAlive()) {
            try {
                internalLogger.trace("Waiting for client connections...");
                Socket clientSocket = serverSocket.accept();

                internalLogger.debug("Connection received from {}; starting monitoring thread...", clientSocket.getInetAddress());
                synchronized (clientSockets) {
                    clientSockets.add(clientSocket);
                }
                clientHandlerService.submit(new ClientHandler(clientSocket));
            } catch (Exception e) {
                if (rosNode.isAlive()) {
                    internalLogger.warn("TCPROS server encountered exception!", e);
                }
            }
        }

        internalLogger.trace("Closing client connections...");
        for (Socket clientSocket : clientSockets) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                internalLogger.warn("Encountered exception while closing client connection!", e);
            }
        }

        internalLogger.trace("Closing client handler thread pool...");
        clientHandlerService.shutdownNow();
        try {
            clientHandlerService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            internalLogger.warn("Interrupted while waiting for TCPROS client handlers to clean up!", e);
        }

        internalLogger.trace("Closing server socket...");
        try {
            serverSocket.close();
        } catch (IOException e) {
            internalLogger.warn("Encountered exception while closing server socket!", e);
        }
    }

    private class ClientHandler implements Runnable {

        private final Socket clientSocket;
        private final AtomicBoolean clientHandlerAlive = new AtomicBoolean(true);

        @Nullable
        private RosId remoteId;
        @Nullable
        private RosId targetId;

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @SuppressWarnings("StatementWithEmptyBody")
        @Override
        public void run() {
            Thread.currentThread().setName("TCPROS Server Client Handler: " + clientSocket.getInetAddress());
            internalLogger.trace("Client handler thread for {} initialized.", clientSocket.getInetAddress());
            try (
                    OutputStream toClient = clientSocket.getOutputStream();
                    InputStream fromClient = clientSocket.getInputStream()
            ) {
                internalLogger.trace("Waiting for connection header...");
                TcpStateMachine stator = new TcpStateMachine(ExpectHeaderDatagram.expectHeader(fields -> {
                    processHeader(fields, toClient);
                    return null;
                }));
                while (isServerAlive() && !clientSocket.isClosed() && clientHandlerAlive.get() && stator.accept(fromClient))
                    ;

                internalLogger.trace("Idling...");
                while (isServerAlive() && !clientSocket.isClosed() && clientHandlerAlive.get() && fromClient.read() != -1)
                    ;
            } catch (Exception e) {
                if (rosNode.isAlive()) {
                    internalLogger.warn("TCPROS client connection errored!", e);
                }
            }

            internalLogger.trace("Cleaning up client connection {}...", clientSocket.getInetAddress());
            try {
                clientSocket.close();
            } catch (IOException e) {
                internalLogger.warn("Encountered exception while closing client connection!", e);
            }
            if (remoteId != null && targetId != null) {
                rosNode.getTransportManager().getBusStateTracker().closeOutgoing(remoteId, targetId);
            }
            rosNode.getTransportManager().notifyPubConnectionKilled(clientSocket);
            synchronized (clientSockets) {
                clientSockets.remove(clientSocket);
            }
        }

        void kill() {
            clientHandlerAlive.set(false);
        }

        private void processHeader(Map<String, String> fields, OutputStream toClient) {
            try {
                internalLogger.trace("Received connection header: {}", fields);
                remoteId = RosId.resolveGlobal(fields.get("callerid"));
                if (fields.containsKey("topic")) {
                    RosId typeId = RosId.resolveGlobal(fields.get("type"));
                    RosMessageType<?> msgType = RosMessageType.get(typeId);
                    if (msgType == null) {
                        throw new NoSuchElementException("Unknown message type: " + typeId);
                    }
                    DataTypeSpecification.Source msgSrc = msgType.getDataType().getTypeSpecification().getSource();
                    if (!msgSrc.getMd5Sum().equals(fields.get("md5sum"))) {
                        throw new IllegalStateException("MD5 checksum mismatch!");
                    }
                    targetId = RosId.resolveGlobal(fields.get("topic"));
                    if (fields.containsKey("tcp_nodelay") && fields.get("tcp_nodelay").equals("1")) {
                        clientSocket.setTcpNoDelay(true);
                    }
                    TcpRosHeader header = new TcpRosHeader();
                    header.putField("callerid", rosNode.getId().toString());
                    header.putField("type", typeId.toString());
                    header.putField("md5sum", msgSrc.getMd5Sum());
                    header.putField("latching", rosNode.getTransportManager().isLatching(targetId) ? "1" : "0");
                    header.writeQuietly(toClient);
                    rosNode.getTransportManager().getBusStateTracker().openOutgoing(remoteId, targetId);
                    rosNode.getTransportManager().registerPubConnection(targetId, clientSocket, toClient);
                    internalLogger.debug("Negotiated topic connection with {} for {} ({}).",
                            remoteId, targetId, typeId.toUnrootedString());
                } else if (fields.containsKey("service")) {
                    throw new UnsupportedOperationException("service"); // TODO services
                } else {
                    throw new IllegalArgumentException("TCPROS client connection did not specify a transport!");
                }
            } catch (Exception e) {
                TcpRosHeader header = new TcpRosHeader();
                header.putField("error", e.toString());
                header.writeQuietly(toClient);
                kill();
            }
        }

    }

}
