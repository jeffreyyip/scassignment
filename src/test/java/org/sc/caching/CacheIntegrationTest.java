package org.sc.caching;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class CacheIntegrationTest {

    private final int THREAD_CNT = 3;

    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_CNT);
    private Cache<Long, Long> cache;
    private final CountDownLatch endLatch = new CountDownLatch(THREAD_CNT);
    private final AtomicInteger runCnt = new AtomicInteger();

    @Test
    public void start() throws InterruptedException, ExecutionException {
        cache = new CacheImpl<>(func());
        AtomicLong key = new AtomicLong(4L);
        for(int i = 0; i< THREAD_CNT; i++){
                new Thread(() -> {
                    try {
                        executorService.submit(task(key.get())).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    } finally {
                        endLatch.countDown();
                    }

                }).start();
        }

        endLatch.await();

        // Run single thread
        try {
            Long rtn = executorService.submit(task(key.get())).get();
            System.out.println(Thread.currentThread().getName() + " " + rtn);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
        executorService.awaitTermination(500, TimeUnit.MILLISECONDS);

        assertEquals(1, runCnt.get());
        System.out.println("going to end");
    }

    private Callable<Long> task(Long key){
        return () -> {
            try {
                Long rtn = cache.get(key);
                System.out.println(Thread.currentThread().getName() + " return " + rtn);
                return rtn;
            }catch(Throwable e){
                System.out.println(Thread.currentThread().getName() + " Cache throws exception " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        };
    }

    private Function<Long, Long> func(){
        return l -> {
            try {
                sleep(5000);
                System.out.println(Thread.currentThread().getName() + " running with input " + l);

                return l * l / l;
            }finally{
                runCnt.incrementAndGet();
            }
        };
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }catch (InterruptedException ie){
            Thread.currentThread().interrupt();
        }
    }
}
