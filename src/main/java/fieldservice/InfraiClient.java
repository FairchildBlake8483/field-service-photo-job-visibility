package fieldservice;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small REST client shared by the queue and observability calls. */
public final class InfraiClient {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern ERROR = Pattern.compile("\\\"error\\\"\\s*:\\s*(\\{.*?\\}|null)", Pattern.DOTALL);
    private final InfraiConfig config;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public InfraiClient(InfraiConfig config) {
        this.config = config;
    }

    public String createPhotoDispatchCron(String cronExpression) throws IOException, InterruptedException {
        return request("POST", "/v1/cron/create", jsonObject(
                "cron_expr", cronExpression, "task", config.webhookUrl()), true);
    }

    public String listCronRuns(String jobId) throws IOException, InterruptedException {
        return request("GET", "/v1/cron/runs/list/" + jobId, null, false);
    }

    public String publishWorkOrderPhoto(WorkOrderPhoto photo) throws IOException, InterruptedException {
        String event = jsonObject("work_order", photo.workOrder(), "photo", photo.photoReference(),
                "dispatch_status", photo.dispatchStatus());
        return request("POST", "/v1/queue/publish",
                jsonObject("queue", "photo-dispatch", "payload", event), true);
    }

    public String listPhotoDispatchDeadLetters(String queue) throws IOException, InterruptedException {
        return request("GET", "/v1/queue/dlq/list/" + queue, null, false);
    }

    public String captureDispatchException(String exception) throws IOException, InterruptedException {
        return request("POST", "/v1/errors/capture", jsonObject("exception", exception), true);
    }

    public String listCapturedErrors() throws IOException, InterruptedException {
        return request("GET", "/v1/errors/groups", null, false);
    }

    private String request(String method, String path, String body, boolean write)
            throws IOException, InterruptedException {
        String idempotencyKey = write ? UUID.randomUUID().toString() : null;
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "application/json");
            if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey);
            if (body != null) builder.header("Content-Type", "application/json");
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Envelope envelope = Envelope.parse(response.body()); // Decode the API result before status handling.
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep(retryDelay(response, attempt));
                continue;
            }
            if (!envelope.ok()) throw new InfraiException(envelope.error());
            if (response.statusCode() >= 500) throw new IOException("Infrai request could not complete.");
            return response.body();
        }
        throw new IOException("Request retry budget exhausted.");
    }

    private long retryDelay(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try { return Long.parseLong(retryAfter) * 1000L; }
        catch (NumberFormatException ignored) { return 250L * (1L << attempt); }
    }

    private static String jsonObject(String... values) {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < values.length; i += 2) {
            if (i > 0) json.append(',');
            json.append('"').append(values[i]).append("\":\"")
                    .append(escape(values[i + 1])).append('"');
        }
        return json.append('}').toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private record Envelope(boolean ok, String error) {
        static Envelope parse(String body) throws IOException {
            Matcher ok = OK.matcher(body);
            if (!ok.find()) throw new IOException("Response did not contain an Infrai envelope.");
            Matcher error = ERROR.matcher(body);
            return new Envelope(Boolean.parseBoolean(ok.group(1)), error.find() ? error.group(1) : "{}" );
        }
    }

    public static final class InfraiException extends IOException {
        InfraiException(String error) { super("Infrai rejected the request: " + error); }
    }
}
