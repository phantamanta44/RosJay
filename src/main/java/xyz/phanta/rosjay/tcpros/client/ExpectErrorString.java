package xyz.phanta.rosjay.tcpros.client;

import xyz.phanta.rosjay.tcpros.stator.ExpectDatagramLength;
import xyz.phanta.rosjay.tcpros.stator.TcpStateMachine;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

class ExpectErrorString implements TcpStateMachine.State {

    private final int length;
    private final Consumer<String> callback;

    static TcpStateMachine.State expect(Consumer<String> callback) {
        return new ExpectDatagramLength(len -> new ExpectErrorString(len, callback));
    }

    private ExpectErrorString(int length, Consumer<String> callback) {
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
        byte[] data = new byte[length];
        buf.get(data);
        callback.accept(new String(data, StandardCharsets.US_ASCII));
        return null;
    }

}
