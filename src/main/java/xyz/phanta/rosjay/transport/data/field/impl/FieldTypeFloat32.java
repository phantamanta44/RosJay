package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationLink;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

public class FieldTypeFloat32 implements RosDataFieldType<Float> {

    public static final FieldTypeFloat32 TYPE = new FieldTypeFloat32();

    private FieldTypeFloat32() {
        // NO-OP
    }

    @Override
    public Float getDefaultValue() {
        return 0F;
    }

    @Override
    public void serializeField(Float value, DataOutput dest, int seqIndex) throws IOException {
        dest.writeFloat(value);
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<Float> callback) {
        return new DeserializationLink(next, 4, ((buf, length) -> callback.accept(buf.getFloat())));
    }

    @Override
    public String toString() {
        return "float32";
    }

}
