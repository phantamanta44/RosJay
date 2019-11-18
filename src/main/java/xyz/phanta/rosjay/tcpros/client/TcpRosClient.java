package xyz.phanta.rosjay.tcpros.client;

import org.slf4j.Logger;
import xyz.phanta.rosjay.node.RosNode;
import xyz.phanta.rosjay.tcpros.TcpRosHeader;
import xyz.phanta.rosjay.tcpros.stator.ExpectHeaderDatagram;
import xyz.phanta.rosjay.tcpros.stator.TcpStateMachine;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.transport.spec.DataTypeSpecification;
import xyz.phanta.rosjay.transport.srv.RosServiceType;
import xyz.phanta.rosjay.util.RosUtils;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.lowdata.LEDataOutputStream;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpRosClient {

    private final TcpRosClientManager manager;
    private final RosNode rosNode;
    private final SocketAddress address;
    private final TcpRosTarget target;
    private final Logger internalLogger;

    @Nullable
    private ClientInstance instance;

    public TcpRosClient(TcpRosClientManager manager, SocketAddress address, TcpRosTarget target) {
        this.manager = manager;
        this.rosNode = manager.getRosNode();
        this.address = address;
        this.target = target;
        this.internalLogger = rosNode.getChildInternalLogger("tcpros_client");
    }

    SocketAddress getAddress() {
        return address;
    }

    public void connect() throws IOException {
        if (instance != null) {
            throw new IllegalStateException("Client is already running!");
        }

        internalLogger.trace("Establishing socket connection to {}...", address);
        Socket socket = new Socket();
        socket.connect(address);

        internalLogger.trace("Starting socket monitoring thread...");
        instance = new ClientInstance(socket);
        instance.start();
    }

    public void kill() throws IOException {
        if (instance == null) {
            throw new IllegalStateException("Client is not running!");
        }
        instance.kill();
        try {
            internalLogger.debug("Waiting for client monitor thread to terminate...");
            instance.join();
        } catch (InterruptedException e) {
            internalLogger.error("Interrupted while waiting for TCPROS client to clean up!", e);
        }
        instance = null;
    }

    private class ClientInstance extends Thread {

        private final Socket socket;
        private final AtomicBoolean active = new AtomicBoolean(true);

        @Nullable
        private RosId remoteId;

        ClientInstance(Socket socket) {
            super("TCPROS Client: " + socket.getInetAddress());
            this.socket = socket;
        }

        @Override
        public void run() {
            internalLogger.trace("Client monitor thread for {} initialized.", socket.getInetAddress());
            try (
                    OutputStream toServer = socket.getOutputStream();
                    InputStream fromServer = socket.getInputStream()
            ) {
                internalLogger.trace("Writing connection header...");
                TcpRosHeader header = new TcpRosHeader();
                header.putField("callerid", rosNode.getId().toString());
                target.populateHeader(header);
                header.writeQuietly(toServer);

                internalLogger.trace("Beginning monitoring loop...");
                TcpStateMachine stator = new TcpStateMachine(ExpectHeaderDatagram.expectHeader(
                        fields -> processHeader(fields, toServer)));
                //noinspection StatementWithEmptyBody
                while (active.get() && stator.accept(fromServer)) ;
            } catch (Exception e) {
                if (rosNode.isAlive()) {
                    internalLogger.warn("TCPROS client encountered exception!", e);
                }
            }

            internalLogger.trace("Cleaning up TCPROS client {}...", socket.getInetAddress());
            try {
                socket.close();
            } catch (IOException e) {
                internalLogger.warn("Encountered exception while closing socket!", e);
            }
            target.onConnectionClosed(rosNode);
            if (remoteId != null) {
                rosNode.getTransportManager().getBusStateTracker().closeIncoming(remoteId, target.getId());
            }
            manager.notifyClientKilled(TcpRosClient.this);
        }

        @Nullable
        private TcpStateMachine.State processHeader(Map<String, String> fields, OutputStream toServer) {
            try {
                remoteId = RosId.resolveGlobal(fields.get("callerid"));
                if (target instanceof TcpRosTarget.Topic) {
                    RosMessageType<?> msgType = ((TcpRosTarget.Topic)target).getMessageType();
                    DataTypeSpecification.Source msgSrc = msgType.getDataType().getTypeSpecification().getSource();
                    if (!msgSrc.getMd5Sum().equals(fields.get("md5sum"))) {
                        throw new IllegalStateException("MD5 checksum mismatch!");
                    }
                    rosNode.getTransportManager().getBusStateTracker().openIncoming(remoteId, target.getId());
                    internalLogger.debug("Negotiated topic connection with {} for {}.", remoteId, target);
                    return ExpectDeserializerChain.expect(msgType.getDataType(),
                            msg -> rosNode.getTransportManager().notifyReceivedMessage(target.getId(), msg));
                } else if (target instanceof TcpRosTarget.Service) {
                    // TODO persistent service client support
                    RosServiceType<?, ?> srvType = ((TcpRosTarget.Service)target).getServiceType();
                    DataTypeSpecification.Source srvSrc = srvType.getRequestType().getTypeSpecification().getSource();
                    if (!srvSrc.getMd5Sum().equals(fields.get("md5sum"))) {
                        throw new IllegalStateException("MD5 checksum mismatch!");
                    }
                    internalLogger.debug("Negotiated service connection with {} for {}.", remoteId, target);
                    rosNode.getTransportManager().getBusStateTracker().openIncoming(remoteId, target.getId());
                    byte[] reqData = RosUtils.serializeDataPacket(((TcpRosTarget.Service)target).getRequestData(), 0);
                    LEDataOutputStream dataOutput = new LEDataOutputStream(toServer);
                    dataOutput.writeInt(reqData.length);
                    dataOutput.write(reqData);
                    return new ExpectServiceOkayByte(rosNode.getTransportManager(), target.getId(), srvType);
                }
            } catch (Exception e) {
                TcpRosHeader header = new TcpRosHeader();
                header.putField("error", e.toString());
                header.writeQuietly(toServer);
                markShouldDie();
            }
            return null;
        }

        private void markShouldDie() {
            active.set(false);
        }

        void kill() throws IOException {
            internalLogger.trace("Closing socket...");
            socket.close();

            try {
                internalLogger.debug("Waiting for monitoring thread to terminate...");
                join();
            } catch (InterruptedException e) {
                internalLogger.error("Interrupted while waiting for TCPROS client to clean up!", e);
            }
        }

    }

}
