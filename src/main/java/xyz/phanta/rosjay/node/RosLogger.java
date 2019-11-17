package xyz.phanta.rosjay.node;

import xyz.phanta.rosjay.rospkg.rosgraph_msgs.Log;
import xyz.phanta.rosjay.transport.msg.RosPublisher;

import java.util.stream.Collectors;

public class RosLogger {

    private final RosNode owner;
    private final RosPublisher<Log> rosOut;

    public RosLogger(RosNode owner, RosPublisher<Log> rosOut) {
        this.owner = owner;
        this.rosOut = rosOut;
    }

    public void debug(Object message) {
        debug(message.toString());
    }

    public void debug(String format, Object... args) {
        debug(String.format(format, args));
    }

    public void debug(String message) {
        dispatchMessage(message, Log.DEBUG);
    }

    public void info(Object message) {
        info(message.toString());
    }

    public void info(String format, Object... args) {
        info(String.format(format, args));
    }

    public void info(String message) {
        dispatchMessage(message, Log.INFO);
    }

    public void warn(Object message) {
        warn(message.toString());
    }

    public void warn(String format, Object... args) {
        warn(String.format(format, args));
    }

    public void warn(String message) {
        dispatchMessage(message, Log.WARN);
    }

    public void error(Object message) {
        error(message.toString());
    }

    public void error(String format, Object... args) {
        error(String.format(format, args));
    }

    public void error(String message) {
        dispatchMessage(message, Log.ERROR);
    }

    public void fatal(Object message) {
        fatal(message.toString());
    }

    public void fatal(String format, Object... args) {
        fatal(String.format(format, args));
    }

    public void fatal(String message) {
        dispatchMessage(message, Log.FATAL);
    }

    private void dispatchMessage(String msgBody, byte level) {
        Log msg = Log.TYPE.newInstance();
        msg.setLevel(level);
        msg.setName(owner.getId().toString());
        msg.setMsg(msgBody);
        msg.setTopics(owner.getTransportManager().getPubEntries().stream()
                .map(pub -> pub.getKey().toString())
                .collect(Collectors.toList()));
        rosOut.publish(msg);
    }

}
