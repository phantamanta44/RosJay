package xyz.phanta.rosjay.transport.msg;

import xyz.phanta.rosjay.transport.data.RosData;

import java.util.function.Consumer;

public interface RosSubscriber<T extends RosData<T>> extends RosMessageTransport<T> {

    void addCallback(Consumer<T> callback);

    void removeCallback(Consumer<T> callback);

    boolean hasCallbacks();

}
