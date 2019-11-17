package xyz.phanta.rosjay.node;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.transport.msg.RosPublisher;
import xyz.phanta.rosjay.util.id.RosId;

class NodePublishHandler<T extends RosData<T>> extends NodeMessageHandler<T> implements RosPublisher<T> {

    private final boolean latch;
    private boolean alive = true;

    NodePublishHandler(NodeTransportManager manager, RosId topicId, RosMessageType<T> msgType, int bufferSize, boolean latch) {
        super(manager, topicId, msgType, bufferSize);
        this.latch = latch;
    }

    boolean isLatching() {
        return latch;
    }

    @Override
    public void publish(T message) {
        if (alive) {
            getDataQueue().offer(message);
        }
    }

    @Override
    public void kill() {
        alive = false;
        getManager().notifyPublicationKilled(this);
    }

}
