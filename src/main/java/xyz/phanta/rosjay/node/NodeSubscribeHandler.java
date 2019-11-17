package xyz.phanta.rosjay.node;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.transport.msg.RosSubscriber;
import xyz.phanta.rosjay.util.RosDataQueue;
import xyz.phanta.rosjay.util.id.RosId;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

class NodeSubscribeHandler<T extends RosData<T>> extends NodeMessageHandler<T> implements RosSubscriber<T> {

    private final Set<Consumer<T>> callbacks = new HashSet<>();

    NodeSubscribeHandler(NodeTransportManager manager, RosId topicId, RosMessageType<T> msgType, int bufferSize) {
        super(manager, topicId, msgType, bufferSize);
    }

    @Override
    public void addCallback(Consumer<T> callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeCallback(Consumer<T> callback) {
        callbacks.remove(callback);
    }

    @Override
    public boolean hasCallbacks() {
        return !callbacks.isEmpty();
    }

    void consumeMessages() {
        RosDataQueue.Entry<T> entry;
        if ((entry = getDataQueue().poll()) != null) {
            T msg = entry.getValue();
            for (Consumer<T> callback : callbacks) {
                callback.accept(msg);
            }
        }
    }

    @Override
    public void kill() {
        getManager().notifySubscriptionKilled(this);
    }

}
