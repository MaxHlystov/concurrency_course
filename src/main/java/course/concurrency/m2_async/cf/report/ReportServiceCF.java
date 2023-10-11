package course.concurrency.m2_async.cf.report;

import course.concurrency.m2_async.cf.LoadGenerator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ReportServiceCF implements ReportService {

    private ExecutorService executor;

    private LoadGenerator loadGenerator;

    public ReportServiceCF(ExecutorService executor, LoadGenerator loadGenerator) {
        this.executor = executor;
        this.loadGenerator = loadGenerator;
    }

    @Override
    public Others.Report getReport() {
        CompletableFuture<Collection<Others.Item>> itemsCF =
                CompletableFuture.supplyAsync(() -> getItems(), executor);

        CompletableFuture<Collection<Others.Customer>> customersCF =
                CompletableFuture.supplyAsync(() -> getActiveCustomers(), executor);

        CompletableFuture<Others.Report> reportTask =
                customersCF.thenCombine(itemsCF,
                        (customers, orders) -> combineResults(orders, customers));

        return reportTask.join();
    }

    private Others.Report combineResults(Collection<Others.Item> items, Collection<Others.Customer> customers) {
        return new Others.Report();
    }

    private Collection<Others.Customer> getActiveCustomers() {
        loadGenerator.work();
        loadGenerator.work();
        return List.of(new Others.Customer(), new Others.Customer());
    }

    private Collection<Others.Item> getItems() {
        loadGenerator.work();
        return List.of(new Others.Item(), new Others.Item());
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
