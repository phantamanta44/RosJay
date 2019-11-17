package xyz.phanta.rosjay.rpc;

import xyz.phanta.rosjay.node.RosNode;
import xyz.phanta.rosjay.util.BusStateTracker;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.id.RosNamespace;
import xyz.phanta.rosjay.util.RosUtils;
import xyz.phanta.jxmlrpc.XmlRpcClient;
import xyz.phanta.jxmlrpc.data.XmlRpcArray;
import xyz.phanta.jxmlrpc.data.XmlRpcData;
import xyz.phanta.jxmlrpc.data.XmlRpcInt;
import xyz.phanta.jxmlrpc.data.XmlRpcString;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RosRpcNode {

    private final XmlRpcClient rpcOut;

    public RosRpcNode(URI nodeUri) {
        this.rpcOut = new XmlRpcClient(nodeUri);
    }

    // TODO getBusStats

    public List<BusInfoResponse> getBusInfo(RosNode caller) throws IOException {
        List<BusInfoResponse> result = new ArrayList<>();
        for (XmlRpcArray<XmlRpcString> busInfo : RosUtils.<XmlRpcArray<XmlRpcArray<XmlRpcString>>>unwrapRpcResult(
                rpcOut.invokeRemote("getBusInfo", new XmlRpcString(caller.getId()))
        )) {
            result.add(new BusInfoResponse(
                    URI.create(busInfo.get(1).value),
                    BusStateTracker.State.parse(busInfo.get(2).value),
                    busInfo.get(3).value,
                    RosNamespace.ROOT.resolveId(busInfo.get(4).value)));
        }
        return result;
    }

    public static class BusInfoResponse {

        public final URI destination;
        public final BusStateTracker.State direction;
        public final String transportType;
        public final RosId transportName;

        BusInfoResponse(URI destination, BusStateTracker.State direction, String transportType, RosId transportName) {
            this.destination = destination;
            this.direction = direction;
            this.transportType = transportType;
            this.transportName = transportName;
        }

    }

    public URI getMasterUri(RosNode caller) throws IOException {
        return URI.create(RosUtils.<XmlRpcString>unwrapRpcResult(
                rpcOut.invokeRemote("getMasterUri", new XmlRpcString(caller.getId().toString()))
        ).value);
    }

    public void shutdown(RosNode caller, String reason) throws IOException {
        rpcOut.invokeRemote("shutdown", new XmlRpcString(caller.getId()), new XmlRpcString(reason));
    }

    public int getPid(RosNode caller) throws IOException {
        return RosUtils.<XmlRpcInt>unwrapRpcResult(
                rpcOut.invokeRemote("getPid", new XmlRpcString(caller.getId()))
        ).value;
    }

    public List<TopicResponse> getSubscriptions(RosNode caller) throws IOException {
        List<TopicResponse> result = new ArrayList<>();
        for (XmlRpcArray<XmlRpcString> sub : RosUtils.<XmlRpcArray<XmlRpcArray<XmlRpcString>>>unwrapRpcResult(
                rpcOut.invokeRemote("getSubscriptions", new XmlRpcString(caller.getId()))
        )) {
            result.add(new TopicResponse(
                    RosNamespace.ROOT.resolveId(sub.get(0).value),
                    RosNamespace.ROOT.resolveId(sub.get(1).value)));
        }
        return result;
    }

    public List<TopicResponse> getPublications(RosNode caller) throws IOException {
        List<TopicResponse> result = new ArrayList<>();
        for (XmlRpcArray<XmlRpcString> pub : RosUtils.<XmlRpcArray<XmlRpcArray<XmlRpcString>>>unwrapRpcResult(
                rpcOut.invokeRemote("getPublications", new XmlRpcString(caller.getId()))
        )) {
            result.add(new TopicResponse(
                    RosNamespace.ROOT.resolveId(pub.get(0).value),
                    RosNamespace.ROOT.resolveId(pub.get(1).value)));
        }
        return result;
    }

    public static class TopicResponse {

        public final RosId topicName;
        public final RosId topicType;

        TopicResponse(RosId topicName, RosId topicType) {
            this.topicName = topicName;
            this.topicType = topicType;
        }

    }

    public void paramUpdate(RosNode caller, RosId paramId, XmlRpcData paramValue) throws IOException {
        rpcOut.invokeRemote("paramUpdate", new XmlRpcString(caller.getId()), new XmlRpcString(paramId), paramValue);
    }

    public void publisherUpdate(RosNode caller, RosId topicId, Collection<URI> pubUris) throws IOException {
        rpcOut.invokeRemote("publisherUpdate", new XmlRpcString(caller), new XmlRpcString(topicId),
                pubUris.stream().map(XmlRpcString::new).collect(XmlRpcArray.collect()));
    }

    @Nullable
    public InetSocketAddress requestTopic(RosNode caller, RosId topicId) throws IOException {
        XmlRpcArray<XmlRpcData> result = RosUtils.unwrapRpcResult(
                rpcOut.invokeRemote("requestTopic",
                        new XmlRpcString(caller.getId()),
                        new XmlRpcString(topicId),
                        XmlRpcArray.of(XmlRpcArray.of(new XmlRpcString("TCPROS"))))
        );
        return result.isEmpty() ? null
                : new InetSocketAddress(((XmlRpcString)result.get(1)).value, ((XmlRpcInt)result.get(2)).value);
    }

}
