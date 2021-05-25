package org.sc.caching;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;

public class CacheImpl<K,V> implements Cache<K,V>{

    private final ConcurrentMap<K, Future<V>> cache = new ConcurrentHashMap<>();
    private final Function<K, V> function;

    /**
     * Constructor
     * @param function - to be executed when particular Key is requested at the first time by {@link #get(K) get} method
     */
    public CacheImpl(Function<K, V> function) {
        this.function = function;
    }

    /**
     * Get the result for the provided function in constructor.
     *
     * @param key - input for the provided function
     * @return result that is in the cache, or run the provided function to get the result
     *
     * @throws RuntimeException if there is any exception when executing the provided function,
     *                          no result will be cached.
     * @throws IllegalArgumentException if the input key is Null
     */
    @Override
    public V get(K key) {

            raiseExceptionIfNull(key);

            Future<V> future = cache.get(key);

            if (Objects.isNull(future)) {
                FutureTask<V> futureTask = new FutureTask<>(() -> function.apply(key));

                future = cache.putIfAbsent(key, futureTask);

                if (Objects.isNull(future)){
                    future = futureTask;
                    futureTask.run();
                }
            }

            try {
                return future.get();

            } catch (ExecutionException | CancellationException | InterruptedException e){

                cache.remove(key, future);

                throw new RuntimeException(e.getMessage(), e);

            }
    }

    private void raiseExceptionIfNull(K key){
        if (Objects.isNull(key)){
            throw new IllegalArgumentException("Key cannot be null");
        }
    }

}
