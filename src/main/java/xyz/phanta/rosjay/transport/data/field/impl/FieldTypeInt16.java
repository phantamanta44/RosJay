package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationLink;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

public class FieldTypeInt16 implements RosDataFieldType<Short> {

    public static final FieldTypeInt16 TYPE = new FieldTypeInt16();

    private FieldTypeInt16() {
        // NO-OP
    }

    @Override
    public Short getDefaultValue() {
        return 0;
    }

    @Override
    public void serializeField(Short value, DataOutput dest, int seqIndex) throws IOException {
        dest.writeShort(value);
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<Short> callback) {
        return new DeserializationLink(next, 2, ((buf, length) -> callback.accept(buf.getShort())));
    }

    @Override
    public String toString() {
        return "int16";
    }

}
