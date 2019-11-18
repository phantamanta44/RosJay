package xyz.phanta.rosjay.tcpros.client;

import xyz.phanta.rosjay.node.NodeTransportManager;
import xyz.phanta.rosjay.tcpros.stator.TcpStateMachine;
import xyz.phanta.rosjay.transport.srv.RosServiceType;
import xyz.phanta.rosjay.util.id.RosId;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

class ExpectServiceOkayByte implements TcpStateMachine.State {

    private final NodeTransportManager transMan;
    private final RosId serviceId;
    private final RosServiceType<?, ?> srvType;

    ExpectServiceOkayByte(NodeTransportManager transMan, RosId serviceId, RosServiceType<?, ?> srvType) {
        this.transMan = transMan;
        this.serviceId = serviceId;
        this.srvType = srvType;
    }

    @Override
    public int getExpectedBytes() {
        return 1;
    }

    @Nullable
    @Override
    public TcpStateMachine.State consume(ByteBuffer buf) {
        return buf.get() != 1 ? ExpectErrorString.expect(err -> transMan.notifyServiceConnectionKilled(serviceId, err))
                : ExpectDeserializerChain.expect(srvType.getResponseType(),
                res -> transMan.notifyReceivedServiceResponse(serviceId, res));
    }

}
