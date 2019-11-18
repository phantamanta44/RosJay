package xyz.phanta.rosjay.node;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.srv.RosServiceProvider;
import xyz.phanta.rosjay.transport.srv.RosServiceType;
import xyz.phanta.rosjay.util.RosDataQueue;
import xyz.phanta.rosjay.util.RosUtils;
import xyz.phanta.rosjay.util.id.RosId;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

class NodeServiceServer<REQ extends RosData<REQ>, RES extends RosData<RES>>
        extends NodeTransportHandler<NodeServiceServer.Request<REQ>> implements RosServiceProvider<REQ, RES> {

    private final RosId serviceId;
    private final RosServiceType<REQ, RES> srvType;
    @Nullable
    private Function<REQ, RES> handler;

    NodeServiceServer(NodeTransportManager manager, RosId serviceId, RosServiceType<REQ, RES> srvType, int bufferSize) {
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
    public void setHandler(@Nullable Function<REQ, RES> handler) {
        this.handler = handler;
    }

    @Override
    public boolean hasHandler() {
        return handler != null;
    }

    void processRequests() throws IOException {
        RosDataQueue.Entry<Request<REQ>> entry;
        if ((entry = getDataQueue().poll()) != null) {
            Request<REQ> req = entry.getValue();
            if (handler != null) {
                try {
                    sendResponseSuccess(handler.apply(req.request), req.requester);
                } catch (Exception e) {
                    sendResponseFailure(e.toString(), req.requester);
                }
            } else {
                sendResponseFailure("No handler!", req.requester);
            }
        }
    }

    @Override
    public void kill() {
        getManager().notifyServiceServerKilled(this);
    }

    private static void sendResponseSuccess(RosData<?> data, DataOutput dest) throws IOException {
        byte[] dataBytes = RosUtils.serializeDataPacket(data, 0);
        dest.write(1);
        dest.writeInt(dataBytes.length);
        dest.write(dataBytes);
    }

    private static void sendResponseFailure(String errorMessage, DataOutput dest) throws IOException {
        byte[] msgBytes = errorMessage.getBytes(StandardCharsets.US_ASCII);
        dest.write(0);
        dest.write(msgBytes.length);
        dest.write(msgBytes);
    }

    static class Request<REQ extends RosData<REQ>> {

        final REQ request;
        final DataOutput requester;

        Request(REQ request, DataOutput requester) {
            this.request = request;
            this.requester = requester;
        }

    }

}
