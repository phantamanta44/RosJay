package xyz.phanta.rosjay.util;

import xyz.phanta.rosjay.transport.data.RosData;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RosDataQueue<T extends RosData<T>> {

    private final Entry<T>[] buffer;
    private int fPtr = 0, bPtr = 0;
    private int count = 0, totalOfferCount = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Condition offerAwait = lock.writeLock().newCondition();

    @SuppressWarnings("unchecked")
    public RosDataQueue(int capacity) {
        this.buffer = (Entry<T>[])new Entry[capacity];
    }

    public void offer(T value) {
        lock.writeLock().lock();
        try {
            buffer[fPtr] = new Entry<>(value, totalOfferCount++);
            incrementFront();
            if (count == buffer.length) {
                incrementBack();
            } else {
                ++count;
            }
            offerAwait.signal();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Nullable
    public Entry<T> poll() {
        lock.writeLock().lock();
        try {
            if (count == 0) {
                return null;
            } else {
                --count;
            }
            Entry<T> value = buffer[bPtr];
            buffer[bPtr] = null;
            incrementBack();
            return value;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Entry<T> pollBlocking() throws InterruptedException {
        lock.writeLock().lock();
        try {
            while (count == 0) {
                offerAwait.await();
            }
            return Objects.requireNonNull(poll());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getCount() {
        lock.readLock().lock();
        try {
            return count;
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getCapacity() {
        lock.readLock().lock();
        try {
            return buffer.length;
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getTotalOfferCount() {
        lock.readLock().lock();
        try {
            return totalOfferCount;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void incrementFront() {
        fPtr = (fPtr + 1) % buffer.length;
    }

    private void incrementBack() {
        bPtr = (bPtr + 1) % buffer.length;
    }

    public static class Entry<T extends RosData<T>> {

        private final T value;
        private final int seqIndex;

        Entry(T value, int seqIndex) {
            this.value = value;
            this.seqIndex = seqIndex;
        }

        public T getValue() {
            return value;
        }

        public int getSeqIndex() {
            return seqIndex;
        }

    }

}
