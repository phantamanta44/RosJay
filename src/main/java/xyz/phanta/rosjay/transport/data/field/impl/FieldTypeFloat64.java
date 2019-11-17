package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationLink;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

public class FieldTypeFloat64 implements RosDataFieldType<Double> {

    public static final FieldTypeFloat64 TYPE = new FieldTypeFloat64();

    private FieldTypeFloat64() {
        // NO-OP
    }

    @Override
    public Double getDefaultValue() {
        return 0D;
    }

    @Override
    public void serializeField(Double value, DataOutput dest, int seqIndex) throws IOException {
        dest.writeDouble(value);
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<Double> callback) {
        return new DeserializationLink(next, 8, ((buf, length) -> callback.accept(buf.getDouble())));
    }

    @Override
    public String toString() {
        return "float64";
    }

}
