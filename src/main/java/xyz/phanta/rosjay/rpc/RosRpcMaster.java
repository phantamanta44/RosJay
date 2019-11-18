package xyz.phanta.rosjay.rpc;

import xyz.phanta.jxmlrpc.XmlRpcClient;
import xyz.phanta.jxmlrpc.data.*;
import xyz.phanta.rosjay.node.RosNode;
import xyz.phanta.rosjay.transport.msg.RosPublisher;
import xyz.phanta.rosjay.transport.msg.RosSubscriber;
import xyz.phanta.rosjay.util.RosUtils;
import xyz.phanta.rosjay.util.id.RosId;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class RosRpcMaster {

    private final RosNode caller;
    private final XmlRpcString callerId;
    private final XmlRpcClient rpcOut;

    public RosRpcMaster(RosNode caller, URI masterUri) {
        this.caller = caller;
        this.callerId = new XmlRpcString(caller.getId());
        this.rpcOut = new XmlRpcClient(masterUri);
    }

    public URI getRpcUri() {
        return rpcOut.getServerUri();
    }

    // topic/service provider registration

    public void registerService(RosId serviceId) throws IOException {
        URI tcpUri = caller.getTcpServerUri();
        rpcOut.invokeRemote("registerService", callerId,
                new XmlRpcString(serviceId),
                new XmlRpcString("rosrpc://" + tcpUri.getHost() + ":" + tcpUri.getPort()),
                new XmlRpcString(caller.getRpcServerUri()));
    }

    public void unregisterService(RosId serviceId) throws IOException {
        rpcOut.invokeRemote("unregisterService", callerId,
                new XmlRpcString(serviceId),
                new XmlRpcString(caller.getTcpServerUri()));
    }

    public List<URI> registerSubscriber(RosSubscriber<?> sub) throws IOException {
        return RosUtils.<XmlRpcArray<XmlRpcString>>unwrapRpcResult(
                rpcOut.invokeRemote("registerSubscriber", callerId,
                        new XmlRpcString(sub.getTopicId()),
                        new XmlRpcString(sub.getMsgType().getId().toUnrootedString()),
                        new XmlRpcString(caller.getRpcServerUri()))
        ).stream().map(u -> URI.create(u.value)).collect(Collectors.toList());
    }

    public void unregisterSubscriber(RosId topicId) throws IOException {
        rpcOut.invokeRemote("unregisterSubscriber", callerId,
                new XmlRpcString(topicId),
                new XmlRpcString(caller.getRpcServerUri()));
    }

    public void registerPublisher(RosPublisher<?> pub) throws IOException {
        rpcOut.invokeRemote("registerPublisher", callerId,
                new XmlRpcString(pub.getTopicId()),
                new XmlRpcString(pub.getMsgType().getId().toUnrootedString()),
                new XmlRpcString(caller.getRpcServerUri()));
    }

    public void unregisterPublisher(RosId topicId) throws IOException {
        rpcOut.invokeRemote("unregisterPublisher", callerId,
                new XmlRpcString(topicId),
                new XmlRpcString(caller.getRpcServerUri()));
    }

    // ros master properties

    public URI lookupNode(RosId nodeId) throws IOException {
        return URI.create(RosUtils.<XmlRpcString>unwrapRpcResult(
                rpcOut.invokeRemote("lookupNode", callerId, new XmlRpcString(nodeId))
        ).value);
    }

    public Map<String, String> getPublishedTopics(@Nullable String subgraph) throws IOException {
        return RosUtils.<XmlRpcArray<XmlRpcArray<XmlRpcString>>>unwrapRpcResult(
                rpcOut.invokeRemote("getPublishedTopics", callerId, new XmlRpcString(subgraph != null ? subgraph : ""))
        ).stream().collect(Collectors.toMap(e -> e.get(0).value, e -> e.get(1).value));
    }

    public Map<String, String> getTopicTypes() throws IOException {
        return RosUtils.<XmlRpcArray<XmlRpcArray<XmlRpcString>>>unwrapRpcResult(
                rpcOut.invokeRemote("getTopicTypes", callerId)
        ).stream().collect(Collectors.toMap(e -> e.get(0).value, e -> e.get(1).value));
    }

    public SystemStateResponse getSystemState() throws IOException {
        XmlRpcArray<XmlRpcArray<XmlRpcArray<?>>> res = RosUtils.unwrapRpcResult(
                rpcOut.invokeRemote("getSystemState", callerId)
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

    public URI getUri() throws IOException {
        return URI.create(RosUtils.<XmlRpcString>unwrapRpcResult(
                rpcOut.invokeRemote("getUri", callerId)
        ).value);
    }

    @Nullable
    public URI lookupService(RosId serviceId) throws IOException {
        XmlRpcArray<?> result = (XmlRpcArray<?>)rpcOut.invokeRemote("lookupService", callerId, new XmlRpcString(serviceId));
        return ((XmlRpcInt)result.get(0)).value != 1 ? null : URI.create(RosUtils.<XmlRpcString>unwrapRpcResult(result).value);
    }

    // parameter server

    public void deleteParam(RosId paramKey) throws IOException {
        rpcOut.invokeRemote("deleteParam", callerId, new XmlRpcString(paramKey));
    }

    public void setParam(RosId paramKey, XmlRpcData value) throws IOException {
        rpcOut.invokeRemote("setParam", callerId, new XmlRpcString(paramKey), value);
    }

    @Nullable
    public XmlRpcData getParam(RosId paramKey) throws IOException {
        return resolveNullableParam(
                (XmlRpcArray<?>)rpcOut.invokeRemote("getParam", callerId, new XmlRpcString(paramKey))
        );
    }

    @Nullable
    public RosId searchParam(String paramName) throws IOException {
        XmlRpcArray<?> result = (XmlRpcArray<?>)rpcOut.invokeRemote("searchParam", callerId, new XmlRpcString(paramName));
        return ((XmlRpcInt)result.get(0)).value != 1 ? null
                : RosId.resolveGlobal(RosUtils.<XmlRpcString>unwrapRpcResult(result).value);
    }

    @Nullable
    public XmlRpcData subscribeParam(RosId paramKey) throws IOException {
        return resolveNullableParam(
                (XmlRpcArray<?>)rpcOut.invokeRemote("subscribeParam", callerId,
                        new XmlRpcString(caller.getRpcServerUri()),
                        new XmlRpcString(paramKey))
        );
    }

    public void unsubscribeParam(RosId paramKey) throws IOException {
        rpcOut.invokeRemote("unsubscribeParam", callerId,
                new XmlRpcString(caller.getRpcServerUri()),
                new XmlRpcString(paramKey));
    }

    public boolean hasParam(RosId paramKey) throws IOException {
        return RosUtils.<XmlRpcBool>unwrapRpcResult(
                rpcOut.invokeRemote("hasParam", callerId, new XmlRpcString(paramKey))
        ).value;
    }

    public List<RosId> getParamNames() throws IOException {
        return RosUtils.<XmlRpcArray<XmlRpcString>>unwrapRpcResult(
                rpcOut.invokeRemote("getParamNames", callerId)
        ).stream().map(s -> RosId.resolveGlobal(s.value)).collect(Collectors.toList());
    }

    @Nullable
    private static XmlRpcData resolveNullableParam(XmlRpcArray<?> response) {
        if (((XmlRpcInt)response.get(0)).value != 1) {
            return null;
        }
        XmlRpcData result = response.get(2);
        return (result instanceof XmlRpcStruct<?> && ((XmlRpcStruct<?>)result).getEntries().isEmpty()) ? null : result;
    }

}
