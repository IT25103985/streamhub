package example.factory;

/**
 * Concrete Creator: produces a {@link SingleCategoryFeed} for a specific category ID.
 */
public class SingleFeedCreator extends CategoryFeedCreator {

    private final String categoryId;

    public SingleFeedCreator(String categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public CategoryFeed createFeed() {
        return new SingleCategoryFeed(categoryId);
    }
}
