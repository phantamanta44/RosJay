package xyz.phanta.rosjay.node;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageTransport;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.util.id.RosId;

abstract class NodeMessageHandler<T extends RosData<T>> extends NodeTransportHandler<T> implements RosMessageTransport<T> {

    private final RosId topicId;
    private final RosMessageType<T> msgType;

    NodeMessageHandler(NodeTransportManager manager, RosId topicId, RosMessageType<T> msgType, int bufferSize) {
        super(manager, bufferSize);
        this.topicId = topicId;
        this.msgType = msgType;
    }

    @Override
    public RosId getTopicId() {
        return topicId;
    }

    @Override
    public RosMessageType<T> getMsgType() {
        return msgType;
    }

}
