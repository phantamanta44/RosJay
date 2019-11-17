package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationLink;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;

public class FieldTypeDuration implements RosDataFieldType<Duration> {

    public static final FieldTypeDuration TYPE = new FieldTypeDuration();

    private FieldTypeDuration() {
        // NO-OP
    }

    @Override
    public Duration getDefaultValue() {
        return Duration.ZERO;
    }

    @Override
    public void serializeField(Duration value, DataOutput dest, int seqIndex) throws IOException {
        dest.writeInt((int)value.getSeconds());
        dest.writeInt(value.getNano());
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<Duration> callback) {
        // nanos are signed but probably won't be an issue
        return new DeserializationLink(next, 8, (buf, length) -> callback.accept(Duration.ofSeconds(buf.getInt(), buf.getInt())));
    }

    @Override
    public String toString() {
        return "duration";
    }

}
