package xyz.phanta.rosjay.rospkg.roscpp_tutorials;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.srv.RosServiceType;

public class TwoInts {

    public static final RosServiceType<Req, Res> TYPE
            = RosServiceType.resolve(RosCppTutorials.NAMESPACE, "TwoInts", Req.class, Res.class);

    private TwoInts() {
        // NO-OP
    }

    public interface Req extends RosData<Req> {

        long getA();

        void setA(long value);

        long getB();

        void setB(long value);

    }

    public interface Res extends RosData<Res> {

        long getSum();

        void setSum(long value);

    }

}
