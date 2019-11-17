package xyz.phanta.rosjay.tcpros.stator;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ExpectHeaderDatagram implements TcpStateMachine.State {

    public static TcpStateMachine.State expectHeader(Function<Map<String, String>, TcpStateMachine.State> callback) {
        return new ExpectDatagramLength(length -> new ExpectHeaderDatagram(length, callback));
    }

    private final int length;
    private final Function<Map<String, String>, TcpStateMachine.State> callback;

    private ExpectHeaderDatagram(int length, Function<Map<String, String>, TcpStateMachine.State> callback) {
        this.length = length;
        this.callback = callback;
    }

    @Override
    public int getExpectedBytes() {
        return length;
    }

    @Nullable
    @Override
    public TcpStateMachine.State consume(ByteBuffer buf) {
        Map<String, String> fields = new HashMap<>();
        while (buf.hasRemaining()) {
            int fieldLength = buf.getInt();
            byte[] fieldData = new byte[fieldLength];
            buf.get(fieldData);
            String fieldStr = new String(fieldData, StandardCharsets.US_ASCII);
            int splitIndex = fieldStr.indexOf('=');
            fields.put(fieldStr.substring(0, splitIndex), fieldStr.substring(splitIndex + 1));
        }
        return callback.apply(fields);
    }

}
