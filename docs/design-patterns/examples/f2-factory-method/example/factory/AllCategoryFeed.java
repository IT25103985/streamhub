package example.factory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Concrete Product: returns ALL videos sorted newest-first.
 * Mirrors the StreamHub rule that the "All" category feed is strictly
 * chronological with no boosting or filtering.
 */
public class AllCategoryFeed implements CategoryFeed {

    @Override
    public List<VideoSummary> load(List<VideoSummary> all) {
        return all.stream()
                .sorted(Comparator.comparing(VideoSummary::createdAt).reversed())
                .collect(Collectors.toList());
    }
}
