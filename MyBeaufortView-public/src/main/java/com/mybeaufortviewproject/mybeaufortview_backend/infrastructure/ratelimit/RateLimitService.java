package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.ratelimit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Map<String, List<Long>> requestLogs = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS = 10;

    private static final long WINDOW_MS = 60_000;

    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();

        requestLogs.computeIfAbsent(key, k -> new ArrayList<>());
        List<Long> timestamps = requestLogs.get(key);

        // remove old timestamps
        timestamps.removeIf(ts -> ts < now - WINDOW_MS);

        if (timestamps.size() >= MAX_REQUESTS) {
            return false;
        }

        timestamps.add(now);
        return true;
    }
}
