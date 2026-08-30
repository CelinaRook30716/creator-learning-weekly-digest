package education.digest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.HexFormat;

public final class CreatorDigestScheduler {
    private final DigestConfiguration configuration;
    private final InfraiCronClient cronClient;
    private final Clock clock;

    public CreatorDigestScheduler(
            DigestConfiguration configuration, InfraiCronClient cronClient, Clock clock) {
        this.configuration = configuration;
        this.cronClient = cronClient;
        this.clock = clock;
    }

    public ScheduleResult schedule(WeeklyDigestPlan plan) throws IOException, InterruptedException {
        if (!plan.isReadyToTeach()) {
            return new ScheduleResult(false, "Digest held until subscribers, lessons, and downloads are ready");
        }
        String key = stableKey(plan.creatorId(), configuration.taskUrl(),
                configuration.cronExpression(), LocalDate.now(clock));
        String jobId = cronClient.create(configuration.cronExpression(), configuration.taskUrl(), key);
        return new ScheduleResult(true,
                "Scheduled " + plan.updateCount() + " learning updates as job " + jobId);
    }

    static String stableKey(String creatorId, String taskUrl, String cronExpression, LocalDate date) {
        int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        String source = creatorId + ":" + taskUrl + ":" + cronExpression
                + ":" + date.getYear() + ":" + week;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return "creator-digest-" + HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record ScheduleResult(boolean scheduled, String message) {}
}
