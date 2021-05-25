package org.sc.caching;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.*;
import java.util.function.Function;

import static org.junit.Assert.*;

public class CacheImplTest {

    @Test
    public void getLong() {
        Cache<Long, Long> cache = new CacheImpl<>(l -> l * 2);

        long rtn = cache.get(3L);
        System.out.println(rtn);
    }

    @Test
    public void getString() {
        Cache<String, String> cache = new CacheImpl<>(s -> s.toUpperCase());

        String rtn = cache.get("abc");
        System.out.println(rtn);
    }


    @Test(expected = IllegalArgumentException.class)
    public void NullKey(){
        Cache<String, String> cache = new CacheImpl<>(s -> s.toUpperCase());

        String rtn = cache.get(null);
        System.out.println(rtn);

    }
}