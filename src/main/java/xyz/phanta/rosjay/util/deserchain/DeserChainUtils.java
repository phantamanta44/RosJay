package xyz.phanta.rosjay.util.deserchain;

import javax.annotation.Nullable;

public class DeserChainUtils {

    @Nullable
    public static DeserializationChain iterate(@Nullable DeserializationChain next, int count, IndexedIterationFunction func) {
        while (count > 0) {
            --count;
            next = func.apply(next, count);
        }
        return next;
    }

    @FunctionalInterface
    public interface IndexedIterationFunction {

        @Nullable
        DeserializationChain apply(@Nullable DeserializationChain next, int index);

    }

}
