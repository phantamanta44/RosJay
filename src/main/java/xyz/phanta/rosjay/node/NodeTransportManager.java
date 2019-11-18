package xyz.phanta.rosjay.node;

import org.slf4j.Logger;
import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.transport.msg.RosPublisher;
import xyz.phanta.rosjay.transport.msg.RosSubscriber;
import xyz.phanta.rosjay.transport.srv.RosServiceClient;
import xyz.phanta.rosjay.transport.srv.RosServiceType;
import xyz.phanta.rosjay.util.BusStateTracker;
import xyz.phanta.rosjay.util.RosDataQueue;
import xyz.phanta.rosjay.util.RosUtils;
import xyz.phanta.rosjay.util.id.NamespacedMap;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.lowdata.LEDataOutputStream;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NodeTransportManager {

    private final RosNode owner;
    private final Logger internalLogger;
    private final BusStateTracker busStates = new BusStateTracker();

    // publishers
    private final NamespacedMap<NodePublishHandler<?>> pubs = new NamespacedMap<>();
    private final NamespacedMap<Map<Socket, DataOutput>> pubConnections = new NamespacedMap<>();

    // subscribers
    private final NamespacedMap<NodeSubscribeHandler<?>> subs = NamespacedMap.concurrent();

    // service clients
    private final NamespacedMap<NodeServiceClient<?, ?>> srvClients = NamespacedMap.concurrent();

    // service servers
    // TODO service servers

    private boolean alive = true;

    NodeTransportManager(RosNode owner) {
        this.owner = owner;
        this.internalLogger = owner.getChildInternalLogger("trans");
    }

    RosNode getOwningNode() {
        return owner;
    }

    public BusStateTracker getBusStateTracker() {
        return busStates;
    }

    Set<Map.Entry<RosId, NodePublishHandler<?>>> getPubEntries() {
        return pubs.entrySet();
    }

    Set<Map.Entry<RosId, NodeSubscribeHandler<?>>> getSubEntries() {
        return subs.entrySet();
    }

    <T extends RosData<T>> RosPublisher<T> resolvePub(RosId topicId, RosMessageType<T> msgType, int bufferSize,
                                                      boolean latch) throws IOException {
        //noinspection unchecked
        NodePublishHandler<T> pub = (NodePublishHandler<T>)pubs.get(topicId);
        if (pub != null) {
            return pub;
        }

        internalLogger.debug("Creating pub to {} ({}) with bufSize={}, latch={}...", topicId, msgType, bufferSize, latch);
        pub = new NodePublishHandler<>(this, topicId, msgType, bufferSize, latch);
        pubs.put(topicId, pub);
        owner.getRosMaster().registerPublisher(pub);
        return pub;
    }

    @Nullable
    RosPublisher<?> getPub(RosId topicId) {
        return pubs.get(topicId);
    }

    <T extends RosData<T>> RosSubscriber<T> resolveSub(RosId topicId, RosMessageType<T> msgType, int bufferSize)
            throws IOException {
        //noinspection unchecked
        NodeSubscribeHandler<T> sub = (NodeSubscribeHandler<T>)subs.get(topicId);
        if (sub != null) {
            return sub;
        }

        internalLogger.debug("Creating sub to {} ({}) with bufSize={}...", topicId, msgType, bufferSize);
        sub = new NodeSubscribeHandler<>(this, topicId, msgType, bufferSize);
        subs.put(topicId, sub);
        owner.scheduleRpcTask(new OpenSubscriberTask(
                owner, topicId, msgType, owner.getRosMaster().registerSubscriber(sub)));
        return sub;
    }

    @Nullable
    RosSubscriber<?> getSub(RosId topicId) {
        return subs.get(topicId);
    }

    <REQ extends RosData<REQ>, RES extends RosData<RES>> RosServiceClient<REQ, RES> resolveSrvClient(RosId serviceId,
                                                                                                     RosServiceType<REQ, RES> srvType) {
        //noinspection unchecked
        NodeServiceClient<REQ, RES> client = (NodeServiceClient<REQ, RES>)srvClients.get(serviceId);
        if (client != null) {
            return client;
        }

        internalLogger.debug("Creating srv client for {} ({})...", serviceId, srvType);
        client = new NodeServiceClient<>(this, serviceId, srvType, 4);
        srvClients.put(serviceId, client);
        return client;
    }

    void notifyPublicationKilled(NodePublishHandler<?> publisher) {
        internalLogger.debug("Cleaning up pub to {} ({})...", publisher.getTopicId(), publisher.getMsgType());
        if (alive) {
            pubs.remove(publisher.getTopicId());
        }
        try {
            owner.getRosMaster().unregisterPublisher(publisher.getTopicId());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to clean up publisher!", e);
        }
    }

    void notifySubscriptionKilled(NodeSubscribeHandler<?> subscriber) {
        internalLogger.debug("Cleaning up sub to {} ({})...", subscriber.getTopicId(), subscriber.getMsgType());
        if (alive) {
            subs.remove(subscriber.getTopicId());
        }
        try {
            owner.getRosMaster().unregisterSubscriber(subscriber.getTopicId());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to clean up subscriber!", e);
        }
    }

    void notifyServiceClientKilled(NodeServiceClient<?, ?> client) {
        internalLogger.debug("Cleaning up service client for {} ({})...", client.getServiceId(), client.getSrvType());
        if (alive) {
            srvClients.remove(client.getServiceId());
        }
    }

    public boolean isLatching(RosId topicId) {
        NodePublishHandler<?> pub = pubs.get(topicId);
        return pub != null && pub.isLatching();
    }

    public void registerPubConnection(RosId topicId, Socket clientSocket, OutputStream toClient) {
        synchronized (pubConnections) {
            pubConnections.computeIfAbsent(topicId, HashMap::new).put(clientSocket, new LEDataOutputStream(toClient));
        }
    }

    public void notifyPubConnectionKilled(Socket clientSocket) {
        synchronized (pubConnections) {
            for (Map.Entry<RosId, Map<Socket, DataOutput>> topicEntry : pubConnections.entrySet()) {
                topicEntry.getValue().remove(clientSocket);
            }
        }
    }

    public void notifyReceivedMessage(RosId topicId, RosData<?> msg) {
        NodeSubscribeHandler<?> sub = subs.get(topicId);
        if (sub != null) {
            //noinspection unchecked
            ((RosDataQueue)sub.getDataQueue()).offer(msg);
        }
    }

    public void notifyReceivedServiceResponse(RosId serviceId, RosData<?> res) {
        NodeServiceClient<?, ?> client = srvClients.get(serviceId);
        if (client != null) {
            internalLogger.trace("Received service response for {}", serviceId);
            //noinspection unchecked
            ((RosDataQueue)client.getDataQueue()).offer(res);
        } else {
            internalLogger.warn("Ignoring anomalous service response for {}: {}", serviceId, res);
        }
    }

    public void notifyServiceConnectionKilled(RosId serviceId, String errorMessage) {
        NodeServiceClient<?, ?> client = srvClients.get(serviceId);
        if (client != null && client.interrupt(errorMessage)) {
            internalLogger.debug("Interrupted service client call for {}: {}", serviceId, errorMessage);
        }
    }

    void tick() throws IOException {
        for (Map.Entry<RosId, NodePublishHandler<?>> pub : pubs.entrySet()) {
            RosDataQueue<?> pubQueue = pub.getValue().getDataQueue();
            RosDataQueue.Entry<?> entry;
            while ((entry = pubQueue.poll()) != null) {
                RosData<?> msg = entry.getValue();
                synchronized (pubConnections) {
                    Map<Socket, DataOutput> connections = pubConnections.get(pub.getKey());
                    if (connections != null) {
                        byte[] msgData = RosUtils.serializeDataPacket(entry.getValue(), entry.getSeqIndex());
                        for (DataOutput stream : connections.values()) {
                            stream.writeInt(msgData.length);
                            stream.write(msgData);
                        }
                    }
                }
            }
        }
        for (Map.Entry<RosId, NodeSubscribeHandler<?>> sub : subs.entrySet()) {
            sub.getValue().consumeMessages();
        }
        // TODO tick service providers
    }

    void kill() {
        alive = false;

        internalLogger.debug("Cleaning up publications...");
        for (Map.Entry<RosId, NodePublishHandler<?>> pub : pubs.entrySet()) {
            try {
                pub.getValue().kill();
            } catch (Exception e) {
                internalLogger.warn("Failed to clean up publication!", e);
            }
        }

        internalLogger.debug("Cleaning up subscriptions...");
        for (Map.Entry<RosId, NodeSubscribeHandler<?>> sub : subs.entrySet()) {
            try {
                sub.getValue().kill();
            } catch (Exception e) {
                internalLogger.warn("Failed to clean up subscription!", e);
            }
        }

        internalLogger.debug("Cleaning up service clients...");
        for (Map.Entry<RosId, NodeServiceClient<?, ?>> client : srvClients.entrySet()) {
            try {
                client.getValue().kill();
            } catch (Exception e) {
                internalLogger.warn("Failed to clean up service client!", e);
            }
        }
    }

}
