package example.factory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Concrete Product: filters videos to a single category, sorted newest-first.
 */
public class SingleCategoryFeed implements CategoryFeed {

    private final String categoryId;

    public SingleCategoryFeed(String categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public List<VideoSummary> load(List<VideoSummary> all) {
        return all.stream()
                .filter(v -> categoryId.equals(v.categoryId()))
                .sorted(Comparator.comparing(VideoSummary::createdAt).reversed())
                .collect(Collectors.toList());
    }
}
