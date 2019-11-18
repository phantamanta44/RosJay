package xyz.phanta.rosjay.transport.srv;

import xyz.phanta.rosjay.transport.data.RosData;

import javax.annotation.Nullable;
import java.util.function.Function;

public interface RosServiceProvider<REQ extends RosData<REQ>, RES extends RosData<RES>> extends RosServiceTransport<REQ, RES> {

    void setHandler(@Nullable Function<REQ, RES> handler);

    boolean hasHandler();

}
