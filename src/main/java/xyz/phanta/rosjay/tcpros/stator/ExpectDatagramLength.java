package xyz.phanta.rosjay.tcpros.stator;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

public class ExpectDatagramLength implements TcpStateMachine.State {

    private final IntFunction<? extends TcpStateMachine.State> transition;

    public ExpectDatagramLength(IntFunction<? extends TcpStateMachine.State> transition) {
        this.transition = transition;
    }

    @Override
    public int getExpectedBytes() {
        return 4;
    }

    @Override
    public TcpStateMachine.State consume(ByteBuffer buf) {
        return transition.apply(buf.getInt());
    }

}
