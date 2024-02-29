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

public class WindowTinyLFUCache {
    private final int windowSize;
    private final Map<String, CacheEntry> cache;
    private final ExecutorService evictionExecutor;

    public WindowTinyLFUCache(int windowSize) {
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

        cache.evictionExecutor.shutdown();
    }
} 
