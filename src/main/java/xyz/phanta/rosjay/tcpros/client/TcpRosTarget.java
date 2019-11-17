package xyz.phanta.rosjay.tcpros.client;

import xyz.phanta.rosjay.tcpros.TcpRosHeader;
import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.transport.spec.DataTypeSpecification;
import xyz.phanta.rosjay.util.id.RosId;

public abstract class TcpRosTarget {

    private final RosId targetId;

    private TcpRosTarget(RosId targetId) {
        this.targetId = targetId;
    }

    public RosId getId() {
        return targetId;
    }

    public abstract void populateHeader(TcpRosHeader header);

    public static class Topic<T extends RosData<T>> extends TcpRosTarget {

        private final RosMessageType<T> msgType;

        public Topic(RosId targetId, RosMessageType<T> msgType) {
            super(targetId);
            this.msgType = msgType;
        }

        RosMessageType<T> getMessageType() {
            return msgType;
        }

        @Override
        public void populateHeader(TcpRosHeader header) {
            header.putField("topic", getId().toString());
            header.putField("type", msgType.getId().toUnrootedString());
            DataTypeSpecification.Source msgSrc = msgType.getDataType().getTypeSpecification().getSource();
            header.putField("message_definition", msgSrc.getNormalizedText());
            header.putField("md5sum", msgSrc.getMd5Sum());
        }

        @Override
        public String toString() {
            return getId() + " (" + msgType.getId().toUnrootedString() + ")";
        }

    }

    // TODO tcpros service target

}
