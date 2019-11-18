package xyz.phanta.rosjay.example.service;

import xyz.phanta.rosjay.node.RosNode;
import xyz.phanta.rosjay.rospkg.roscpp_tutorials.TwoInts;
import xyz.phanta.rosjay.transport.srv.RosServiceProvider;

public class AddTwoIntsServer {

    public static void main(String[] args) {
        // initialize node
        RosNode node = new RosNode("add_two_ints_client");
        node.init();

        // create service server and define handler
        RosServiceProvider<TwoInts.Req, TwoInts.Res> client = node.advertiseService("add_two_ints", TwoInts.TYPE,
                req -> {
                    TwoInts.Res response = TwoInts.TYPE.newResponse();
                    response.setSum(req.getA() + req.getB());
                    node.getLogger().info("request: x={}, y={}", req.getA(), req.getB());
                    node.getLogger().info("sending back response: [{}]", response.getSum());
                    return response;
                });

        // spin forever
        node.spin();
    }

}
