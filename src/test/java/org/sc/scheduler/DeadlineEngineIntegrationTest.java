package org.sc.scheduler;

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.assertEquals;

public class DeadlineEngineIntegrationTest {

    private static final int ENGINE_THREAD = 4;
    private static final int CLIENT_THREAD = 4;
    private static final int POLL_SIZE = 500;
    private static final int DEADLINE_CNT = 10000;
    private static final int DEADLINE_WINDOW = 10;
    private static final int CLIENT_SLEEP = 10;
    private static final Random r = new Random();
    private static final CountDownLatch startLatch = new CountDownLatch(1);
    private static final CountDownLatch endLatch = new CountDownLatch(CLIENT_THREAD);
    private static final LongAdder polled = new LongAdder();

    @Test
    public void start(){

        run(new DeadlineEngineImpl(ENGINE_THREAD));
    }

    private void run(DeadlineEngine engine){

        Arrays.asList(deadlines(DEADLINE_CNT))
                .forEach(engine::schedule);

        for (int t = 0; t< CLIENT_THREAD; t++) {
            new Thread(task(engine, startLatch, endLatch, t, polled)).start();
        }

        System.out.println("start...");
        long startTime= System.currentTimeMillis();
        startLatch.countDown();

        try {
            endLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("end: " + (System.currentTimeMillis()-startTime) + " ms, polled: " + polled.toString());

        assertEquals(DEADLINE_CNT, polled.intValue());
    }

    private Runnable task(DeadlineEngine engine, CountDownLatch start, CountDownLatch endLatch, int i, LongAdder adder){
        return () -> {
            try {
                start.await();
            }catch(InterruptedException ignored) { }

            while(engine.size() >0){

                int runCnt = engine.poll(System.currentTimeMillis(), (id) -> {}, POLL_SIZE);
                adder.add(runCnt);
                sleep(r.nextInt(CLIENT_SLEEP));

            }

            endLatch.countDown();

        };
    }
    private Long[] deadlines(int total){
        long now = System.currentTimeMillis();

        Long[] deadlines = new Long[total];
        for (int i = 0; i <total; i++){
            deadlines[i] = now + r.nextInt(DEADLINE_WINDOW);
        }
        return deadlines;
    }

    private void sleep(long millis){
        try {
            Thread.sleep(millis);
        }catch (InterruptedException ie){

        }
    }


}
