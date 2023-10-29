package course.concurrency.exams.auction;

import java.util.concurrent.*;

public class Notifier {

    private ExecutorService executorService;

    public Notifier() {
        executorService = new ThreadPoolExecutor(4, 2000,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100000000));
    }

    public void sendOutdatedMessage(Bid bid) {
        executorService.execute(this::imitateSending);
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
