package course.concurrency.m5_queue;

public interface BlockingQueue<T> {
    void enqueue(T value) throws InterruptedException;

    T dequeue() throws InterruptedException;

    int getSize();

    int getMaxSize();
}
