import java.util.HashMap;
import java.util.Map;

public class Test {

    public static HashMap<Integer, HashMap<Integer, Float>> data;
    public static HashMap<Pair, Float> similarityTable = new HashMap<>();
    public static HashMap<Integer, Float> averageRatings = new HashMap<>();

    public static void populateAveragesInMap() {
        for (Integer userID : data.keySet()) {
            HashMap<Integer, Float> userRatings = data.get(userID);
            Float average = calculateAverageHelper(userRatings);
            averageRatings.put(userID, average);
        }
    }

    public static Float calculateAverageHelper(HashMap<Integer, Float> userRatings) {
        Float average = 0.0f;
        Float sum = 0.0f;
        for (Map.Entry<Integer, Float> entry : userRatings.entrySet()) {
            Integer itemID = entry.getKey();
            Float rating = entry.getValue();
            sum += rating;
        }
        average = sum / userRatings.size();
        return average;
    }

    public static Float calculateSimilarityHelper(Integer item1, Integer item2, Float averageRating) {
        Float similarity = 0.0f;

        return similarity;
    }

    public static void calculateAllSimilarities() {
        for (Map.Entry<Integer, HashMap<Integer, Float>> entry : data.entrySet()) {
            Integer userID = entry.getKey();
            HashMap<Integer, Float> itemToRating = entry.getValue();

        }
    }

    public static void main(String[] args) {
        data = new HashMap<>();

        HashMap<Integer, Float> rating1 = new HashMap<>();
        rating1.put(1, 3.5f);
        rating1.put(2, 1.5f);
        rating1.put(3, 2.0f);
        HashMap<Integer, Float> rating2 = new HashMap<>();
        rating2.put(1, 2.0f);
        rating2.put(2, 2.5f);
        rating2.put(3, 4.0f);
        HashMap<Integer, Float> rating3 = new HashMap<>();
        rating3.put(1, 5.0f);
        rating3.put(2, 3.5f);
        rating3.put(3, 3.0f);
        HashMap<Integer, Float> rating4 = new HashMap<>();
        rating4.put(1, 2.5f);
        rating4.put(2, 4.5f);
        rating4.put(3, 4.0f);
        HashMap<Integer, Float> rating5 = new HashMap<>();
        rating5.put(1, 4.5f);
        rating5.put(2, 2.5f);
        rating5.put(3, 5.0f);

        data.put(1000, rating1);
        data.put(2000, rating2);
        data.put(3000, rating3);
        data.put(4000, rating4);
        data.put(5000, rating5);

        populateAveragesInMap();

        for (Map.Entry<Integer, Float> entry : averageRatings.entrySet()) {
            System.out.println("User: " + entry.getKey() + ", average rating: " + entry.getValue());
        }



    }

}
