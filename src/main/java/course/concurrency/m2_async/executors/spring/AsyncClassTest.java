package course.concurrency.m2_async.executors.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class AsyncClassTest {

    @Autowired
    public ApplicationContext context;

    @Autowired
    @Lazy
    protected AsyncClassTest self;

    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private Executor executor;

    @Async
    public void runAsyncTask() {
        System.out.println("runAsyncTask: " + Thread.currentThread().getName());
        self.internalTask();
    }

    @Async("threadPoolTaskExecutor")
    public void internalTask() {
        System.out.println("internalTask: " + Thread.currentThread().getName());
    }
}
