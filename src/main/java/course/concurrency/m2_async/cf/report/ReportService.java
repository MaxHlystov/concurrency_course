package course.concurrency.m2_async.cf.report;

public interface ReportService {
    Others.Report getReport();

    void shutdown();
}
