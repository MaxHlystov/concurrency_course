package course.concurrency.m5_queue;

class BlockingQueueOneLockTest extends BlockingQueueBaseTest {

    @Override
    protected BlockingQueue<Object> getQueue(int maxSize) {
        return new BlockingQueueOneLock<>(maxSize);
    }
}