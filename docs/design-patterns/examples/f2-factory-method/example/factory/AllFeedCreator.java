package example.factory;

/**
 * Concrete Creator: produces an {@link AllCategoryFeed}.
 */
public class AllFeedCreator extends CategoryFeedCreator {

    @Override
    public CategoryFeed createFeed() {
        return new AllCategoryFeed();
    }
}
