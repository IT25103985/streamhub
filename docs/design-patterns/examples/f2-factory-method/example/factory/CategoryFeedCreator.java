package example.factory;

import java.util.List;

/**
 * Abstract Creator (Factory Method pattern).
 *
 * Declares the factory method {@code createFeed()} that subclasses override
 * to produce the appropriate {@link CategoryFeed} product.  The template
 * method {@code showFeed()} uses the factory method so the creator never
 * needs to know the concrete product type.
 */
public abstract class CategoryFeedCreator {

    /**
     * Factory Method: subclasses decide which CategoryFeed to instantiate.
     */
    public abstract CategoryFeed createFeed();

    /**
     * Template method that delegates filtering/sorting to the product
     * returned by the factory method.
     *
     * @param all the complete list of videos on the platform
     * @return the feed-specific filtered and sorted list
     */
    public List<VideoSummary> showFeed(List<VideoSummary> all) {
        CategoryFeed feed = createFeed();
        return feed.load(all);
    }
}
