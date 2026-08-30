package education.digest;

import java.util.List;

public record WeeklyDigestPlan(
        String creatorId,
        int activeSubscribers,
        List<String> processedLessons,
        List<String> readyDownloads) {

    public WeeklyDigestPlan {
        processedLessons = List.copyOf(processedLessons);
        readyDownloads = List.copyOf(readyDownloads);
    }

    public boolean isReadyToTeach() {
        return activeSubscribers > 0 && !processedLessons.isEmpty() && !readyDownloads.isEmpty();
    }

    public int updateCount() {
        return processedLessons.size() + readyDownloads.size();
    }
}
