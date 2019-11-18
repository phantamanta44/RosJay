package xyz.phanta.rosjay.transport.data;

import xyz.phanta.rosjay.util.deserchain.DeserializationChain;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;

public interface RosData<T extends RosData<T>> {

    RosDataType<T> retrieveDataType(); // break nomenclature to make sure ros datatype property getters don't conflict

    void serializeData(DataOutput dest, int seqIndex) throws IOException;

    DeserializationChain linkDeserializer(@Nullable DeserializationChain next);

}
