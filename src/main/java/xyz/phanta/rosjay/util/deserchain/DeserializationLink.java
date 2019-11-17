package xyz.phanta.rosjay.util.deserchain;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class DeserializationLink implements DeserializationChain {

    @Nullable
    private final DeserializationChain next;
    private final int expLen;
    private final Sink sink;

    public DeserializationLink(@Nullable DeserializationChain next, int expLen, Sink sink) {
        this.next = next;
        this.expLen = expLen;
        this.sink = sink;
    }

    @Override
    public int getExpectedBytes() {
        return expLen;
    }

    @Nullable
    @Override
    public DeserializationChain consume(ByteBuffer buf, int length) {
        sink.consume(buf, length);
        return next;
    }

    public interface Sink {

        void consume(ByteBuffer buf, int length);

    }

}
