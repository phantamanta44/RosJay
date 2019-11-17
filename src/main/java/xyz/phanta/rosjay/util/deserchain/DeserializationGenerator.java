package xyz.phanta.rosjay.util.deserchain;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class DeserializationGenerator implements DeserializationChain {

    @Nullable
    private final DeserializationChain next;
    private final int expLen;
    private final Factory factory;

    public DeserializationGenerator(@Nullable DeserializationChain next, int expLen, Factory factory) {
        this.next = next;
        this.expLen = expLen;
        this.factory = factory;
    }

    @Override
    public int getExpectedBytes() {
        return expLen;
    }

    @Override
    public DeserializationChain consume(ByteBuffer buf, int length) {
        return factory.link(buf, length, next);
    }

    public interface Factory {

        @Nullable
        DeserializationChain link(ByteBuffer buf, int length, @Nullable DeserializationChain next);

    }

}
