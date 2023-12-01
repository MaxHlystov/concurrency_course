package course.concurrency.m5_queue;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingQueueTwoLocks<T> implements BlockingQueue<T> {

    private final int maxSize;
    private Node<T> tail;
    private Node<T> head;
    private final AtomicInteger size = new AtomicInteger(0);
    private final ReentrantLock tailLock = new ReentrantLock();
    private final ReentrantLock headLock = new ReentrantLock();
    private final Condition queueIsNotFull = tailLock.newCondition();
    private final Condition queueIsNotEmpty = headLock.newCondition();

    public BlockingQueueTwoLocks(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void enqueue(T value) throws InterruptedException {
        try {
            tailLock.lock();
            while (size.get() == getMaxSize()) {
                queueIsNotFull.await();
            }
            final Node<T> newNode = new Node<>(value);
            if (tail != null) {
                tail.setTail(newNode);
            }
            tail = newNode;
            if (head == null) {
                try {
                    // We always prevent order of locking: tailLock -> headLock.
                    // In dequeue() we use only headLock.
                    headLock.lock();
                    head = newNode;
                    queueIsNotEmpty.signal();
                } finally {
                    headLock.unlock();
                }
                if (size.incrementAndGet() < maxSize - 1) {
                    queueIsNotFull.signal();
                }
            }
        } finally {
            tailLock.unlock();
        }
        if (size.get() > 0) {
            signal(headLock, queueIsNotEmpty);
        }
    }

    @Override
    public T dequeue() throws InterruptedException {
        final T value;
        try {
            headLock.lock();
            while (size.get() == 0) {
                queueIsNotEmpty.await();
            }
            final Node<T> headNode = head;
            value = headNode.getValue();
            head = headNode.getTail();
            if (size.decrementAndGet() > 0) {
                queueIsNotEmpty.signal();
            }
        } finally {
            headLock.unlock();
        }
        if (size.get() < maxSize - 1) {
            signal(tailLock, queueIsNotFull);
        }
        return value;
    }

    @Override
    public int getSize() {
        return this.size.get();
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    private void signal(Lock lock, Condition condition) throws InterruptedException {
        try {
            lock.lockInterruptibly();
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    private static class Node<T> {
        private final T value;
        private Node<T> tail;

        protected Node(T value) {
            this.value = value;
        }

        protected T getValue() {
            return this.value;
        }

        public Node<T> getTail() {
            return tail;
        }

        public void setTail(Node<T> tail) {
            if (this.tail != null) {
                throw new IllegalStateException("Tail for node " + this + " have already set.");
            }
            this.tail = tail;
        }
    }
}
