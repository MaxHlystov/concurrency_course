package course.concurrency.m5_streams;

import java.util.concurrent.*;

public class ThreadPoolTask {

    // Task #1
    public ThreadPoolExecutor getLifoExecutor() {
        return new ThreadPoolExecutor(12, 12,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<Runnable>() {

                    @Override
                    public boolean offer(Runnable e) {
                        return super.offerFirst(e);
                    }

                    @Override
                    public boolean offer(Runnable e, long timeout, TimeUnit unit) throws InterruptedException {
                        return super.offerFirst(e, timeout, unit);
                    }


                    @Override
                    public boolean add(Runnable e) {
                        return super.offerFirst(e);
                    }

                    @Override
                    public void put(Runnable e) throws InterruptedException {
                        super.putFirst(e);
                    }
                });
    }

    // Task #2
    public ThreadPoolExecutor getRejectExecutor() {
        return new ThreadPoolExecutor(8, 8,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.DiscardPolicy());
    }
}
