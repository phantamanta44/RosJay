package xyz.phanta.rosjay.example.param;

import xyz.phanta.jxmlrpc.data.XmlRpcString;
import xyz.phanta.rosjay.node.RosNode;

public class ParamEcho {

    public static void main(String[] args) {
        // initialize node
        RosNode node = new RosNode("param_echo");
        node.init();

        // register parameter callback
        node.getParameters().<XmlRpcString>addCallback("echo", s -> {
            if (s != null) {
                node.getLogger().info(s.value);
            }
        });

        // spin forever
        node.spin();
    }

}
