package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationLink;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

public class FieldTypeInt8 implements RosDataFieldType<Byte> {

    public static final FieldTypeInt8 TYPE = new FieldTypeInt8();

    private FieldTypeInt8() {
        // NO-OP
    }

    @Override
    public Byte getDefaultValue() {
        return 0;
    }

    @Override
    public void serializeField(Byte value, DataOutput dest, int seqIndex) throws IOException {
        dest.write(value);
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<Byte> callback) {
        return new DeserializationLink(next, 1, ((buf, length) -> callback.accept(buf.get())));
    }

    @Override
    public String toString() {
        return "int8";
    }

}
