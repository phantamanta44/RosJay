package xyz.phanta.rosjay.util.deserchain;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class DeserializationComposer implements DeserializationChain {

    @Nullable
    private final DeserializationChain next;
    private final DeserializationChain underlying;
    private final DeserializationLink.Sink sink;

    public DeserializationComposer(@Nullable DeserializationChain next, UnaryOperator<DeserializationChain> underlyingFactory,
                                   DeserializationLink.Sink sink) {
        this.next = next;
        this.underlying = Objects.requireNonNull(underlyingFactory.apply(null));
        this.sink = sink;
    }

    @Override
    public int getExpectedBytes() {
        return underlying.getExpectedBytes();
    }

    @Nullable
    @Override
    public DeserializationChain consume(ByteBuffer buf, int length) {
        underlying.consume(buf, length);
        sink.consume(buf, length);
        return next;
    }

}
