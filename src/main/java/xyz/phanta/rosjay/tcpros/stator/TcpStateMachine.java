package xyz.phanta.rosjay.tcpros.stator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TcpStateMachine {

    @Nullable
    private State state;
    @SuppressWarnings("NullableProblems")
    private byte[] stage;
    private int stagePointer;

    public TcpStateMachine(State initialState) {
        this.state = initialState;
        reallocStage();
    }

    public boolean accept(InputStream strIn) throws IOException {
        if (state == null) {
            throw new IllegalStateException("State machine is terminated!");
        }
        int readLength = strIn.read(stage, stagePointer, stage.length - stagePointer);
        if (readLength == -1) {
            throw new IllegalStateException("Client socket reached EOF!");
        }
        stagePointer += readLength;
        if (stagePointer >= stage.length) {
            ByteBuffer buf = ByteBuffer.wrap(stage);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            state = state.consume(buf);
            if (state != null) {
                reallocStage();
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean isAlive() {
        return state != null;
    }

    private void reallocStage() {
        if (state == null) {
            throw new IllegalStateException("State machine is terminated!");
        }
        stage = new byte[state.getExpectedBytes()];
        stagePointer = 0;
    }

    public interface State {

        int getExpectedBytes();

        @Nullable
        State consume(ByteBuffer buf);

    }

}
