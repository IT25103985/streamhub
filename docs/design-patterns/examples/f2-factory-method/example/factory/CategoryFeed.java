package example.factory;

import java.util.List;

/**
 * Product interface: defines how a category feed loads and filters videos.
 */
public interface CategoryFeed {

    /**
     * Loads and returns a filtered/sorted view of the given video list.
     *
     * @param all the complete list of videos available on the platform
     * @return the filtered and sorted feed
     */
    List<VideoSummary> load(List<VideoSummary> all);
}
