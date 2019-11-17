package xyz.phanta.rosjay.util;

import xyz.phanta.rosjay.util.id.NamespacedMap;
import xyz.phanta.rosjay.util.id.RosId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class BusStateTracker {

    private final NamespacedMap<NamespacedMap<PeerState>> peers = new NamespacedMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void openOutgoing(RosId peerId, RosId targetId) {
        lock.writeLock().lock();
        try {
            peers.computeIfAbsent(peerId, NamespacedMap::new).computeIfAbsent(targetId, PeerState::new).outgoing = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void closeOutgoing(RosId peerId, RosId targetId) {
        decState(peerId, targetId, s -> s.outgoing = false);
    }

    public void openIncoming(RosId peerId, RosId targetId) {
        lock.writeLock().lock();
        try {
            peers.computeIfAbsent(peerId, NamespacedMap::new).computeIfAbsent(targetId, PeerState::new).incoming = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void closeIncoming(RosId peerId, RosId targetId) {
        decState(peerId, targetId, s -> s.incoming = false);
    }

    private void decState(RosId peerId, RosId targetId, Consumer<PeerState> mutator) {
        lock.writeLock().lock();
        try {
            NamespacedMap<PeerState> peerState = peers.get(peerId);
            if (peerState != null) {
                PeerState targetState = peerState.get(targetId);
                if (targetState != null) {
                    mutator.accept(targetState);
                    if (!targetState.isValid()) {
                        peerState.remove(targetId);
                        if (peerState.isEmpty()) {
                            peers.remove(peerId);
                        }
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public State computeState(RosId peerId, RosId targetId) {
        lock.readLock().lock();
        try {
            NamespacedMap<PeerState> peerState = peers.get(peerId);
            if (peerState != null) {
                PeerState targetState = peerState.get(targetId);
                if (targetState != null) {
                    return targetState.computeState();
                }
            }
            return State.NONE;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<Connection> collectStates() {
        lock.readLock().lock();
        try {
            Collection<Connection> result = new ArrayList<>();
            for (Map.Entry<RosId, NamespacedMap<PeerState>> peerEntry : peers.entrySet()) {
                for (Map.Entry<RosId, PeerState> targetEntry : peerEntry.getValue().entrySet()) {
                    result.add(new Connection(peerEntry.getKey(), targetEntry.getKey(), targetEntry.getValue()));
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    private static class PeerState {

        private boolean outgoing = false;
        private boolean incoming = false;

        boolean isValid() {
            return outgoing || incoming;
        }

        State computeState() {
            return incoming ? (outgoing ? State.BOTH : State.INCOMING) : (outgoing ? State.OUTGOING : State.NONE);
        }

    }

    public enum State {

        NONE(""),
        INCOMING("i"),
        OUTGOING("o"),
        BOTH("b");

        public final String symbol;

        State(String symbol) {
            this.symbol = symbol;
        }

        public static State parse(String symbol) {
            switch (symbol) {
                case "i":
                    return INCOMING;
                case "o":
                    return OUTGOING;
                case "b":
                    return BOTH;
                default:
                    return NONE;
            }
        }

    }

    public static class Connection {

        public final RosId peerId;
        public final RosId targetId;
        public final State state;

        Connection(RosId peerId, RosId targetId, PeerState peerState) {
            this.peerId = peerId;
            this.targetId = targetId;
            this.state = peerState.computeState();
        }

    }

}
