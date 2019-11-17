package xyz.phanta.rosjay.transport.msg;

import xyz.phanta.rosjay.transport.RosTransport;
import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.util.id.RosId;

public interface RosMessageTransport<T extends RosData<T>> extends RosTransport {

    RosId getTopicId();

    RosMessageType<T> getMsgType();

}
