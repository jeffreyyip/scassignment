package org.sc.scheduler;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * This class is unconditionally thread-safe
 * thread-safe is done by synchronization on internal lock object
 */
public class DeadlineEngineImpl implements DeadlineEngine {

    private final Object lock = new Object();
    private final Comparator<DeadlineEvent> eventComparator = Comparator.comparing(e -> e.deadline);
    private final AtomicLong atomicLong = new AtomicLong();
    private final PriorityQueue<DeadlineEvent> events = new PriorityQueue<>(11, eventComparator);
    private final ExecutorService executorService;

    /**
     * Constructor
     * @param threadCnt - number of threads to execute provided handler in {@link #poll(long, Consumer, int) poll} method
     */
    public DeadlineEngineImpl(int threadCnt) {
        this.executorService = Executors.newFixedThreadPool(threadCnt);
        shutdownHook();
    }

    /**
     * to schedule the deadline
     * @param deadlineMs the millis
     * @return id of scheduled deadline, it is only valid per instance
     */
    @Override
    public long schedule(long deadlineMs) {
        DeadlineEvent event = new DeadlineEvent(atomicLong.getAndIncrement(), deadlineMs);
        synchronized (lock) {
            events.add(event);
        }
        return event.id;

    }

    @Override
    public boolean cancel(long requestId) {
        DeadlineEvent event = new DeadlineEvent(requestId, 0);
        synchronized (lock) {
            return events.remove(event);
        }
    }

    @Override
    public int poll(long nowMs, Consumer<Long> handler, int maxPoll) {

        DeadlineEvent event;
        List<DeadlineEvent> eventList = new ArrayList<>(maxPoll);

        synchronized (lock) {
            while (eventList.size() < maxPoll) {
                event = events.peek();

                if (Objects.isNull(event) || event.deadline > nowMs) {
                    break;
                }

                eventList.add(events.poll());
            }
        }

        handleEvents(eventList, handler);

        return eventList.size();

    }

    @Override
    public int size() {
        synchronized (lock) {
            return events.size();
        }
    }

    private void handleEvents(List<DeadlineEvent> events, Consumer<Long> handler){
        events.forEach( event ->
            executorService.submit(() -> handler.accept(event.id))
        );
    }

    private void shutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            try{
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)){
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS)){
                        System.err.println("Executor Service did not terminated");
                    }
                }
            }catch (InterruptedException ie){
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    private static class DeadlineEvent {
        private final long id;
        private final long deadline;

        public DeadlineEvent(long id, long deadline){
            this.id = id;
            this.deadline = deadline;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeadlineEvent event = (DeadlineEvent) o;
            return id == event.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "DeadlineEvent{" +
                    "id=" + id +
                    ", deadline=" + deadline +
                    '}';
        }
    }

}
