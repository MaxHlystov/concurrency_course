package course.concurrency.m2_async.cf.min_price;

import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PriceAggregator {

    private static final int MAX_THREADS = 200;
    private static final int MAX_TASKS_IN_QUEUE = 1000;

    private final Executor executor;

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public PriceAggregator() {
        this.executor = new ThreadPoolExecutor(
                50, MAX_THREADS,
                6L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(MAX_TASKS_IN_QUEUE),
                createDefaultFactory(),
                new ThreadPoolExecutor.DiscardOldestPolicy() // prices stale so quickly
        );
    }

    public PriceAggregator(@NonNull Executor executor) {
        this.executor = executor;
    }

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        final List<CompletableFuture<Double>> results = shopIds.stream()
                .map(shopId -> getPriceAsync(itemId, shopId))
                .collect(Collectors.toList());
        try {
            CompletableFuture.allOf(results.toArray(CompletableFuture[]::new))
                    .get(2900, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException | CancellationException ignored) {
        }
        return results.stream()
                .mapToDouble(this::unpackWithoutException)
                .filter(price -> !Double.isNaN(price))
                .min()
                .orElse(Double.NaN);
    }

    private double unpackWithoutException(CompletableFuture<Double> result) {
        if (result.isDone() && !result.isCancelled() && !result.isCompletedExceptionally()) {
            try {
                return result.get();
            } catch (InterruptedException | CancellationException ignored) {

            } catch (Exception e) {
                System.out.println("Log as warning: something went wrong in price retrieval: " + e.getMessage());
            }
        }
        return Double.NaN;
    }

    private CompletableFuture<Double> getPriceAsync(long itemId, long shopId) {
        return CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor);
    }

    private static ThreadFactory createDefaultFactory() {
        return new ThreadFactory() {
            private final AtomicInteger id = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                int threadId = id.incrementAndGet();
                thread.setName("PriceTh-" + threadId);
                return thread;
            }
        };
    }
}
