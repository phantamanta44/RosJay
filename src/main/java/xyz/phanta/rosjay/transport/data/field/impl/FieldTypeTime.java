package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationLink;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;

public class FieldTypeTime implements RosDataFieldType<Instant> {

    public static final FieldTypeTime TYPE = new FieldTypeTime();

    private FieldTypeTime() {
        // NO-OP
    }

    @Override
    public Instant getDefaultValue() {
        return Instant.EPOCH;
    }

    @Override
    public void serializeField(Instant value, DataOutput dest, int seqIndex) throws IOException {
        dest.writeInt((int)value.getEpochSecond());
        dest.writeInt(value.getNano());
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<Instant> callback) {
        // nanos are signed but probably won't be an issue
        return new DeserializationLink(next, 8, (buf, length) -> callback.accept(Instant.ofEpochSecond(buf.getInt(), buf.getInt())));
    }

    @Override
    public String toString() {
        return "time";
    }

}
