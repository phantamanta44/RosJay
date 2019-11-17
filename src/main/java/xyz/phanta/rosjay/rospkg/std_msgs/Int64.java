package xyz.phanta.rosjay.rospkg.std_msgs;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageType;

public interface Int64 extends RosData<Int64> {

    RosMessageType<Int64> TYPE = RosMessageType.resolve(StdMsgs.NAMESPACE, "Int64", Int64.class);

    long getData();

    void setData(long value);

}
