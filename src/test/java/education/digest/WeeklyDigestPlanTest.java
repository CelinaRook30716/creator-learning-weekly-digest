package education.digest;

import java.time.LocalDate;
import java.util.List;

public final class WeeklyDigestPlanTest {
    public static void main(String[] args) {
        WeeklyDigestPlan complete = new WeeklyDigestPlan(
                "course-studio-42", 12, List.of("Lesson edited"), List.of("workbook.pdf"));
        WeeklyDigestPlan missingDelivery = new WeeklyDigestPlan(
                "course-studio-42", 12, List.of("Lesson edited"), List.of());

        check(complete.isReadyToTeach(), "a complete learning update should be scheduled");
        check(complete.updateCount() == 2, "lesson and download should both appear in the digest");
        check(!missingDelivery.isReadyToTeach(), "a digest without its promised download must wait");

        LocalDate date = LocalDate.of(2026, 8, 29);
        String baseKey = CreatorDigestScheduler.stableKey(
                "course-studio-42", "https://example.com/digest", "0 9 * * 1", date);
        check(!baseKey.equals(CreatorDigestScheduler.stableKey(
                        "course-studio-42", "https://example.com/other", "0 9 * * 1", date)),
                "the task URL must contribute to scheduling identity");
        check(!baseKey.equals(CreatorDigestScheduler.stableKey(
                        "course-studio-42", "https://example.com/digest", "0 10 * * 1", date)),
                "the cron expression must contribute to scheduling identity");
        System.out.println("WeeklyDigestPlanTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
