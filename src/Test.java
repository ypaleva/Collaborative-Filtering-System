import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Test {

    public static HashMap<Integer, HashMap<Integer, Float>> data;
    public static HashMap<ItemTuple, Float> similarityTable = new HashMap<>();
    public static HashMap<Integer, Float> averageRatings = new HashMap<>();
    public static ArrayList<ItemTuple> itemTuples = new ArrayList<>();
    public static HashMap<Integer, HashMap<Integer, Float>> userHM;

    //ItemID -> UserID -> Rating
    public static HashMap<Integer, HashMap<Integer, Float>> itemHM = new HashMap<>();
    public static HashMap<ItemTuple, Float> differencesTable = new HashMap<>();

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

    public static void getAllItemTuples2() {
        for (Integer item1 : itemHM.keySet()) {
            for (Integer item2 : itemHM.keySet()) {
                if (!item1.equals(item2)) {
                    ItemTuple itemTuple1 = new ItemTuple(item1, item2);
                    ItemTuple itemTuple2 = new ItemTuple(item2, item1);
                    if (!(itemTuples.contains(itemTuple1) || itemTuples.contains(itemTuple2))) {
                        itemTuples.add(itemTuple1);
                        System.out.println("Tuple items added: " + itemTuple1.item1 + " and " + itemTuple1.item2);
                    }
                }
            }
        }
    }

    public static void getAllItemTuples() {
        for (Integer item1 : itemHM.keySet()) {
            for (Integer item2 : itemHM.keySet()) {
                if (!item1.equals(item2)) {
                    ItemTuple itemTuple1 = new ItemTuple(item1, item2);
                    ItemTuple itemTuple2 = new ItemTuple(item2, item1);
                    itemTuples.add(itemTuple1);
                    itemTuples.add(itemTuple2);
                }
            }
        }
    }


    public static Float calculateNumeratorForSimilarityFunction(Integer item1, Integer item2) {
        HashMap<Integer, RatingTuple> userRatingsForTwoItems = getUserRatingsForTwoItems(item1, item2);
        Float sum = 0.0f;

        for (Integer user : userRatingsForTwoItems.keySet()) {
            Float userAverageRating = averageRatings.get(user);
            //System.out.println(userAverageRating);
            Float f1 = userRatingsForTwoItems.get(user).getRating1() - userAverageRating;
            Float f2 = userRatingsForTwoItems.get(user).getRating2() - userAverageRating;
            Float p = f1 * f2;
            sum += p;
        }
        return sum;
    }

    public static Float calculateDenominatorForSimilarityFunction(Integer item1, Integer item2) {
        HashMap<Integer, RatingTuple> userRatingsForTwoItems = getUserRatingsForTwoItems(item1, item2);
        Float sum1 = 0.0f;
        Float sum2 = 0.0f;
        Float prod = 0.0f;

        for (Integer user : userRatingsForTwoItems.keySet()) {
            Float userAverageRating = averageRatings.get(user);
            Float f1 = userRatingsForTwoItems.get(user).getRating1() - userAverageRating;
            Float f2 = userRatingsForTwoItems.get(user).getRating2() - userAverageRating;
            sum1 += (float) Math.pow(f1, 2);
            sum2 += (float) Math.pow(f2, 2);
        }

        prod = (float) Math.sqrt(sum1) * (float) Math.sqrt(sum2);
        return prod;
    }

    public static Float calculateSimilarityBetweenTwoItems(Integer item1, Integer item2) {
        Float numerator = calculateNumeratorForSimilarityFunction(item1, item2);
        Float denominator = calculateDenominatorForSimilarityFunction(item1, item2);
        return numerator / denominator;
    }

    public static HashMap<Integer, RatingTuple> getUserRatingsForTwoItems(int item1, int item2) {
        HashMap<Integer, Float> userRatingsForItem1 = itemHM.get(item1);
        HashMap<Integer, Float> userRatingsForItem2 = itemHM.get(item2);
        //int count = 0;
        HashMap<Integer, RatingTuple> userRatingsForTwoItems = new HashMap<Integer, RatingTuple>();
        //System.out.println(userRatingsForTwoItems.size());

        for (Integer user : userRatingsForItem1.keySet()) {
            if (userRatingsForItem2.containsKey(user)) {
                userRatingsForTwoItems.put(user, new RatingTuple(userRatingsForItem1.get(user), userRatingsForItem2.get(user)));
                //System.out.println("User : " + user + " Rating for item 1: " + userRatingsForItem1.get(user) + " Rating for item 2: " + userRatingsForItem2.get(user));
                //count++;

            }
        }
        //System.out.println(userRatingsForTwoItems.size());
        return userRatingsForTwoItems;
    }

    public static void calculateSimilarities() {
        for (ItemTuple tuple : itemTuples) {
            Float similarity = calculateSimilarityBetweenTwoItems(tuple.item1, tuple.item2);
            similarityTable.put(tuple, similarity);
            System.out.println("Similarity between item " + tuple.item1 + " and item " + tuple.item2 + ": "+ similarity);
        }
    }

    public static Float predictRating(Integer user, Integer item) {
        Float n = 0.0f;
        Float d = 0.0f;
        Float pred = 0.0f;

        for (Map.Entry<ItemTuple, Float> entry : similarityTable.entrySet()) {

            if (entry.getKey().item1.equals(item) || entry.getKey().item2.equals(item)) {
                if (entry.getValue() > 0.0f) {
                    Float similarity = entry.getValue();
                    Float rating = 0.0f;
                    if (entry.getKey().item1.equals(item)) {
                        rating = data.get(user).get(entry.getKey().item2);
                    } else {
                        rating = data.get(user).get(entry.getKey().item1);
                    }
                    n = n + (similarity * rating);
                    d = d + similarity;
                }
            }

        }
        pred = n / d;
        return pred;
    }


    public static Float calculateDifferenceBetweenTwoItems(Integer item1, Integer item2) {
        HashMap<Integer, RatingTuple> userRatingsForTwoItems = getUserRatingsForTwoItems(item1, item2);
        Float sum = 0.0f;
        Float result = 0.0f;

        for (Integer user : userRatingsForTwoItems.keySet()) {
            Float f = userRatingsForTwoItems.get(user).getRating1() - userRatingsForTwoItems.get(user).getRating2();
            sum += f;
        }
        result = sum / userRatingsForTwoItems.size();
        return result;
    }

    public static void calculateAllDifferences() {
        for (ItemTuple tuple : itemTuples) {
            Float difference = calculateDifferenceBetweenTwoItems(tuple.item1, tuple.item2);
            differencesTable.put(tuple, difference);
            System.out.println("Similarity between item " + tuple.item1 + " and item " + tuple.item2 + ": " + difference + " # of differences calculated" + differencesTable.size());
        }
    }

    public static Float predict(Integer userID, Integer itemID) {
        Float sum = 0.0f;
        int counter = 0;
        for (Map.Entry<ItemTuple, Float> entry : differencesTable.entrySet()) {

            if (entry.getKey().item2.equals(itemID)) {
                sum += entry.getValue();
                counter++;
                System.out.println("entry: " + entry.getValue());
            }

        }
        System.out.println("Counter: " + counter);
        sum = sum / counter;
        System.out.println("sum: " + sum);
        System.out.println("average: " + averageRatings.get(userID));
        return averageRatings.get(userID) + sum;
    }

    public static Float predictSmarterRating(Integer userID, Integer itemID) {
        Float n = 0.0f;
        Float d = 0.0f;
        Float pred = 0.0f;
        ItemTuple myTuple = null;
        for (Integer item : itemHM.keySet()) {
            if (!item.equals(itemID)) {
                HashMap<Integer, Float> allUserRatingForItem = itemHM.get(item);
                Float similarity = 0.0f;
                Float rating = 0.0f;
                if (allUserRatingForItem.containsKey(userID)) {

                    ItemTuple tuple = new ItemTuple(item, itemID);
                    if(similarityTable.containsKey(tuple)){
                        myTuple = tuple;
                    }
                    else
                    {
                        myTuple = new ItemTuple(itemID, item);
                    }
                    System.out.println(myTuple + " item 1: " + myTuple.item1 + " item 2 " + myTuple.item2);

                    similarity = similarityTable.get(myTuple);
                    if(similarity > 0) {
                        rating = allUserRatingForItem.get(userID);

                        System.out.println("Similarity between " + item + " and " + itemID + " is " + similarity);
                        System.out.println("Rating " + rating);
                        n = n + (similarity * rating);
                        d = d + similarity;
                    }

                }
            }
            pred = n / d;

            if (Float.isNaN(pred)) {
                pred = averageRatings.get(userID);
            }
        }

            return pred;
        }



    public static void main(String[] args) {
        data = new HashMap<>();

        HashMap<Integer, Float> rating1 = new HashMap<>();
        rating1.put(1, 8f);
        rating1.put(4, 2f);
        rating1.put(5, 7f);
        HashMap<Integer, Float> rating2 = new HashMap<>();
        rating2.put(1, 2.0f);
        rating2.put(3, 5f);
        rating2.put(4, 7f);
        rating2.put(5, 5f);
        HashMap<Integer, Float> rating3 = new HashMap<>();
        rating3.put(1, 5.0f);
        rating3.put(2, 4f);
        rating3.put(3, 7f);
        rating3.put(4, 4f);
        rating3.put(5, 7f);
        HashMap<Integer, Float> rating4 = new HashMap<>();
        rating4.put(1, 7f);
        rating4.put(2, 1f);
        rating4.put(3, 7.0f);
        rating4.put(4, 3f);
        rating4.put(5, 8f);
        HashMap<Integer, Float> rating5 = new HashMap<>();
        rating5.put(1, 1f);
        rating5.put(2, 7f);
        rating5.put(3, 4f);
        rating5.put(4, 6f);
        rating5.put(5, 5f);
        HashMap<Integer, Float> rating6 = new HashMap<>();
        rating6.put(1, 8f);
        rating6.put(2, 3f);
        rating6.put(3, 8f);
        rating6.put(4, 3f);
        rating6.put(5, 7f);

        data.put(1, rating1);
        data.put(2, rating2);
        data.put(3, rating3);
        data.put(4, rating4);
        data.put(5, rating5);
        data.put(6, rating6);


        HashMap<Integer, Float> itemToUser1 = new HashMap<>();
        itemToUser1.put(1, 8f);
        itemToUser1.put(2, 2f);
        itemToUser1.put(3, 5f);
        itemToUser1.put(4, 7f);
        itemToUser1.put(5, 1f);
        itemToUser1.put(6, 8f);

        HashMap<Integer, Float> itemToUser2 = new HashMap<>();
        itemToUser2.put(3, 4f);
        itemToUser2.put(4, 1f);
        itemToUser2.put(5, 7f);
        itemToUser2.put(6, 3f);

        HashMap<Integer, Float> itemToUser3 = new HashMap<>();
        itemToUser3.put(2, 5f);
        itemToUser3.put(3, 7f);
        itemToUser3.put(4, 7f);
        itemToUser3.put(5, 4f);
        itemToUser3.put(6, 8f);
        HashMap<Integer, Float> itemToUser4 = new HashMap<>();
        itemToUser4.put(1, 2f);
        itemToUser4.put(2, 7f);
        itemToUser4.put(3, 4f);
        itemToUser4.put(4, 3f);
        itemToUser4.put(5, 6f);
        itemToUser4.put(6, 3f);
        HashMap<Integer, Float> itemToUser5 = new HashMap<>();
        itemToUser5.put(1, 7f);
        itemToUser5.put(2, 5f);
        itemToUser5.put(3, 7f);
        itemToUser5.put(4, 8f);
        itemToUser5.put(5, 5f);
        itemToUser5.put(6, 7f);

        populateAveragesInMap();

        for (Map.Entry<Integer, Float> entry : averageRatings.entrySet()) {
            System.out.println("User: " + entry.getKey() + ", average rating: " + entry.getValue());
        }

        itemHM.put(1, itemToUser1);
        itemHM.put(2, itemToUser2);
        itemHM.put(3, itemToUser3);
        itemHM.put(4, itemToUser4);
        itemHM.put(5, itemToUser5);

        getAllItemTuples2();

        /*for (ItemTuple tuple : itemTuples) {
            System.out.println("item 1: " + tuple.item1 + ", item 2: " + tuple.item2);
        }

        calculateAllDifferences();
        for (Map.Entry<ItemTuple, Float> entry : differencesTable.entrySet()) {
            System.out.print(entry.getValue());
        }


        System.out.println();
        System.out.println("Predicted rating for user 1, item 3: " + predictRating(1, 3));


        System.out.println("1 and 2: " + calculateDifferenceBetweenTwoItems(1, 2));
        System.out.println("4 and 2: " + calculateDifferenceBetweenTwoItems(4, 2));
        System.out.println("5 and 2: " + calculateDifferenceBetweenTwoItems(5, 2));

        System.out.println(predict(1, 2));*/
        calculateSimilarities();
        System.out.println("Predicted " + predictSmarterRating(2, 2));

    }

}
