package xyz.phanta.rosjay.util.deserchain;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public interface DeserializationChain {

    int getExpectedBytes();

    @Nullable
    DeserializationChain consume(ByteBuffer buf, int length);

}
