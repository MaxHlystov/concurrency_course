package course.concurrency.m2_async.cf;

import course.concurrency.m2_async.cf.report.ReportService;
import course.concurrency.m2_async.cf.report.ReportServiceCF;
import course.concurrency.m2_async.cf.report.ReportServiceExecutors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ReportServiceTests {

    private ReportService reportService; // = new ReportServiceExecutors();
//    private ReportServiceCF reportService = new ReportServiceCF();
//    private ReportServiceVirtual reportService = new ReportServiceVirtual();


    private static Stream<Arguments> argumentsForReportServiceExecutors() {
        return Stream.of(
                //arguments(ExecutorServiceType.SINGLE, 0, true),
                arguments(ExecutorServiceType.FIXED, 4, true),
                arguments(ExecutorServiceType.FIXED, 8, true),
                arguments(ExecutorServiceType.FIXED, 10, true),
                arguments(ExecutorServiceType.FIXED, 128, true),
                arguments(ExecutorServiceType.FIXED, 12, true),
                arguments(ExecutorServiceType.FIXED, 14, true),
                arguments(ExecutorServiceType.FIXED, 16, true),
                arguments(ExecutorServiceType.FIXED, 32, true),
                arguments(ExecutorServiceType.FIXED, 36, true),
                arguments(ExecutorServiceType.FIXED, 64, true),
                arguments(ExecutorServiceType.FIXED, Integer.MAX_VALUE, true),
                arguments(ExecutorServiceType.CACHED, 0, true),
                arguments(ExecutorServiceType.STEALING, 0, true),

                //arguments(ExecutorServiceType.SINGLE, 0, false),
                arguments(ExecutorServiceType.FIXED, 4, false),
                arguments(ExecutorServiceType.FIXED, 8, false),
                arguments(ExecutorServiceType.FIXED, 10, false),
                arguments(ExecutorServiceType.FIXED, 12, false),
                arguments(ExecutorServiceType.FIXED, 14, false),
                arguments(ExecutorServiceType.FIXED, 16, false),
                arguments(ExecutorServiceType.FIXED, 32, false),
                arguments(ExecutorServiceType.FIXED, 36, false),
                arguments(ExecutorServiceType.FIXED, 64, false),
                arguments(ExecutorServiceType.FIXED, 128, false),
                arguments(ExecutorServiceType.FIXED, Integer.MAX_VALUE, false),
                arguments(ExecutorServiceType.CACHED, 0, false),
                arguments(ExecutorServiceType.STEALING, 0, false)
        );
    }

    @ParameterizedTest
    @MethodSource("argumentsForReportServiceExecutors")
    public void testMultipleTasksWithReportServiceExecutors(
            ExecutorServiceType type,
            int poolSizeForTest,
            boolean isSleep
    ) throws InterruptedException {
        reportService = new ReportServiceExecutors(
                getExecutorService(type, poolSizeForTest),
                new LoadGenerator(isSleep)
        );
        testWithReportService();
    }

    private static Stream<Arguments> argumentsForMultipleTasksWithReportServiceCF() {
        return Stream.of(
                arguments(ExecutorServiceType.COMMON_FORK_JOIN, 0, true)
//                arguments(ExecutorServiceType.FIXED, 4, true),
//                arguments(ExecutorServiceType.FIXED, 8, true),
//                arguments(ExecutorServiceType.FIXED, 10, true),
//                arguments(ExecutorServiceType.FIXED, 12, true),
//                arguments(ExecutorServiceType.FIXED, 14, true),
//                arguments(ExecutorServiceType.FIXED, 16, true),
//                arguments(ExecutorServiceType.FIXED, 32, true),
//                arguments(ExecutorServiceType.FIXED, 36, true),
//                arguments(ExecutorServiceType.FIXED, 64, true),
//                arguments(ExecutorServiceType.FIXED, 128, true),
//                arguments(ExecutorServiceType.FIXED, Integer.MAX_VALUE, true),
//                arguments(ExecutorServiceType.CACHED, 0, true),
//                arguments(ExecutorServiceType.STEALING, 0, true),
//
//                arguments(ExecutorServiceType.COMMON_FORK_JOIN, 0, false),
//                arguments(ExecutorServiceType.FIXED, 4, false),
//                arguments(ExecutorServiceType.FIXED, 8, false),
//                arguments(ExecutorServiceType.FIXED, 10, false),
//                arguments(ExecutorServiceType.FIXED, 12, false),
//                arguments(ExecutorServiceType.FIXED, 14, false),
//                arguments(ExecutorServiceType.FIXED, 16, false),
//                arguments(ExecutorServiceType.FIXED, 32, false),
//                arguments(ExecutorServiceType.FIXED, 36, false),
//                arguments(ExecutorServiceType.FIXED, 64, false),
//                arguments(ExecutorServiceType.FIXED, 128, false),
//                arguments(ExecutorServiceType.FIXED, Integer.MAX_VALUE, false),
//                arguments(ExecutorServiceType.CACHED, 0, false),
//                arguments(ExecutorServiceType.STEALING, 0, false)
        );
    }

    @ParameterizedTest
    @MethodSource("argumentsForMultipleTasksWithReportServiceCF")
    public void testMultipleTasksWithReportServiceCF(
            ExecutorServiceType type,
            int poolSizeForTest,
            boolean isSleep
    ) throws InterruptedException {
        reportService = new ReportServiceCF(
                getExecutorService(type, poolSizeForTest),
                new LoadGenerator(isSleep)
        );
        testWithReportService();
    }

    private void testWithReportService() throws InterruptedException {
        int poolSize = Runtime.getRuntime().availableProcessors() * 3;
        int iterations = 5;

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        System.out.println("Pool size " + poolSize);
        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
                for (int it = 0; it < iterations; it++) {
                    reportService.getReport();
                }
            });
        }

        long start = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(20, TimeUnit.MINUTES);
        long end = System.currentTimeMillis();

        reportService.shutdown();

        System.out.println("Execution time: " + (end - start));
    }

    private ExecutorService getExecutorService(ExecutorServiceType type, int poolSize) {
        switch (type) {
            case FIXED: return Executors.newFixedThreadPool(poolSize);
            case CACHED: return Executors.newCachedThreadPool();
            case SINGLE: return Executors.newSingleThreadExecutor();
            case STEALING: return Executors.newWorkStealingPool();
            case COMMON_FORK_JOIN: return ForkJoinPool.commonPool();
        }
        return Executors.newSingleThreadExecutor();
    }

    public enum ExecutorServiceType {
        SINGLE,
        FIXED,
        CACHED,
        STEALING,
        COMMON_FORK_JOIN
    }
}
