package course.concurrency.m5_queue;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingQueueOneLock<T> {

    private final int maxSize;
    private Node<T> tail;
    private Node<T> head;
    private final AtomicInteger size = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition queueIsNotFull = lock.newCondition();
    private final Condition queueIsNotEmpty = lock.newCondition();

    public BlockingQueueOneLock(int maxSize) {
        this.maxSize = maxSize;
    }

    public void enqueue(T value) throws InterruptedException {
        try {
            lock.lock();
            while (size.get() == getMaxSize()) {
                queueIsNotFull.await();
            }
            final Node<T> newNode = new Node<>(value);
            if (tail != null) {
                tail.setTail(newNode);
            }
            tail = newNode;
            if (head == null) {
                head = newNode;
            }
            size.incrementAndGet();
            queueIsNotEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T dequeue() throws InterruptedException {
        try {
            lock.lock();
            while (size.get() == 0) {
                queueIsNotEmpty.await();
            }
            final Node<T> headNode = head;
            head = headNode.getTail();
            if (head == null) {
                tail = null;
            }
            size.decrementAndGet();
            queueIsNotFull.signal();
            return headNode.getValue();
        } finally {
            lock.unlock();
        }
    }

    public int getSize() {
        return this.size.get();
    }

    public int getMaxSize() {
        return maxSize;
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
