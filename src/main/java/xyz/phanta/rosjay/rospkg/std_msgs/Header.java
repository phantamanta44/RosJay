package xyz.phanta.rosjay.rospkg.std_msgs;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageType;

import java.time.Instant;

public interface Header extends RosData<Header> {

    RosMessageType<Header> TYPE = RosMessageType.resolve(StdMsgs.NAMESPACE, "Header", Header.class);

    int getSeq();

    void setSeq(int value);

    Instant getStamp();

    void setStamp(Instant value);

    RosString getFrameId();

    void setFrameId(RosString value);

}
