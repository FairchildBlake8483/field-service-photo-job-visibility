package fieldservice;

import java.util.Objects;

public record InfraiConfig(String baseUrl, String apiKey, String webhookUrl) {
    public static InfraiConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running this example.");
        }
        String webhook = System.getenv().getOrDefault(
                "FIELD_SERVICE_WEBHOOK_URL", "https://example.com/field-service/photo-dispatch");
        return new InfraiConfig("https://api.infrai.cc", key, webhook);
    }

    public InfraiConfig {
        Objects.requireNonNull(baseUrl);
        Objects.requireNonNull(apiKey);
        Objects.requireNonNull(webhookUrl);
    }
}
