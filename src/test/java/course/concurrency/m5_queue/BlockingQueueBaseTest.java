package course.concurrency.m5_queue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

abstract class BlockingQueueBaseTest {

    protected final static int cores = Runtime.getRuntime().availableProcessors();

    protected static final Logger log = Logger.getLogger(BlockingQueueBaseTest.class.getName());

    protected abstract BlockingQueue<Object> getQueue(int maxSize);
    
    @Test
    void addOneItem_isOk() throws InterruptedException {
        BlockingQueue<Object> queue = new BlockingQueueOneLock<>(5);
        final Object value = new Object();
        queue.enqueue(value);
        assertEquals(value, queue.dequeue());
    }

    @Test
    void addOneItem_sizeIsOne() throws InterruptedException {
        BlockingQueue<Object> queue = new BlockingQueueOneLock<>(5);
        final Object value = new Object();
        queue.enqueue(value);
        assertEquals(1, queue.getSize());
    }

    @Test
    void addOneItem_dequeueIsOk() throws InterruptedException {
        BlockingQueue<Object> queue = new BlockingQueueOneLock<>(5);
        for (int i = 0; i < 3; ++i) {
            final Object value = new Object();
            queue.enqueue(value);
            assertEquals(value, queue.dequeue());
            assertEquals(0, queue.getSize());
        }
    }

    @Test
    void addSeveralItems_dequeueSavesOrder() throws InterruptedException {
        final int size = 3;
        BlockingQueue<Object> queue = new BlockingQueueOneLock<>(5);
        final Object[] values = getValues(size);
        AtomicBoolean threadFinised = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            for (int i = 0; i < size; ++i) {
                try {
                    queue.enqueue(values[i]);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            threadFinised.set(true);
        });
        thread.start();
        thread.join(1000);
        assertTrue(threadFinised.get());
        for (int i = 0; i < size; ++i) {
            assertEquals(values[i], queue.dequeue());
        }
    }

    @Test
    void addSeveralItems_sizeIsOk() throws InterruptedException {
        final int size = 3;
        BlockingQueue<Object> queue = new BlockingQueueOneLock<>(5);
        final Object[] values = getValues(size);
        AtomicBoolean threadFinised = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            for (int i = 0; i < size; ++i) {
                try {
                    queue.enqueue(values[i]);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            threadFinised.set(true);
        });
        thread.start();
        thread.join(1000);
        assertTrue(threadFinised.get());
        assertEquals(size, queue.getSize());
    }

    @Test
    void addTwoManyItems_blocksQueue() throws InterruptedException {
        final int maxSize = 5;
        final int size = maxSize + 1;
        BlockingQueue<Object> queue = new BlockingQueueOneLock<>(maxSize);
        final Object[] values = getValues(size);
        AtomicBoolean threadFinished = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            for (int i = 0; i < size; ++i) {
                try {
                    queue.enqueue(values[i]);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            threadFinished.set(true);
        });
        thread.start();
        thread.join(1000);
        assertFalse(threadFinished.get());
        assertEquals(maxSize, queue.getSize());
    }

    @Test
    void dequeueOfFullQueue_unblocksQueue() throws InterruptedException {
        final int maxSize = 5;
        final int size = maxSize + 2;
        BlockingQueue<Object> queue = new BlockingQueueOneLock<>(maxSize);
        final Object[] values = getValues(size);
        AtomicBoolean threadFinished = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            for (int i = 0; i < size; ++i) {
                try {
                    queue.enqueue(values[i]);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            threadFinished.set(true);
        });
        thread.start();
        thread.join(500);
        assertFalse(threadFinished.get());
        assertEquals(values[0], queue.dequeue());
        assertEquals(values[1], queue.dequeue());

        thread.join(500);
        assertTrue(threadFinished.get());
        for (int i = 2; i < size; ++i) {
            assertEquals(values[i], queue.dequeue());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "1,1",
            "1,10",
            "10,1",
            "10,10",
            "1,100",
            "100,1",
            "100,100",
    })
    void severalWriteSeveralRead_isOk(final int writersNumber, final int readersNumber) {
        log.info("Writers " + writersNumber + " readers " + readersNumber);
        final int maxSize = 10;
        final int size = maxSize * 100_000;
        final int writerBatchSize = size / writersNumber;
        final int readerBatchSize = size / readersNumber;
        final BlockingQueue<Object> queue = new BlockingQueueOneLock<>(maxSize);
        final Object[] values = getValues(size);
        final Map<Object, AtomicBoolean> valueWasDequeued = Arrays.stream(values)
                .collect(Collectors.toMap(Function.identity(), (ignored) -> new AtomicBoolean(false)));
        final AtomicBoolean[] writeThreadFinished = getFlagsArray(writersNumber, false);
        final AtomicBoolean[] readThreadFinished = getFlagsArray(readersNumber, false);
        final CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(cores * (writersNumber + readersNumber));

        final List<Future<?>> writers = createTasks(
                executor,
                writersNumber,
                (int taskIdx) -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    processBatch(
                            taskIdx, writersNumber, writerBatchSize, size,
                            (valueIdx) -> {
                                try {
                                    queue.enqueue(values[valueIdx]);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                    writeThreadFinished[taskIdx].set(true);
                });
        final List<Future<?>> readers = createTasks(
                executor,
                readersNumber,
                (int readerIdx) -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    processBatch(
                            readerIdx, readersNumber, readerBatchSize, size,
                            (valueIdx) -> {
                                try {
                                    Object value = queue.dequeue();
                                    AtomicBoolean wasQueued = valueWasDequeued.get(value);
                                    if (wasQueued != null) {
                                        wasQueued.set(true);
                                    } else {
                                        throw new IllegalStateException("Got not expected value ");
                                    }
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                    readThreadFinished[readerIdx].set(true);
                });
        log.info("Start working");
        latch.countDown();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.MINUTES)) {
                log.info("It has been broken by timeout.");
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            log.info("It has been interrupted.");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("End working");

        for (int i = 0; i < writersNumber; ++i) {
            boolean finished = writeThreadFinished[i].get();
            assertTrue(finished, "Thread " + i + " not finished");
        }
        for (int i = 0; i < readersNumber; ++i) {
            boolean finished = readThreadFinished[i].get();
            assertTrue(finished);
        }
        for (int i = 0; i < size; ++i) {
            boolean wasDequeued = valueWasDequeued.get(values[i]).get();
            assertTrue(wasDequeued);
        }

    }

    private List<Future<?>> createTasks(ExecutorService executor, int size, IntConsumer task) {
        return IntStream.range(0, size)
                .mapToObj(threadIdx -> (Runnable) () -> task.accept(threadIdx))
                .map(executor::submit)
                .collect(Collectors.toList());
    }

    private static AtomicBoolean[] getFlagsArray(int size, boolean defaultValue) {
        final AtomicBoolean[] array = new AtomicBoolean[size];
        for (int i = 0; i < size; ++i) {
            array[i] = new AtomicBoolean(defaultValue);
        }
        return array;
    }

    private Object[] getValues(int size) {
        final Object[] values = new Object[size];
        for (int i = 0; i < size; ++i) {
            values[i] = new Object();
        }
        return values;
    }

    private static void processBatch(int taskIdx, int tasksNumber, int batchSize, int size, IntConsumer processor) {
        int start = taskIdx * batchSize;
        int end = taskIdx == tasksNumber - 1
                ? size
                : start + batchSize;
        for (int valueIdx = start; valueIdx < end; ++valueIdx) {
            processor.accept(valueIdx);
        }
    }
}