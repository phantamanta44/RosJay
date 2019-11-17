package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationLink;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

public class FieldTypeInt64 implements RosDataFieldType<Long> {

    public static final FieldTypeInt64 TYPE = new FieldTypeInt64();

    private FieldTypeInt64() {
        // NO-OP
    }

    @Override
    public Long getDefaultValue() {
        return 0L;
    }

    @Override
    public void serializeField(Long value, DataOutput dest, int seqIndex) throws IOException {
        dest.writeLong(value);
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<Long> callback) {
        return new DeserializationLink(next, 8, ((buf, length) -> callback.accept(buf.getLong())));
    }

    @Override
    public String toString() {
        return "int64";
    }

}
