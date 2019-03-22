import java.util.Comparator;

public class SimilarityRatingTuple implements Comparable<SimilarityRatingTuple> {

    public Float similarity;
    public Float rating;

    public SimilarityRatingTuple(Float similarity, Float rating) {
        this.similarity = similarity;
        this.rating = rating;
    }

    @Override
    public int compareTo(SimilarityRatingTuple tuple) {
        if (this.similarity > tuple.similarity) {
            return 1;
        } else if (this.similarity < tuple.similarity) {
            return -1;
        } else {
            return 0;
        }
    }
}
