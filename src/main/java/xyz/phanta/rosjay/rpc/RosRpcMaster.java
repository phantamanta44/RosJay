package xyz.phanta.rosjay.rpc;

import xyz.phanta.jxmlrpc.XmlRpcClient;
import xyz.phanta.jxmlrpc.data.*;
import xyz.phanta.rosjay.node.RosNode;
import xyz.phanta.rosjay.transport.msg.RosPublisher;
import xyz.phanta.rosjay.transport.msg.RosSubscriber;
import xyz.phanta.rosjay.util.RosUtils;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.id.RosNamespace;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class RosRpcMaster {

    private final XmlRpcClient rpcOut;

    public RosRpcMaster(URI masterUri) {
        this.rpcOut = new XmlRpcClient(masterUri);
    }

    public URI getUri() {
        return rpcOut.getServerUri();
    }

    // topic/service provider registration

    public void registerService(RosNode caller, String serviceName) throws IOException {
        rpcOut.invokeRemote("registerService",
                new XmlRpcString(caller.getId()),
                new XmlRpcString(serviceName),
                new XmlRpcString(caller.getTcpServerUri()),
                new XmlRpcString(caller.getRpcServerUri()));
    }

    public void unregisterService(RosNode caller, String serviceName) throws IOException {
        rpcOut.invokeRemote("unregisterService",
                new XmlRpcString(caller.getId()),
                new XmlRpcString(serviceName),
                new XmlRpcString(caller.getTcpServerUri()));
    }

    public List<URI> registerSubscriber(RosNode caller, RosSubscriber<?> sub) throws IOException {
        return RosUtils.<XmlRpcArray<XmlRpcString>>unwrapRpcResult(
                rpcOut.invokeRemote("registerSubscriber",
                        new XmlRpcString(caller.getId()),
                        new XmlRpcString(sub.getTopicId()),
                        new XmlRpcString(sub.getMsgType().getId().toUnrootedString()),
                        new XmlRpcString(caller.getRpcServerUri()))
        ).stream().map(u -> URI.create(u.value)).collect(Collectors.toList());
    }

    public void unregisterSubscriber(RosNode caller, RosId topicId) throws IOException {
        rpcOut.invokeRemote("unregisterSubscriber",
                new XmlRpcString(caller.getId()),
                new XmlRpcString(topicId),
                new XmlRpcString(caller.getRpcServerUri()));
    }

    public void registerPublisher(RosNode caller, RosPublisher<?> pub) throws IOException {
        rpcOut.invokeRemote("registerPublisher",
                new XmlRpcString(caller.getId()),
                new XmlRpcString(pub.getTopicId()),
                new XmlRpcString(pub.getMsgType().getId().toUnrootedString()),
                new XmlRpcString(caller.getRpcServerUri()));
    }

    public void unregisterPublisher(RosNode caller, RosId topicId) throws IOException {
        rpcOut.invokeRemote("unregisterPublisher",
                new XmlRpcString(caller.getId()),
                new XmlRpcString(topicId),
                new XmlRpcString(caller.getRpcServerUri()));
    }

    // ros master properties

    public URI lookupNode(RosNode caller, RosId nodeId) throws IOException {
        return URI.create(RosUtils.<XmlRpcString>unwrapRpcResult(
                rpcOut.invokeRemote("lookupNode", new XmlRpcString(caller.getId()), new XmlRpcString(nodeId))
        ).value);
    }

    public Map<String, String> getPublishedTopics(RosNode caller, @Nullable String subgraph) throws IOException {
        return RosUtils.<XmlRpcArray<XmlRpcArray<XmlRpcString>>>unwrapRpcResult(
                rpcOut.invokeRemote("getPublishedTopics",
                        new XmlRpcString(caller.getId()),
                        new XmlRpcString(subgraph != null ? subgraph : ""))
        ).stream().collect(Collectors.toMap(e -> e.get(0).value, e -> e.get(1).value));
    }

    public Map<String, String> getTopicTypes(RosNode caller) throws IOException {
        return RosUtils.<XmlRpcArray<XmlRpcArray<XmlRpcString>>>unwrapRpcResult(
                rpcOut.invokeRemote("getTopicTypes", new XmlRpcString(caller.getId()))
        ).stream().collect(Collectors.toMap(e -> e.get(0).value, e -> e.get(1).value));
    }

    public SystemStateResponse getSystemState(RosNode caller) throws IOException {
        XmlRpcArray<XmlRpcArray<XmlRpcArray<?>>> res = RosUtils.unwrapRpcResult(
                rpcOut.invokeRemote("getSystemState", new XmlRpcString(caller.getId()))
        );
        SystemStateResponse state = new SystemStateResponse();
        for (XmlRpcArray<?> pubTopic : res.get(0)) {
            state.publishers.put(((XmlRpcString)pubTopic.get(0)).value,
                    ((XmlRpcArray<XmlRpcString>)pubTopic.get(1)).stream()
                            .map(s -> s.value)
                            .collect(Collectors.toList()));
        }
        for (XmlRpcArray<?> subTopic : res.get(1)) {
            state.subscribers.put(((XmlRpcString)subTopic.get(0)).value,
                    ((XmlRpcArray<XmlRpcString>)subTopic.get(1)).stream()
                            .map(s -> s.value)
                            .collect(Collectors.toList()));
        }
        for (XmlRpcArray<?> service : res.get(2)) {
            state.services.put(((XmlRpcString)service.get(0)).value,
                    ((XmlRpcArray<XmlRpcString>)service.get(1)).stream()
                            .map(s -> s.value)
                            .collect(Collectors.toList()));
        }
        return state;
    }

    public static class SystemStateResponse {

        private final Map<String, List<String>> publishers = new HashMap<>();
        private final Map<String, List<String>> subscribers = new HashMap<>();
        private final Map<String, List<String>> services = new HashMap<>();

        public Map<String, List<String>> getPublishers() {
            return publishers;
        }

        public Map<String, List<String>> getSubscribers() {
            return subscribers;
        }

        public Map<String, List<String>> getServices() {
            return services;
        }

    }

    public URI getUri(RosNode caller) throws IOException {
        return URI.create(RosUtils.<XmlRpcString>unwrapRpcResult(
                rpcOut.invokeRemote("getUri", new XmlRpcString(caller.getId()))
        ).value);
    }

    public URI getService(RosNode caller, String serviceName) throws IOException {
        return URI.create(RosUtils.<XmlRpcString>unwrapRpcResult(
                rpcOut.invokeRemote("lookupService", new XmlRpcString(caller.getId()), new XmlRpcString(serviceName))
        ).value);
    }

    // parameter server

    public void deleteParam(RosNode caller, RosId paramKey) throws IOException {
        rpcOut.invokeRemote("deleteParam", new XmlRpcString(caller.getId()), new XmlRpcString(paramKey));
    }

    public void setParam(RosNode caller, RosId paramKey, XmlRpcData value) throws IOException {
        rpcOut.invokeRemote("setParam", new XmlRpcString(caller.getId()), new XmlRpcString(paramKey), value);
    }

    public XmlRpcData getParam(RosNode caller, RosId paramKey) throws IOException {
        return RosUtils.unwrapRpcResult(
                rpcOut.invokeRemote("getParam", new XmlRpcString(caller.getId()), new XmlRpcString(paramKey))
        );
    }

    @Nullable
    public RosId searchParam(RosNode caller, String paramName) throws IOException {
        XmlRpcArray<?> result = (XmlRpcArray<?>)rpcOut.invokeRemote("searchParam",
                new XmlRpcString(caller.getId()),
                new XmlRpcString(paramName));
        return ((XmlRpcInt)result.get(0)).value != 1 ? null
                : RosNamespace.ROOT.resolveId(RosUtils.<XmlRpcString>unwrapRpcResult(result).value);
    }

    @Nullable
    public XmlRpcData subscribeParam(RosNode caller, RosId paramKey) throws IOException {
        XmlRpcArray<?> result = (XmlRpcArray<?>)rpcOut.invokeRemote("subscribeParam",
                new XmlRpcString(caller.getId()),
                new XmlRpcString(caller.getRpcServerUri()),
                new XmlRpcString(paramKey));
        return ((XmlRpcInt)result.get(0)).value != 1 ? null : result.get(2);
    }

    public void unsubscribeParam(RosNode caller, RosId paramKey) throws IOException {
        rpcOut.invokeRemote("unsubscribeParam",
                new XmlRpcString(caller.getId()),
                new XmlRpcString(caller.getRpcServerUri()),
                new XmlRpcString(paramKey));
    }

    public boolean hasParam(RosNode caller, RosId paramKey) throws IOException {
        return RosUtils.<XmlRpcBool>unwrapRpcResult(
                rpcOut.invokeRemote("hasParam", new XmlRpcString(caller.getId()), new XmlRpcString(paramKey))
        ).value;
    }

    public List<RosId> getParamNames(RosNode caller) throws IOException {
        return RosUtils.<XmlRpcArray<XmlRpcString>>unwrapRpcResult(
                rpcOut.invokeRemote("getParamNames", new XmlRpcString(caller.getId()))
        ).stream().map(s -> RosNamespace.ROOT.resolveId(s.value)).collect(Collectors.toList());
    }

}
