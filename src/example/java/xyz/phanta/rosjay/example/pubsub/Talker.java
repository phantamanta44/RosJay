package xyz.phanta.rosjay.example.pubsub;

import xyz.phanta.rosjay.node.RosNode;
import xyz.phanta.rosjay.rospkg.std_msgs.RosString;
import xyz.phanta.rosjay.transport.msg.RosPublisher;
import xyz.phanta.rosjay.util.RosRate;

public class Talker {

    public static void main(String[] args) {
        // initialize node
        RosNode node = new RosNode("talker");
        node.init();

        // create publication
        RosPublisher<RosString> chatterPub = node.advertise("chatter", RosString.TYPE, 1000);

        // create rate limiter
        RosRate rate = new RosRate(10D);

        // counter to be published
        int count = 0;

        // main loop
        while (node.isAlive()) {
            // create a std_msgs/String instance
            RosString msg = RosString.TYPE.newInstance();
            msg.setData("hello world " + count);

            // log message data to rosout
            node.getLogger().info(msg.getData());

            // publish the message
            chatterPub.publish(msg);

            // spin and sleep
            node.spinOnce();
            rate.sleep();

            // increment counter
            ++count;
        }
    }

}
