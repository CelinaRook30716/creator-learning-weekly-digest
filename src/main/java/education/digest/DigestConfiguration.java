package education.digest;

public record DigestConfiguration(String apiKey, String taskUrl, String cronExpression) {
    public static DigestConfiguration fromEnvironment() {
        return new DigestConfiguration(
                required("INFRAI_API_KEY"),
                required("DIGEST_TASK_URL"),
                System.getenv().getOrDefault("DIGEST_CRON", "0 9 * * 1"));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + " before starting the service");
        }
        return value;
    }
}
