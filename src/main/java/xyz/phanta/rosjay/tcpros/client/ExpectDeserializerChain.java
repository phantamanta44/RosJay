package xyz.phanta.rosjay.tcpros.client;

import xyz.phanta.rosjay.tcpros.stator.ExpectDatagramLength;
import xyz.phanta.rosjay.tcpros.stator.TcpStateMachine;
import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.data.RosDataType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Supplier;

class ExpectDeserializerChain<T extends RosData<T>> implements TcpStateMachine.State {

    static <T extends RosData<T>> TcpStateMachine.State expect(RosDataType<T> type, Consumer<T> callback) {
        return new ExpectDatagramLength(len -> {
            T dataInstance = type.newInstance();
            return getChainLink(dataInstance, dataInstance.linkDeserializer(null), callback, () -> expect(type, callback));
        });
    }

    private static <T extends RosData<T>> TcpStateMachine.State getChainLink(T dataInstance,
                                                                             DeserializationChain deserializer,
                                                                             Consumer<T> callback,
                                                                             Supplier<TcpStateMachine.State> transitionFactory) {
        int expLen = deserializer.getExpectedBytes();
        return expLen > 0 ? new ExpectDeserializerChain<>(dataInstance, deserializer, callback, expLen, transitionFactory)
                : new ExpectDatagramLength(len -> new ExpectDeserializerChain<>(
                dataInstance, deserializer, callback, len, transitionFactory));
    }

    private final T dataInstance;
    private final DeserializationChain deserializer;
    private final Consumer<T> callback;
    private final int length;
    private final Supplier<TcpStateMachine.State> transitionFactory;

    private ExpectDeserializerChain(T dataInstance, DeserializationChain deserializer, Consumer<T> callback, int length,
                                    Supplier<TcpStateMachine.State> transitionFactory) {
        this.dataInstance = dataInstance;
        this.deserializer = deserializer;
        this.callback = callback;
        this.length = length;
        this.transitionFactory = transitionFactory;
    }

    @Override
    public int getExpectedBytes() {
        return length;
    }

    @Nullable
    @Override
    public TcpStateMachine.State consume(ByteBuffer buf) {
        DeserializationChain next = deserializer.consume(buf, length);
        if (next != null) {
            return getChainLink(dataInstance, next, callback, transitionFactory);
        } else {
            callback.accept(dataInstance);
            return transitionFactory.get();
        }
    }

}
