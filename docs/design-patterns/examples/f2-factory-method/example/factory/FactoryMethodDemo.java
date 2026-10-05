package example.factory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Demonstrates the GoF Factory Method pattern for the Category module feed selection.
 *
 * <p>Client code works against the abstract {@link CategoryFeedCreator} and
 * product interface {@link CategoryFeed} without depending on concrete classes.</p>
 */
public class FactoryMethodDemo {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println(" StreamHub Category Module - GoF Factory Method Pattern Demo");
        System.out.println("============================================================\n");

        // 1. Prepare sample video catalog
        List<VideoSummary> videos = List.of(
                new VideoSummary("v1", "Spring Boot 3 Deep Dive", "tech",
                        LocalDateTime.now().minusDays(3), 1250),
                new VideoSummary("v2", "Top 10 Indie Games 2026", "gaming",
                        LocalDateTime.now().minusDays(1), 5400),
                new VideoSummary("v3", "Lo-Fi Beats for Coding", "music",
                        LocalDateTime.now().minusHours(8), 8900),
                new VideoSummary("v4", "Design Patterns in Java", "tech",
                        LocalDateTime.now().minusHours(2), 3100),
                new VideoSummary("v5", "Elden Ring Speedrun", "gaming",
                        LocalDateTime.now().minusDays(5), 15000),
                new VideoSummary("v6", "Ambient Chill Guitar", "music",
                        LocalDateTime.now().minusDays(2), 2200)
        );

        System.out.println("Total videos in catalog: " + videos.size());
        System.out.println("------------------------------------------------------------\n");

        // 2. Client interaction via AllFeedCreator (produces AllCategoryFeed)
        CategoryFeedCreator allCreator = new AllFeedCreator();
        renderFeed("Feed: 'ALL' (AllFeedCreator -> AllCategoryFeed)", allCreator, videos);

        // 3. Client interaction via SingleFeedCreator (produces SingleCategoryFeed for 'tech')
        CategoryFeedCreator techCreator = new SingleFeedCreator("tech");
        renderFeed("Feed: 'tech' (SingleFeedCreator -> SingleCategoryFeed)", techCreator, videos);

        // 4. Client interaction via SingleFeedCreator (produces SingleCategoryFeed for 'gaming')
        CategoryFeedCreator gamingCreator = new SingleFeedCreator("gaming");
        renderFeed("Feed: 'gaming' (SingleFeedCreator -> SingleCategoryFeed)", gamingCreator, videos);

        System.out.println("============================================================");
        System.out.println(" Demo completed successfully.");
        System.out.println("============================================================");
    }

    /**
     * Client method that relies solely on the abstract CategoryFeedCreator.
     * It does not know or care which concrete feed product is created.
     */
    private static void renderFeed(String label, CategoryFeedCreator creator, List<VideoSummary> catalog) {
        System.out.println(">>> " + label);
        List<VideoSummary> feedVideos = creator.showFeed(catalog);
        for (int i = 0; i < feedVideos.size(); i++) {
            VideoSummary v = feedVideos.get(i);
            System.out.printf("  %d. [%s] %-28s (Cat: %-7s | %d views | %s)%n",
                    i + 1, v.id(), v.title(), v.categoryId(), v.views(), v.createdAt());
        }
        System.out.println();
    }
}
