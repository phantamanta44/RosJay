package xyz.phanta.rosjay.rospkg.std_msgs;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageType;

public interface RosString extends RosData<RosString> { // disambiguates from java String type

    RosMessageType<RosString> TYPE = RosMessageType.resolve(StdMsgs.NAMESPACE, "String", RosString.class);

    String getData();

    void setData(String value);

}
