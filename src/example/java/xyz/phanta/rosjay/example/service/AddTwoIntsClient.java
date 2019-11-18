package xyz.phanta.rosjay.example.service;

import xyz.phanta.rosjay.node.RosNode;
import xyz.phanta.rosjay.rospkg.roscpp_tutorials.TwoInts;
import xyz.phanta.rosjay.transport.srv.RosServiceClient;

public class AddTwoIntsClient {

    public static void main(String[] args) {
        // parse cli args
        if (args.length != 2) {
            System.out.println("Usage: add_two_ints_client a b");
            System.exit(1);
        }
        long a = Long.parseLong(args[0]), b = Long.parseLong(args[1]);

        // initialize node
        RosNode node = new RosNode("add_two_ints_client");
        node.init();

        // create service client
        RosServiceClient<TwoInts.Req, TwoInts.Res> client = node.serviceClient("add_two_ints", TwoInts.TYPE);

        // create request
        TwoInts.Req request = TwoInts.TYPE.newRequest();
        request.setA(a);
        request.setB(b);

        // call service
        try {
            TwoInts.Res response = client.call(request);
            System.out.println("Sum: " + response.getSum());
        } catch (InterruptedException e) {
            System.err.println("Service request interrupted!");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Failed to call service add_two_ints!");
            e.printStackTrace();
        }
    }

}
