package xyz.phanta.rosjay.rospkg.std_msgs;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageType;

public interface Float32 extends RosData<Float32> {

    RosMessageType<Float32> TYPE = RosMessageType.resolve(StdMsgs.NAMESPACE, "Float32", Float32.class);

    float getData();

    void setData(float value);

}
