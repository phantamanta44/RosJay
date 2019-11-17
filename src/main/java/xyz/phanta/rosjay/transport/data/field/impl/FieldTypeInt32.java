package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationLink;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

public class FieldTypeInt32 implements RosDataFieldType<Integer> {

    public static final FieldTypeInt32 TYPE = new FieldTypeInt32();

    private FieldTypeInt32() {
        // NO-OP
    }

    @Override
    public Integer getDefaultValue() {
        return 0;
    }

    @Override
    public void serializeField(Integer value, DataOutput dest, int seqIndex) throws IOException {
        dest.writeInt(value);
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<Integer> callback) {
        return new DeserializationLink(next, 4, ((buf, length) -> callback.accept(buf.getInt())));
    }

    @Override
    public String toString() {
        return "int32";
    }

}
