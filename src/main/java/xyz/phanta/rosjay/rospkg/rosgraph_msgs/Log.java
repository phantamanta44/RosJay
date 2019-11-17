package xyz.phanta.rosjay.rospkg.rosgraph_msgs;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.rospkg.std_msgs.Header;
import xyz.phanta.rosjay.transport.msg.RosMessageType;

import java.util.List;

public interface Log extends RosData<Log> {

    RosMessageType<Log> TYPE = RosMessageType.resolve(RosGraphMsgs.NAMESPACE, "Log", Log.class);
    byte DEBUG = 1, INFO = 2, WARN = 4, ERROR = 8, FATAL = 16;

    Header getHeader();

    void setHeader(Header header);

    byte getLevel();

    void setLevel(byte value);

    String getName();

    void setName(String value);

    String getMsg();

    void setMsg(String value);

    String getFile();

    void setFile(String value);

    String getFunction();

    void setFunction(String value);

    int getLine();

    void setLine(int value);

    List<String> getTopics();

    void setTopics(List<String> value);

}
