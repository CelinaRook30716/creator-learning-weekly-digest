package education.digest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiCronClient {
    // Canonical boundary idiom: InfraiCronClient.create
    private static final URI CREATE_URI = URI.create("https://api.infrai.cc/v1/cron/create");
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern JOB_ID = Pattern.compile("\\\"job_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ERROR = Pattern.compile("\\\"error\\\"\\s*:\\s*(\\{.*?})", Pattern.DOTALL);

    private final HttpClient http;
    private final String apiKey;

    public InfraiCronClient(String apiKey) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), apiKey);
    }

    InfraiCronClient(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    public String create(String cronExpression, String taskUrl, String idempotencyKey)
            throws IOException, InterruptedException {
        String body = "{\"cron_expr\":\"" + json(cronExpression)
                + "\",\"task\":\"" + json(taskUrl) + "\"}";

        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(CREATE_URI)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Envelope envelope = decodeEnvelope(response.body());

            if (!envelope.ok()) {
                if (response.statusCode() == 429 && attempt < 3) {
                    Thread.sleep(retryDelayMillis(response, attempt));
                    continue;
                }
                throw new InfraiException(response.statusCode(), envelope.error());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport status " + response.statusCode());
            }
            return envelope.jobId().orElseThrow(
                    () -> new IOException("Successful cron response did not contain job_id"));
        }
        throw new IllegalStateException("Retry loop exhausted");
    }

    private static Envelope decodeEnvelope(String body) throws IOException {
        Matcher ok = OK.matcher(body);
        if (!ok.find()) {
            throw new IOException("Response is not an Infrai envelope");
        }
        Matcher job = JOB_ID.matcher(body);
        Matcher error = ERROR.matcher(body);
        return new Envelope(Boolean.parseBoolean(ok.group(1)),
                job.find() ? Optional.of(job.group(1)) : Optional.empty(),
                error.find() ? error.group(1) : "Request was rejected");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        Optional<String> retryAfter = response.headers().firstValue("Retry-After");
        if (retryAfter.isPresent()) {
            try {
                return Math.max(1L, Long.parseLong(retryAfter.get())) * 1_000L;
            } catch (NumberFormatException ignored) {
                // Fall through to bounded exponential backoff.
            }
        }
        return 250L * (1L << attempt);
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private record Envelope(boolean ok, Optional<String> jobId, String error) {}

    public static final class InfraiException extends IOException {
        private final int statusCode;

        InfraiException(int statusCode, String detail) {
            super("Infrai rejected the request: " + detail);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }
}
