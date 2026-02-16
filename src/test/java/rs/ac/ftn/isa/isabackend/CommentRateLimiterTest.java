package rs.ac.ftn.isa.isabackend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rs.ac.ftn.isa.isabackend.service.CommentRateLimiterService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CommentRateLimiterTest {

    private CommentRateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new CommentRateLimiterService();
    }

    @Test
    @DisplayName("Dozvoljava 60 komentara po korisniku u sat vremena")
    void allowsUpTo60CommentsPerHour() {
        String username = "testUser";

        for (int i = 1; i <= 60; i++) {
            assertTrue(rateLimiterService.isAllowed(username),
                    "Komentar #" + i + " trebao je biti dozvoljen");
        }

        assertEquals(0, rateLimiterService.getRemainingComments(username));
    }

    @Test
    @DisplayName("Blokira 61. komentar istog korisnika")
    void blocks61stComment() {
        String username = "testUser";

        for (int i = 0; i < 60; i++) {
            rateLimiterService.isAllowed(username);
        }

        assertFalse(rateLimiterService.isAllowed(username),
                "61. komentar trebao je biti blokiran");
    }

    @Test
    @DisplayName("Razliciti korisnici imaju nezavisne limite")
    void differentUsersHaveIndependentLimits() {
        String user1 = "user1";
        String user2 = "user2";

        for (int i = 0; i < 60; i++) {
            rateLimiterService.isAllowed(user1);
        }

        assertFalse(rateLimiterService.isAllowed(user1), "user1 trebao je biti blokiran");
        assertTrue(rateLimiterService.isAllowed(user2), "user2 treba imati svoj nezavisni limit");
    }

    @Test
    @DisplayName("getRemainingComments vraca tacan broj preostalih komentara")
    void remainingCommentsCountIsAccurate() {
        String username = "testUser";

        assertEquals(60, rateLimiterService.getRemainingComments(username));

        for (int i = 0; i < 25; i++) {
            rateLimiterService.isAllowed(username);
        }

        assertEquals(35, rateLimiterService.getRemainingComments(username));
    }

    @Test
    @DisplayName("Simulacija slanja velikog broja komentara - demonstracija rate limitera")
    void simulateMassCommentPosting() {
        String username = "spammer";
        int totalAttempts = 200;
        int allowed = 0;
        int blocked = 0;

        System.out.println("=== SIMULACIJA: " + totalAttempts + " komentara od jednog korisnika ===");

        for (int i = 1; i <= totalAttempts; i++) {
            if (rateLimiterService.isAllowed(username)) {
                allowed++;
            } else {
                blocked++;
                if (blocked == 1) {
                    System.out.println("Prvo blokiranje na pokusaju #" + i);
                }
            }
        }

        System.out.println("Dozvoljeno: " + allowed + ", Blokirano: " + blocked);
        assertEquals(60, allowed, "Tacno 60 komentara treba biti dozvoljeno");
        assertEquals(140, blocked, "Tacno 140 komentara treba biti blokirano");
    }

    @Test
    @DisplayName("Sistem ne degradira pod ekstremnim opterecenjem - konkurentni pristup")
    void systemDoesNotDegradeUnderLoad() throws InterruptedException {
        int numberOfUsers = 100;
        int commentsPerUser = 200;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        AtomicInteger totalAllowed = new AtomicInteger(0);
        AtomicInteger totalBlocked = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();

        System.out.println("=== LOAD TEST: " + numberOfUsers + " korisnika x " + commentsPerUser + " komentara ===");
        long startTime = System.currentTimeMillis();

        for (int u = 0; u < numberOfUsers; u++) {
            final String username = "loadTestUser" + u;
            executor.submit(() -> {
                try {
                    for (int c = 0; c < commentsPerUser; c++) {
                        long reqStart = System.nanoTime();
                        boolean result = rateLimiterService.isAllowed(username);
                        long reqEnd = System.nanoTime();
                        responseTimes.add(reqEnd - reqStart);

                        if (result) {
                            totalAllowed.incrementAndGet();
                        } else {
                            totalBlocked.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;

        // Statistike
        long totalRequests = (long) numberOfUsers * commentsPerUser;
        double avgResponseNs = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxResponseNs = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long p99Index = (long) (responseTimes.size() * 0.99);
        List<Long> sorted = new ArrayList<>(responseTimes);
        sorted.sort(Long::compareTo);
        long p99ResponseNs = sorted.get((int) p99Index);

        System.out.println("Ukupno zahtjeva: " + totalRequests);
        System.out.println("Dozvoljeno: " + totalAllowed.get() + " (ocekivano: " + (numberOfUsers * 60) + ")");
        System.out.println("Blokirano: " + totalBlocked.get());
        System.out.println("Ukupno vrijeme: " + totalTime + " ms");
        System.out.println("Prosjecno vrijeme odgovora: " + (avgResponseNs / 1000) + " μs");
        System.out.println("P99 vrijeme odgovora: " + (p99ResponseNs / 1000) + " μs");
        System.out.println("Max vrijeme odgovora: " + (maxResponseNs / 1000) + " μs");
        System.out.println("Throughput: " + (totalRequests * 1000 / totalTime) + " req/s");

        // Provjere
        assertEquals(numberOfUsers * 60, totalAllowed.get(),
                "Svaki korisnik treba imati tacno 60 dozvoljenih komentara");
        assertTrue(avgResponseNs < 1_000_000,
                "Prosjecno vrijeme odgovora treba biti ispod 1ms, bilo je: " + (avgResponseNs / 1000) + " μs");
        assertTrue(p99ResponseNs < 10_000_000,
                "P99 treba biti ispod 10ms, bilo je: " + (p99ResponseNs / 1000) + " μs");
    }
}
