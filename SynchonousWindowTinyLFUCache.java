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

public class WindowTinyLFUCache {
    private final int windowSize;
    private final Map<String, CacheEntry> cache;

    public WindowTinyLFUCache(int windowSize) {
        this.windowSize = windowSize;
        this.cache = new LinkedHashMap<>(windowSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > windowSize;
            }
        };
    }

    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.incrementFrequency();
            return entry.value;
        }
        return null;
    }

    public void put(String key, String value) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.value = value;
            entry.incrementFrequency();
        } else {
            if (cache.size() >= windowSize) {
                evict();
            }
            cache.put(key, new CacheEntry(key, value));
        }
    }

    private void evict() {
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
        WindowTinyLFUCache cache = new WindowTinyLFUCache(3);

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
    }
}
