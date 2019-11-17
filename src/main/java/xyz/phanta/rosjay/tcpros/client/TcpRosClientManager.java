package xyz.phanta.rosjay.tcpros.client;

import org.slf4j.Logger;
import xyz.phanta.rosjay.node.RosNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

public class TcpRosClientManager {

    private final RosNode rosNode;
    private final Logger internalLog;
    private final Map<SocketAddress, TcpRosClient> clients = new HashMap<>();

    private boolean alive = true;

    public TcpRosClientManager(RosNode rosNode) {
        this.rosNode = rosNode;
        this.internalLog = rosNode.getChildInternalLogger("tcpros_climan");
    }

    RosNode getRosNode() {
        return rosNode;
    }

    public void openConnection(SocketAddress address, TcpRosTarget target) throws IOException {
        TcpRosClient client = new TcpRosClient(this, address, target);
        client.connect();
        synchronized (clients) {
            clients.put(address, client);
        }
    }

    public boolean isConnectionOpen(InetSocketAddress address) {
        return clients.containsKey(address);
    }

    void notifyClientKilled(TcpRosClient client) {
        if (alive) {
            synchronized (clients) {
                clients.remove(client.getAddress());
            }
        }
    }

    public void kill() throws IOException {
        alive = false;
        synchronized (clients) {
            for (TcpRosClient client : clients.values()) {
                internalLog.trace("Cleaning up TCPROS client connection to {}...", client.getAddress());
                client.kill();
            }
        }
    }

}
