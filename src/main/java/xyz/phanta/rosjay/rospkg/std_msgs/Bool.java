package xyz.phanta.rosjay.rospkg.std_msgs;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageType;

public interface Bool extends RosData<Bool> {

    RosMessageType<Bool> TYPE = RosMessageType.resolve(StdMsgs.NAMESPACE, "Bool", Bool.class);

    boolean getData();

    void setData(boolean value);

}
