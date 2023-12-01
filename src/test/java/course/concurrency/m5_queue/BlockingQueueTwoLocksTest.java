package course.concurrency.m5_queue;

class BlockingQueueTwoLocksTest extends BlockingQueueBaseTest {

    @Override
    protected BlockingQueue<Object> getQueue(int maxSize) {
        return new BlockingQueueTwoLocks<>(maxSize);
    }
}