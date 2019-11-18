package xyz.phanta.rosjay.tcpros.server;

import org.slf4j.Logger;
import xyz.phanta.rosjay.node.RosNode;
import xyz.phanta.rosjay.tcpros.TcpRosHeader;
import xyz.phanta.rosjay.tcpros.stator.ExpectDeserializerChain;
import xyz.phanta.rosjay.tcpros.stator.ExpectHeaderDatagram;
import xyz.phanta.rosjay.tcpros.stator.TcpStateMachine;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.transport.spec.DataTypeSpecification;
import xyz.phanta.rosjay.transport.srv.RosServiceType;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.lowdata.LEDataOutputStream;

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

        private boolean isClientHandlerAlive() {
            return isServerAlive() && !clientSocket.isClosed() && clientHandlerAlive.get();
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
                TcpStateMachine stator = new TcpStateMachine(ExpectHeaderDatagram.expectHeader(
                        fields -> processHeader(fields, toClient)));
                while (isClientHandlerAlive() && stator.accept(fromClient)) ;

                if (isClientHandlerAlive()) {
                    internalLogger.trace("Idling...");
                    while (isClientHandlerAlive() && fromClient.read() != -1) ;
                }
            } catch (Exception e) {
                if (rosNode.isAlive()) {
                    internalLogger.warn("TCPROS client connection errored!", e);
                }
            }

            if (targetId != null) {
                internalLogger.trace("Cleaning up client connection {} for {}...", clientSocket.getInetAddress(), targetId);
            } else {
                internalLogger.trace("Cleaning up client connection {}...", clientSocket.getInetAddress());
            }
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

        @Nullable
        private TcpStateMachine.State processHeader(Map<String, String> fields, OutputStream toClient) {
            try {
                internalLogger.trace("Received connection header: {}", fields);
                remoteId = RosId.resolveGlobal(fields.get("callerid"));
                if (fields.containsKey("topic")) {
                    RosId typeId = RosId.resolveGlobal(fields.get("type"));
                    RosMessageType<?> msgType = RosMessageType.get(typeId);
                    if (msgType == null) {
                        internalLogger.warn("Connection requested topic for unknown message type {}!", typeId);
                        throw new NoSuchElementException("Unknown message type: " + typeId);
                    }
                    DataTypeSpecification.Source msgSrc = msgType.getDataType().getTypeSpecification().getSource();
                    if (!msgSrc.getMd5Sum().equals(fields.get("md5sum"))) {
                        internalLogger.warn("Connection requested topic for message type {} with bad MD5!", typeId);
                        internalLogger.warn("Expected {} but got {}!", msgSrc.getMd5Sum(), fields.get("md5sum"));
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
                    if (fields.containsKey("persistent") && fields.get("persistent").equals("1")) {
                        // TODO persistent service server connections
                        throw new UnsupportedOperationException("Persistent connections are not supported!");
                    }
                    targetId = RosId.resolveGlobal(fields.get("service"));
                    RosServiceType<?, ?> srvType = rosNode.getTransportManager().getServiceType(targetId);
                    if (srvType == null) {
                        internalLogger.warn("Connection requested unadvertised service {}!", targetId);
                        throw new NoSuchElementException("Service is not being advertised!");
                    }
                    if (fields.containsKey("probe") && fields.get("probe").equals("1")) {
                        TcpRosHeader header = new TcpRosHeader();
                        header.putField("callerid", rosNode.getId().toString());
                        header.putField("type", srvType.getId().toUnrootedString());
                        header.writeQuietly(toClient);
                        internalLogger.debug("Handled service probe connection with {} for {} ({}).",
                                remoteId, targetId, srvType.getId().toUnrootedString());
                        kill();
                    } else {
                        DataTypeSpecification.Source srvSrc = srvType.getRequestType().getTypeSpecification().getSource();
                        if (!srvSrc.getMd5Sum().equals(fields.get("md5sum"))) {
                            internalLogger.warn("Connection requested service for type {} with bad MD5!", srvType);
                            internalLogger.warn("Expected {} but got {}!", srvSrc.getMd5Sum(), fields.get("md5sum"));
                            throw new IllegalStateException("MD5 checksum mismatch!");
                        }
                        TcpRosHeader header = new TcpRosHeader();
                        header.putField("callerid", rosNode.getId().toString());
                        header.putField("type", srvType.getId().toUnrootedString());
                        header.putField("md5sum", srvSrc.getMd5Sum());
                        header.writeQuietly(toClient);
                        rosNode.getTransportManager().getBusStateTracker().openOutgoing(remoteId, targetId);
                        internalLogger.debug("Negotiated service connection with {} for {} ({}).",
                                remoteId, targetId, srvType.getId().toUnrootedString());
                        return ExpectDeserializerChain.expect(srvType.getRequestType(),
                                req -> rosNode.getTransportManager().queueServiceRequest(targetId, req, new LEDataOutputStream(toClient)));
                    }
                } else {
                    internalLogger.warn("Connection did not specify a valid transport type!");
                    throw new IllegalArgumentException("TCPROS client connection did not specify a transport!");
                }
            } catch (Exception e) {
                TcpRosHeader header = new TcpRosHeader();
                header.putField("error", e.toString());
                header.writeQuietly(toClient);
                kill();
            }
            return null;
        }

    }

}
