package xyz.phanta.rosjay.transport.srv;

import xyz.phanta.rosjay.transport.data.RosData;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface RosServiceClient<REQ extends RosData<REQ>, RES extends RosData<RES>> extends RosServiceTransport<REQ, RES> {

    RES call(REQ request) throws InterruptedException;

    default CompletionStage<RES> callAsync(REQ request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return call(request);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Async service request future thread interrupted!", e);
            }
        });
    }

}
