package xyz.phanta.rosjay.node;

import xyz.phanta.rosjay.tcpros.client.TcpRosTarget;
import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.srv.RosServiceClient;
import xyz.phanta.rosjay.transport.srv.RosServiceType;
import xyz.phanta.rosjay.util.id.RosId;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class NodeServiceClient<REQ extends RosData<REQ>, RES extends RosData<RES>> extends NodeTransportHandler<RES>
        implements RosServiceClient<REQ, RES> {

    private final RosId serviceId;
    private final RosServiceType<REQ, RES> srvType;
    private final Lock callLock = new ReentrantLock();

    @Nullable
    private CallingThread callThread;

    NodeServiceClient(NodeTransportManager manager, RosId serviceId, RosServiceType<REQ, RES> srvType, int bufferSize) {
        super(manager, bufferSize);
        this.serviceId = serviceId;
        this.srvType = srvType;
    }

    @Override
    public RosId getServiceId() {
        return serviceId;
    }

    @Override
    public RosServiceType<REQ, RES> getSrvType() {
        return srvType;
    }

    @Override
    public RES call(REQ request) throws InterruptedException {
        RosNode node = getManager().getOwningNode();
        callLock.lock();
        try {
            URI serviceUri;
            try {
                serviceUri = node.getRosMaster().lookupService(serviceId);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to look up service provider URI!", e);
            }
            if (serviceUri == null) {
                throw new NoSuchElementException("No service provider could be found for id: " + serviceId);
            }
            callThread = new CallingThread();
            try {
                node.getTcpClientManager().openConnection(
                        new InetSocketAddress(serviceUri.getHost(), serviceUri.getPort()),
                        new TcpRosTarget.Service<>(serviceId, srvType, request));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to connect to service provider!", e);
            }
            try {
                return getDataQueue().pollBlocking().getValue();
            } catch (InterruptedException e) {
                callThread.rethrow();
                throw e;
            }
        } finally {
            callThread = null;
            callLock.unlock();
        }
    }

    boolean interrupt(String errorMessage) {
        return interrupt(new IllegalStateException(errorMessage));
    }

    boolean interrupt(Throwable error) {
        if (callThread != null) {
            callThread.interrupt(error);
            return true;
        }
        return false;
    }

    @Override
    public void kill() {
        interrupt("Shutting down!");
        getManager().notifyServiceClientKilled(this);
        // TODO clean up persistent service client
    }

    private static class CallingThread {

        private final Thread thread = Thread.currentThread();
        @Nullable
        private Throwable interruptReason = null;

        void interrupt(Throwable error) {
            interruptReason = error;
            thread.interrupt();
        }

        void rethrow() {
            if (interruptReason != null) {
                if (interruptReason instanceof RuntimeException) {
                    throw (RuntimeException)interruptReason;
                } else {
                    throw new RuntimeException(interruptReason);
                }
            }
        }

    }

}
