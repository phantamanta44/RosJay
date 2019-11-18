package xyz.phanta.rosjay.transport.srv;

import xyz.phanta.rosjay.transport.RosTransport;
import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.util.id.RosId;

public interface RosServiceTransport<REQ extends RosData<REQ>, RES extends RosData<RES>> extends RosTransport {

    RosId getServiceId();

    RosServiceType<REQ, RES> getSrvType();

}
