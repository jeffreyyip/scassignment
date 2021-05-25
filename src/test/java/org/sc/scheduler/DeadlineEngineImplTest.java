package org.sc.scheduler;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DeadlineEngineImplTest {

    private DeadlineEngine engine;

    @Before
    public void setup(){
        engine = new DeadlineEngineImpl(4);
    }

    @Test
    public void schedule() {
        engine.schedule(System.currentTimeMillis());
        assertEquals(1, engine.size());
    }

    @Test
    public void cancel() {
        long id = engine.schedule(System.currentTimeMillis());
        assertEquals(1, engine.size());
        engine.cancel(id);
        assertEquals(0, engine.size());
    }

    @Test
    public void poll() {
        engine.schedule(System.currentTimeMillis());
        engine.schedule(System.currentTimeMillis());
        engine.schedule(System.currentTimeMillis());

        long cnt = engine.poll(System.currentTimeMillis(), System.out::println, 5);
        assertEquals(3, cnt);
    }

    @Test
    public void size() {

        assertEquals(0, engine.size());

    }
}