package education.digest;

import java.time.Clock;
import java.util.List;

public final class DigestServiceExample {
    private DigestServiceExample() {}

    public static void main(String[] args) throws Exception {
        DigestConfiguration configuration = DigestConfiguration.fromEnvironment();
        WeeklyDigestPlan plan = new WeeklyDigestPlan(
                "course-studio-42",
                860,
                List.of("Lighting a product lesson", "Editing the downloadable workbook"),
                List.of("lighting-checklist.pdf"));

        CreatorDigestScheduler scheduler = new CreatorDigestScheduler(
                configuration, new InfraiCronClient(configuration.apiKey()), Clock.systemUTC());
        CreatorDigestScheduler.ScheduleResult result = scheduler.schedule(plan);
        System.out.println(result.message());
    }
}
