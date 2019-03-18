public class ItemSimilarityTuple {
    private static ItemSimilarityTuple ourInstance = new ItemSimilarityTuple();

    public static ItemSimilarityTuple getInstance() {
        return ourInstance;
    }

    private ItemSimilarityTuple() {
    }
}
