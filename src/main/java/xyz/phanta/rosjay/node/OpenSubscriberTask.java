package xyz.phanta.rosjay.node;

import org.slf4j.Logger;
import xyz.phanta.rosjay.rpc.RosRpcNode;
import xyz.phanta.rosjay.tcpros.client.TcpRosTarget;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.util.id.RosId;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

public class OpenSubscriberTask implements Runnable {

    private final RosNode rosNode;
    private final Logger internalLogger;
    private final RosId topicId;
    private final RosMessageType<?> msgType;
    private final List<URI> uris;

    public OpenSubscriberTask(RosNode rosNode, RosId topicId, RosMessageType<?> msgType, List<URI> uris) {
        this.rosNode = rosNode;
        this.internalLogger = rosNode.getChildInternalLogger("task_clicxn");
        this.topicId = topicId;
        this.msgType = msgType;
        this.uris = uris;
    }

    @Override
    public void run() {
        for (URI uri : uris) {
            try {
                RosRpcNode nodeRpc = new RosRpcNode(rosNode, uri);
                InetSocketAddress tcpAddr = nodeRpc.requestTopic(topicId);
                if (tcpAddr != null) {
                    if (!rosNode.getTcpClientManager().isConnectionOpen(tcpAddr)) {
                        rosNode.getTcpClientManager().openConnection(tcpAddr, new TcpRosTarget.Topic<>(topicId, msgType));
                    }
                } else {
                    throw new NoSuchElementException("No valid ROS transport protocols found!");
                }
            } catch (Exception e) {
                internalLogger.warn("Failed to open client connection to " + uri + "!", e);
            }
        }
    }

}
