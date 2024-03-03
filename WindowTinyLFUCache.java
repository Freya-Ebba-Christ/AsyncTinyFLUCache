/**
"""
Created on 29.01.2024

Copyright (C) <2024>  <Freya Ebba Christ>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

@author: Freya Ebba Christ
"""
**/

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class CacheEntry {
    String key;
    String value;
    int frequency;

    public CacheEntry(String key, String value) {
        this.key = key;
        this.value = value;
        this.frequency = 1;
    }

    public void incrementFrequency() {
        frequency++;
    }
}

public class AsyncWindowTinyLFUCache {
    private final int windowSize;
    private final Map<String, CacheEntry> cache;
    private final ExecutorService evictionExecutor;

    public AsyncWindowTinyLFUCache(int windowSize) {
        this.windowSize = windowSize;
        this.cache = new LinkedHashMap<>(windowSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > windowSize;
            }
        };
        this.evictionExecutor = Executors.newSingleThreadExecutor();
    }

    public synchronized String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            synchronized (cache) {
                entry.incrementFrequency();
            }
            return entry.value;
        }
        return null;
    }

    public synchronized void put(String key, String value) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.value = value;
            entry.incrementFrequency();
        } else {
            if (cache.size() >= windowSize) {
                asyncEvict();
            }
            cache.put(key, new CacheEntry(key, value));
        }
    }

    private void asyncEvict() {
        CompletableFuture.runAsync(this::evict, evictionExecutor);
    }

    private synchronized void evict() {
        int minFrequency = Integer.MAX_VALUE;
        String keyToRemove = null;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            int frequency = entry.getValue().frequency;
            if (frequency < minFrequency) {
                minFrequency = frequency;
                keyToRemove = entry.getKey();
            }
        }

        if (keyToRemove != null) {
            cache.remove(keyToRemove);
        }
    }

    public static void main(String[] args) {
        AsyncWindowTinyLFUCache cache = new AsyncWindowTinyLFUCache(3);

        cache.put("1", "One");
        cache.put("2", "Two");
        cache.put("3", "Three");

        System.out.println(cache.get("2")); // Output: Two
        System.out.println(cache.get("1")); // Output: One

        cache.put("4", "Four");

        System.out.println(cache.get("3")); // Output: null (evicted)

        cache.put("5", "Five");

        System.out.println(cache.get("1")); // Output: null (evicted)
        System.out.println(cache.get("2")); // Output: Two

        cache.evictionExecutor.shutdown();
    }
} 
