package xyz.phanta.rosjay.transport.msg;

import xyz.phanta.rosjay.transport.data.RosData;

public interface RosPublisher<T extends RosData<T>> extends RosMessageTransport<T> {

    void publish(T message);

}
