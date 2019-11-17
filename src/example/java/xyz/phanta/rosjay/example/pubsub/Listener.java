package xyz.phanta.rosjay.example.pubsub;

import xyz.phanta.rosjay.node.RosNode;
import xyz.phanta.rosjay.rospkg.std_msgs.RosString;
import xyz.phanta.rosjay.transport.msg.RosSubscriber;

public class Listener {

    public static void main(String[] args) {
        // initialize node
        RosNode node = new RosNode("listener");
        node.init();

        // create subscription and define callback
        RosSubscriber<RosString> sub = node.subscribe("chatter", RosString.TYPE, 1000,
                msg -> node.getLogger().info("I heard: [%s]", msg.getData()));

        // spin forever
        node.spin();
    }

}
