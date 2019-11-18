package xyz.phanta.rosjay.node;

import xyz.phanta.rosjay.transport.RosTransport;
import xyz.phanta.rosjay.util.RosDataQueue;

abstract class NodeTransportHandler<T> implements RosTransport {

    private final NodeTransportManager manager;
    private final RosDataQueue<T> dataQueue;

    NodeTransportHandler(NodeTransportManager manager, int bufferSize) {
        this.manager = manager;
        this.dataQueue = new RosDataQueue<>(bufferSize);
    }

    NodeTransportManager getManager() {
        return manager;
    }

    RosDataQueue<T> getDataQueue() {
        return dataQueue;
    }

}
