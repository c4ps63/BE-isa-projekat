package rs.ac.ftn.isa.isabackend.service;

import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class CommentRateLimiterService {

    private static final int MAX_COMMENTS_PER_HOUR = 60;
    private static final long WINDOW_SIZE_MS = TimeUnit.HOURS.toMillis(1);

    private final ConcurrentHashMap<String, Deque<Long>> userCommentTimestamps = new ConcurrentHashMap<>();

    public boolean isAllowed(String username) {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_SIZE_MS;

        Deque<Long> timestamps = userCommentTimestamps.computeIfAbsent(username, k -> new LinkedList<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < MAX_COMMENTS_PER_HOUR) {
                timestamps.addLast(now);
                return true;
            }

            return false;
        }
    }

    public int getRemainingComments(String username) {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_SIZE_MS;

        Deque<Long> timestamps = userCommentTimestamps.get(username);
        if (timestamps == null) return MAX_COMMENTS_PER_HOUR;

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            return Math.max(0, MAX_COMMENTS_PER_HOUR - timestamps.size());
        }
    }
}
