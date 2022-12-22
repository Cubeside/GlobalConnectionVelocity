package de.cubeside.connection.util;

import java.util.concurrent.locks.Lock;

public class AutoCloseableLockWrapper implements AutoCloseable {
    private final Lock lock;

    public AutoCloseableLockWrapper(Lock lock) {
        this.lock = lock;
    }

    public AutoCloseableLockWrapper open() {
        this.lock.lock();
        return this;
    }

    @Override
    public void close() {
        this.lock.unlock();
    }
}