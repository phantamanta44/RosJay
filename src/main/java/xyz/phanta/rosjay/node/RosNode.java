package xyz.phanta.rosjay.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.phanta.jxmlrpc.XmlRpcRoutine;
import xyz.phanta.jxmlrpc.XmlRpcServer;
import xyz.phanta.jxmlrpc.data.XmlRpcArray;
import xyz.phanta.jxmlrpc.data.XmlRpcData;
import xyz.phanta.jxmlrpc.data.XmlRpcInt;
import xyz.phanta.jxmlrpc.data.XmlRpcString;
import xyz.phanta.rosjay.rospkg.rosgraph_msgs.Log;
import xyz.phanta.rosjay.rpc.RosRpcMaster;
import xyz.phanta.rosjay.tcpros.client.TcpRosClientManager;
import xyz.phanta.rosjay.tcpros.server.TcpRosServer;
import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.transport.msg.RosPublisher;
import xyz.phanta.rosjay.transport.msg.RosSubscriber;
import xyz.phanta.rosjay.util.RosRate;
import xyz.phanta.rosjay.util.RosUtils;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.id.RosNamespace;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RosNode {

    private static final Logger GLOBAL_LOGGER = RosUtils.getGlobalInternalLogger("node");

    private static final HashSet<RosNode> aliveNodes = new HashSet<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            GLOBAL_LOGGER.debug("Shutdown hook: cleaning up remaining live nodes...");
            synchronized (aliveNodes) {
                for (RosNode node : aliveNodes) {
                    try {
                        GLOBAL_LOGGER.debug("Cleaning up node {}...", node.nodeId);
                        node.kill();
                    } catch (Exception e) {
                        GLOBAL_LOGGER.warn("Encountered exception while cleaning up node!", e);
                    }
                }
            }
        }, "ROS Shutdown Hook"));
    }

    private final RosId nodeId;
    private final RosNamespace privateNs;
    private final RosRpcMaster rosMaster;
    private final String localIp;
    private final Logger internalLogger;
    private final NodeTransportManager transportManager;
    private final ParameterManager paramManager;

    // rpc server
    private final RpcController rpcController = new RpcController();
    private final ExecutorService rpcTaskScheduler = Executors.newSingleThreadExecutor();
    private final XmlRpcServer rpcServer = new XmlRpcServer(rpcController);
    @Nullable
    private URI rpcServerUri = null;

    // tcp server
    private final TcpRosServer tcpServer;
    @Nullable
    private URI tcpServerUri = null;

    // tcp clients
    private final TcpRosClientManager tcpClient;

    // other state
    private boolean initializedAlready = false;
    private boolean alive = false;
    @Nullable
    private RosLogger logger = null;

    public RosNode(String nodeId) {
        this(nodeId, System.getenv("ROS_MASTER_URI"), System.getenv("ROS_IP"));
    }

    public RosNode(String nodeId, String masterUri, String localIp) {
        this(RosId.resolveGlobal(nodeId), URI.create(masterUri), localIp);
    }

    public RosNode(RosId nodeId, URI masterUri, String localIp) {
        this.nodeId = nodeId; // TODO remapping?
        this.privateNs = nodeId.getNamespace().resolveNamespace(nodeId.getName());
        this.rosMaster = new RosRpcMaster(this, masterUri);
        this.localIp = localIp;
        this.internalLogger = LoggerFactory.getLogger(GLOBAL_LOGGER.getName() + "." + nodeId.toUnrootedString().replace('/', '.'));
        this.transportManager = new NodeTransportManager(this);
        this.paramManager = new ParameterManager(this);
        this.tcpServer = new TcpRosServer(this);
        this.tcpClient = new TcpRosClientManager(this);
    }

    public RosId getId() {
        return nodeId;
    }

    public RosNamespace getNamespace() {
        return nodeId.getNamespace();
    }

    public RosNamespace getPrivateNamespace() {
        return privateNs;
    }

    public RosId resolveRelativeId(String name) {
        return name.startsWith("~") ? privateNs.resolveId(name.substring(1)) : nodeId.getNamespace().resolveId(name);
    }

    RosRpcMaster getRosMaster() {
        return rosMaster;
    }

    public NodeTransportManager getTransportManager() {
        return transportManager;
    }

    public Logger getChildInternalLogger(String path) {
        return LoggerFactory.getLogger(internalLogger.getName() + "." + path);
    }

    public boolean isAlive() {
        return alive;
    }

    public void init() {
        if (initializedAlready) {
            throw new IllegalStateException("ROS nodes can only be initialized once!");
        }
        initializedAlready = true;
        // TODO sanity check environment to make sure ROS is ready to run
        try {
            internalLogger.debug("Initializing XMLRPC server...");
            rpcServer.serve(new InetSocketAddress(0), rpcTaskScheduler);
            rpcServerUri = RosUtils.buildAddressUri(localIp, rpcServer.getServerAddress().getPort());
            internalLogger.debug("XMLRPC server running at {}", rpcServerUri);

            internalLogger.debug("Initializing TCPROS server...");
            tcpServer.serve(0);
            tcpServerUri = RosUtils.buildAddressUri(localIp, tcpServer.getActivePort());
            internalLogger.debug("TCPROS server running at {}", tcpServerUri);

            alive = true;

            internalLogger.debug("Creating /rosout publisher...");
            logger = new RosLogger(this, advertise("/rosout", Log.TYPE, 100, true));

            internalLogger.debug("Adding to live node set...");
            synchronized (aliveNodes) {
                aliveNodes.add(this);
            }

            internalLogger.debug("Initialization complete.");
        } catch (Throwable e) {
            internalLogger.error("Initialization failed! Cleaning up...");
            alive = false;
            killSockets();
            throw new IllegalStateException("ROS node server initialization failed!", e);
        }
    }

    public URI getRpcServerUri() {
        if (rpcServerUri == null) {
            throw new IllegalStateException("Cannot get RPC URI before initializing node!");
        }
        return rpcServerUri;
    }

    public URI getTcpServerUri() {
        if (tcpServerUri == null) {
            throw new IllegalStateException("Cannot get ROSTCP URI before initializing node!");
        }
        return tcpServerUri;
    }

    public RosLogger getLogger() {
        if (logger == null) {
            throw new IllegalStateException("Cannot get logger before initializing node!");
        }
        return logger;
    }

    void scheduleRpcTask(Runnable task) {
        rpcTaskScheduler.submit(task);
    }

    TcpRosClientManager getTcpClientManager() {
        return tcpClient;
    }

    public <T extends RosData<T>> RosPublisher<T> advertise(String topicName, RosMessageType<T> msgType, int bufferSize) {
        return advertise(topicName, msgType, bufferSize, false);
    }

    public <T extends RosData<T>> RosPublisher<T> advertise(String topicName, RosMessageType<T> msgType, int bufferSize,
                                                            boolean latch) {
        try {
            return transportManager.resolvePub(resolveRelativeId(topicName), msgType, bufferSize, latch);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve publication!", e);
        }
    }

    public <T extends RosData<T>> RosSubscriber<T> subscribe(String topicName, RosMessageType<T> msgType, int bufferSize,
                                                             Consumer<T> callback) {
        RosSubscriber<T> sub = subscribe(topicName, msgType, bufferSize);
        sub.addCallback(callback);
        return sub;
    }

    public <T extends RosData<T>> RosSubscriber<T> subscribe(String topicName, RosMessageType<T> msgType, int bufferSize) {
        try {
            return transportManager.resolveSub(resolveRelativeId(topicName), msgType, bufferSize);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve subscription!", e);
        }
    }

    public ParameterManager getParameters() {
        return paramManager;
    }

    public void spinOnce() {
        if (alive) {
            try {
                transportManager.tick();
            } catch (IOException e) {
                internalLogger.warn("Encountered exception while ticking transports!", e);
            }
        }
    }

    public void spin() {
        RosRate rate = new RosRate(30D);
        while (isAlive()) {
            spinOnce();
            rate.sleep();
        }
    }

    public void kill() {
        if (!alive) {
            throw new IllegalStateException("Node is not running!");
        }
        alive = false;

        internalLogger.debug("Removing from live node set...");
        synchronized (aliveNodes) {
            aliveNodes.remove(this);
        }

        internalLogger.debug("Cleaning up transport manager...");
        transportManager.kill();

        internalLogger.debug("Cleaning up parameter manager...");
        paramManager.kill();

        killSockets();
        internalLogger.debug("Termination complete.");
    }

    private void killSockets() {
        rpcServerUri = null;
        tcpServerUri = null;
        try {
            internalLogger.debug("Cleaning up XMLRPC server...");
            rpcServer.kill();
        } catch (Exception e) {
            internalLogger.warn("Encountered exception while cleaning up XMLRPC server!", e);
        }

        internalLogger.debug("Closing XMLRPC task queue...");
        rpcTaskScheduler.shutdown();

        try {
            internalLogger.debug("Cleaning up TCPROS server...");
            tcpServer.kill();
        } catch (Exception e) {
            internalLogger.warn("Encountered exception while cleaning up TCPROS server!", e);
        }

        try {
            internalLogger.debug("Cleaning up TCPROS clients...");
            tcpClient.kill();
        } catch (Exception e) {
            internalLogger.warn("Encountered exception while cleaning up TCPROS clients!", e);
        }
    }

    private class RpcController {

        @XmlRpcRoutine
        public XmlRpcArray<?> getBusStats(XmlRpcString callerId) {
            internalLogger.trace("getBusStats({})", callerId);
            throw new UnsupportedOperationException("getBusStats"); // TODO getBusStats
        }

        @XmlRpcRoutine
        public XmlRpcArray<?> getBusInfo(XmlRpcString callerId) {
            internalLogger.trace("getBusInfo({})", callerId);
            Map<RosId, URI> peerUriCache = new HashMap<>();
            return RosUtils.buildRpcResult(transportManager.getBusStateTracker().collectStates().stream()
                    .map(state -> XmlRpcArray.of(
                            new XmlRpcString(""),
                            new XmlRpcString(peerUriCache.computeIfAbsent(state.peerId, k -> {
                                try {
                                    return rosMaster.lookupNode(k);
                                } catch (IOException e) {
                                    throw new IllegalStateException("Could not look up node RPC URI: " + k, e);
                                }
                            })),
                            new XmlRpcString(state.state.symbol),
                            new XmlRpcString("TCPROS"),
                            new XmlRpcString(state.targetId)))
                    .collect(XmlRpcArray.collect()));
        }

        @XmlRpcRoutine
        public XmlRpcArray<?> getMasterUri(XmlRpcString callerId) {
            internalLogger.trace("getMasterUri({})", callerId);
            return RosUtils.buildRpcResult(new XmlRpcString(rosMaster.getRpcUri().toString()));
        }

        @XmlRpcRoutine
        public XmlRpcArray<?> shutdown(XmlRpcString callerId, XmlRpcString reason) {
            internalLogger.trace("shutdown({}, {})", callerId, reason);
            try {
                rpcTaskScheduler.submit(RosNode.this::kill);
            } catch (Exception e) {
                throw new IllegalStateException("Shutdown request failed!", e);
            }
            return RosUtils.buildRpcResult(XmlRpcInt.ZERO);
        }

        @XmlRpcRoutine
        public XmlRpcArray<?> getPid(XmlRpcString callerId) {
            internalLogger.trace("getPid({})", callerId);
            int pid = -1;
            String procName = ManagementFactory.getRuntimeMXBean().getName();
            int splitIndex = procName.indexOf('@');
            if (splitIndex != -1) {
                pid = Integer.parseInt(procName.substring(0, splitIndex));
            }
            return RosUtils.buildRpcResult(new XmlRpcInt(pid));
        }

        @XmlRpcRoutine
        public XmlRpcArray<?> getSubscriptions(XmlRpcString callerId) {
            internalLogger.trace("getSubscriptions({})", callerId);
            return RosUtils.buildRpcResult(transportManager.getSubEntries().stream()
                    .map(sub -> XmlRpcArray.of(
                            new XmlRpcString(sub.getKey()),
                            new XmlRpcString(sub.getValue().getMsgType().getId().toUnrootedString())))
                    .collect(XmlRpcArray.collect()));
        }

        @XmlRpcRoutine
        public XmlRpcArray<?> getPublications(XmlRpcString callerId) {
            internalLogger.trace("getPublications({})", callerId);
            return RosUtils.buildRpcResult(transportManager.getPubEntries().stream()
                    .map(pub -> XmlRpcArray.of(
                            new XmlRpcString(pub.getKey()),
                            new XmlRpcString(pub.getValue().getMsgType().getId().toUnrootedString())))
                    .collect(XmlRpcArray.collect()));
        }

        @XmlRpcRoutine
        public XmlRpcArray<?> paramUpdate(XmlRpcString callerId, XmlRpcString paramKey, XmlRpcData paramValue) {
            internalLogger.trace("paramUpdate({}, {}, {})", callerId, paramKey, paramValue);
            paramManager.notifyParamUpdate(RosId.resolveGlobal(paramKey.value), paramValue);
            return RosUtils.buildRpcResult(XmlRpcInt.ZERO);
        }

        @XmlRpcRoutine
        public XmlRpcArray<?> publisherUpdate(XmlRpcString callerId, XmlRpcString topicName, XmlRpcArray<XmlRpcString> pubUris) {
            internalLogger.trace("publisherUpdate({}, {}, {})", callerId, topicName, pubUris);
            RosId topicId = RosId.resolveGlobal(topicName.value);
            RosSubscriber<?> sub = getTransportManager().getSub(topicId);
            if (sub != null) {
                scheduleRpcTask(new OpenSubscriberTask(RosNode.this, sub.getTopicId(), sub.getMsgType(),
                        pubUris.stream().map(u -> URI.create(u.value)).collect(Collectors.toList())));
            }
            return RosUtils.buildRpcResult(XmlRpcInt.ZERO);
        }

        @XmlRpcRoutine
        public XmlRpcArray<?> requestTopic(XmlRpcString callerId, XmlRpcString topicName,
                                           XmlRpcArray<XmlRpcArray<XmlRpcData>> protocols) {
            internalLogger.trace("requestTopic({}, {}, {})", callerId, topicName, protocols);
            for (XmlRpcArray<XmlRpcData> protocol : protocols) {
                if (((XmlRpcString)protocol.get(0)).value.equals("TCPROS")) {
                    Objects.requireNonNull(tcpServerUri);
                    return RosUtils.buildRpcResult(XmlRpcArray.of(
                            new XmlRpcString("TCPROS"),
                            new XmlRpcString(tcpServerUri.getHost()),
                            new XmlRpcInt(tcpServerUri.getPort())));
                }
            }
            throw new NoSuchElementException("No usable protocols!");
        }

    }

}
