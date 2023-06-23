package demo;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.lineSeparator;
import static java.net.http.HttpClient.Redirect.NEVER;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

public class StressTest {
    static final URI serverUri = URI.create("http://localhost:7070");
    static final Duration timeout = Duration.ofSeconds(10);
    static final ThreadFactory virtualThreadFactory = Thread.ofVirtual().factory();
    static final HttpClient httpClient = HttpClient.newBuilder().version(HTTP_1_1).followRedirects(NEVER).connectTimeout(timeout).build();
    static final AtomicInteger totalRequests = new AtomicInteger();
    static final int numberOfRequestsPerSecond = 1000;
    static final int delayBetweenRequests = 1_000_000 / (numberOfRequestsPerSecond * 2); // Small delay between each request to spread out the load.

    @Test
    void stress_test() {
        final int numberOfTestRuns = 1;
        System.out.println("Number of test runs: " + numberOfTestRuns);
        System.out.println("Requests/sec: " + numberOfRequestsPerSecond + lineSeparator());

        for (int i = 0; i < numberOfTestRuns; i++) {
            System.out.println("Start run " + (i + 1));

            try (final var jobScheduler = newScheduledThreadPool(1, virtualThreadFactory)) {
                jobScheduler.scheduleAtFixedRate(StressTest::executeRequests, 1, 1, SECONDS); // Starts one thread every second until the try-block is exited.
                waitAndPrint(5);
            }

            if ((i + 1) < numberOfTestRuns) {
                System.out.println("Pausing..."); // Allow the jit compiler and garbage collector to settle down after runs.
                waitAndPrint(3);
            }
        }

        System.out.println("Total requests: " + totalRequests.get());
    }

    // This method is run once a second in the job scheduler above.
    static void executeRequests() {
        for (int i = 0; i < numberOfRequestsPerSecond; i++) {
            virtualThreadFactory.newThread(() -> {
                try {
                    totalRequests.incrementAndGet();
                    final var request = HttpRequest.newBuilder().uri(serverUri).timeout(timeout).GET().build();
                    final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200)
                        System.err.println("Error: " + response.statusCode());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            try {
                Thread.sleep(Duration.of(delayBetweenRequests, MICROS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static void waitAndPrint(final long seconds) {
        System.out.println("Waiting " + seconds + " seconds...");
        final long start = System.currentTimeMillis();
        final long end = start + seconds * 1000;
        int i = 0;
        while (System.currentTimeMillis() < end) {
            System.out.println(++i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
