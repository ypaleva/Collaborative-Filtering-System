import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Test {

    public static HashMap<Integer, HashMap<Integer, Float>> data;
    public static HashMap<ItemTuple, Float> similarityTable = new HashMap<>();
    public static HashMap<Integer, Float> averageRatings = new HashMap<>();
    public static ArrayList<ItemTuple> itemTuples = new ArrayList<>();
    public static ArrayList<ItemTuple> itemTuples2 = new ArrayList<>();
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
                    //if (!(itemTuples2.contains(itemTuple1) || itemTuples2.contains(itemTuple2))) {
                    //itemTuples2.add(itemTuple1);
                    System.out.println("Tuple items added: " + itemTuple1.item1 + " and " + itemTuple1.item2);
                    //}
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


    public static RatingTuple calculateNumeratorForSimilarityFunction(Integer item1, Integer item2) {
        HashMap<Integer, RatingTuple> userRatingsForTwoItems = getUserRatingsForTwoItems(item1, item2);
        Float nominator = 0.0f;
        Float sum1Denom = 0.0f;
        Float sum2Denom = 0.0f;
        Float denominator = 0.0f;

        for (Integer user : userRatingsForTwoItems.keySet()) {
            Float userAverageRating = averageRatings.get(user);

            Float f1 = userRatingsForTwoItems.get(user).getRating1() - userAverageRating;
            Float f2 = userRatingsForTwoItems.get(user).getRating2() - userAverageRating;
            Float p = f1 * f2;
            nominator += p;

            sum1Denom += (float) Math.pow(f1, 2);
            sum2Denom += (float) Math.pow(f2, 2);
        }
        //System.out.println("Nominator is: " + nominator);
        denominator = (float) Math.sqrt(sum1Denom) * (float) Math.sqrt(sum2Denom);
        RatingTuple tuple = new RatingTuple(nominator, denominator);
        //float[] list = new float[] { nominator, denominator};
        return tuple;
    }


    public static Float calculateSimilarityBetweenTwoItems(Integer item1, Integer item2) {
        RatingTuple tuple = calculateNumeratorForSimilarityFunction(item1, item2);
        //Float numerator = calculateNumeratorAndDenominatorForSimilarity(item1, item2);
        //Float denominator = calculateDenominatorForSimilarityFunction(item1, item2);
        Float numerator = tuple.r1;
        Float denominator = tuple.r2;

        Float similarity = numerator / denominator;
        //System.out.println("Similarity bw item " + item1 + " and item " + item2 + " is: " + similarity);
        return similarity;

    }

    public static HashMap<Integer, RatingTuple> getUserRatingsForTwoItems(int item1, int item2) {
        HashMap<Integer, Float> userRatingsForItem1 = itemHM.get(item1);
        HashMap<Integer, Float> userRatingsForItem2 = itemHM.get(item2);
        //int count = 0;
        HashMap<Integer, RatingTuple> userRatingsForTwoItems = new HashMap<>();
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
        for (ItemTuple tuple : itemTuples2) {
            Float similarity = calculateSimilarityBetweenTwoItems(tuple.item1, tuple.item2);
            similarityTable.put(tuple, similarity);
            System.out.println("Similarity between item " + tuple.item1 + " and item " + tuple.item2 + ": " + similarity);
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
            System.out.println("Difference between item " + tuple.item1 + " and item " + tuple.item2 + ": " + difference + " # of differences calculated" + differencesTable.size());
        }
        System.out.println("Diff table size: " + differencesTable.size());
    }


    public static Float predictSlopeOne(Integer userID, Integer itemID) {
        Float sum = 0.0f;
        int counter = 0;
        for (Integer item : itemHM.keySet()) {
            if (!item.equals(itemID)) {
                HashMap<Integer, Float> allUserRatingForItem = itemHM.get(item);
                Float difference = 0.0f;
                if (allUserRatingForItem.containsKey(userID)) {
                    ItemTuple tuple = new ItemTuple(item, itemID);
                    difference = differencesTable.get(tuple);
                    System.out.println("Difference smarter for items: " + item + " and " + itemID + " : " + difference);
                    sum += difference;
                    System.out.println("Sum: " + sum);


                    counter++;
                }
            }

            if (Float.isNaN(sum)) {
                sum = averageRatings.get(userID);
            }

        }
        System.out.println("Counter: " + counter);
        sum = sum / counter;
        System.out.println("sum: " + sum);
        System.out.println("average: " + averageRatings.get(userID));
        return sum + averageRatings.get(userID);
    }

    public static Float predictWeightedSlopeOne(Integer userID, Integer itemID) {
        Float sum = 0.0f;
        int d = 0;
        for (Integer item : itemHM.keySet()) {
            if (!item.equals(itemID)) {
                HashMap<Integer, Float> allUserRatingForItem = itemHM.get(item);
                Float difference = 0.0f;
                if (allUserRatingForItem.containsKey(userID)) {
                    ItemTuple tuple = new ItemTuple(item, itemID);
                    Float rating = allUserRatingForItem.get(userID);
                    difference = differencesTable.get(tuple);
                    System.out.println("Difference smarter for items: " + item + " and " + itemID + " : " + difference);
                    int size = getUserRatingsForTwoItems(item, itemID).size();
                    System.out.println("Size: " + size);
                    sum += difference * size;
                    System.out.println("Weighted Sum: " + sum);
                    d += size;
                }
            }

            //if (Float.isNaN(sum)) {
            //    sum = averageRatings.get(userID);
            //}

        }
        System.out.println("Denom: " + d);
        sum = sum / d;
        System.out.println("sum: " + sum);
        System.out.println("average: " + averageRatings.get(userID));
        return sum + averageRatings.get(userID);
    }

    public static Float predictWeightedSlopeOneBasedOnSimilarity(Integer userID, Integer itemID) {
        Float sum = 0.0f;
        Float d = 0.0f;
        ItemTuple myTuple = null;

        for (Integer item : itemHM.keySet()) {
            if (!item.equals(itemID)) {
                HashMap<Integer, Float> allUserRatingForItem = itemHM.get(item);
                Float difference = 0.0f;

                if (allUserRatingForItem.containsKey(userID)) {

                    ItemTuple tuple = new ItemTuple(item, itemID);
                    ItemTuple tuple2 = new ItemTuple(itemID, item);

                    Float rating = allUserRatingForItem.get(userID);
                    difference = differencesTable.get(tuple) + rating;

                    if (similarityTable.containsKey(tuple)) {
                        myTuple = tuple;
                    } else if (similarityTable.containsKey(tuple2)) {
                        myTuple = tuple2;
                    } else {
                        break;
                    }
                    Float similarity = similarityTable.get(myTuple);

                    if (similarity > 0) {
                        sum += difference * similarity;
                        d += similarity;
                    }
                }
            }

            if (Float.isNaN(sum)) {
                sum = averageRatings.get(userID);
            }

        }
        System.out.println("Denom: " + d);
        sum = sum / d;
        System.out.println("sum: " + sum);
        System.out.println("average: " + averageRatings.get(userID));
        System.out.println("Sim table size " + similarityTable.size());

        return sum;
    }

    public static Float predictSmartestRating(Integer userID, Integer itemID, int K) {
        Float n = 0.0f;
        Float d = 0.0f;
        Float pred = 0.0f;
        ItemTuple myTuple = null;
        ArrayList<SimilarityRatingTuple> tuples = new ArrayList<>();
        for (Integer item : itemHM.keySet()) {
            if (!item.equals(itemID)) {
                HashMap<Integer, Float> allUserRatingForItem = itemHM.get(item);
                Float similarity = 0.0f;
                Float rating = 0.0f;
                if (allUserRatingForItem.containsKey(userID)) {

                    ItemTuple tuple1 = new ItemTuple(item, itemID);
                    ItemTuple tuple2 = new ItemTuple(itemID, item);

                    if (similarityTable.containsKey(tuple1)) {
                        myTuple = tuple1;
                    } else if (similarityTable.containsKey(tuple2)) {
                        myTuple = tuple2;
                    } else {
                        break;
                    }
                    similarity = similarityTable.get(myTuple);
                    if (similarity > 0) {
                        rating = allUserRatingForItem.get(userID);
                        tuples.add(new SimilarityRatingTuple(similarity, rating));
                    }
                    System.out.println("Tuples size: " + tuples.size());
                }
            }
        }
        Collections.sort(tuples, (s1, s2) -> {
            if (s1.similarity < s2.similarity) {
                return 1;
            } else if (s1.similarity > s2.similarity) {
                return -1;
            } else {
                return 0;
            }
        });

        if (tuples.size() < K) {
            K = tuples.size();
        }
        for (SimilarityRatingTuple tuple : tuples.subList(0, K)) {

            System.out.println("Similarity is " + tuple.similarity + " and rating is " + tuple.rating);
            n = n + (tuple.similarity * tuple.rating);
            d = d + tuple.similarity;
            System.out.println("nominator: " + n);
            System.out.println("Denom: " + d);
            pred = n / d;


            if (Float.isNaN(pred)) {
                pred = averageRatings.get(userID);
            }
        }

        System.out.println("Prediction " + pred);
        return pred;
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

                    ItemTuple tuple1 = new ItemTuple(item, itemID);
                    ItemTuple tuple2 = new ItemTuple(itemID, item);
                    if (similarityTable.containsKey(tuple1)) {
                        myTuple = tuple1;
                    } else if (similarityTable.containsKey(tuple2)) {
                        myTuple = tuple2;
                    } else {
                        break;
                    }
                    similarity = similarityTable.get(myTuple);
                    if (similarity > 0) {
                        rating = allUserRatingForItem.get(userID);
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

    public static void generateTuples() {
        for (Integer item1 : itemHM.keySet()) {
            for (Integer item2 : itemHM.keySet()) {
                if (!item1.equals(item2)) {
                    if (item1 < item2) {
                        System.out.println("Item1: " + item1 + ", item2: " + item2);

                    }
                }
            }
        }
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

        itemHM.put(2, itemToUser1);
        itemHM.put(1, itemToUser2);
        itemHM.put(3, itemToUser3);
        itemHM.put(5, itemToUser4);
        itemHM.put(4, itemToUser5);

        //getAllItemTuples2();
        //getAllItemTuples();
        generateTuples();
        //calculateSimilarities();

        //for (ItemTuple tuple : itemTuples) {
        //    System.out.println("item 1: " + tuple.item1 + ", item 2: " + tuple.item2);
        //}
        //calculateSimilarities();
        //calculateAllDifferences();
        //for (Map.Entry<ItemTuple, Float> entry : differencesTable.entrySet()) {
        //    System.out.print(entry.getValue());
        //}

        //System.out.println("1 and 2: " + calculateDifferenceBetweenTwoItems(1, 2));
        //System.out.println("4 and 2: " + calculateDifferenceBetweenTwoItems(4, 2));
        //System.out.println("5 and 2: " + calculateDifferenceBetweenTwoItems(5, 2));

        //System.out.println("Predicted smarter: " + predictSlopeOne(1, 2));
        //System.out.println("Predicted weighted slope one: " + predictWeightedSlopeOne(2, 2));
        //System.out.println("Predicted weighted slope one with similarities: " + predictWeightedSlopeOneBasedOnSimilarity(1, 2));

        /*ArrayList<SimilarityRatingTuple> tuples = new ArrayList<>();
        tuples.add(new SimilarityRatingTuple(0.5f, 1.4f));
        tuples.add(new SimilarityRatingTuple(0.6f, 10.1f));
        tuples.add(new SimilarityRatingTuple(0.7f, 10.3f));
        tuples.add(new SimilarityRatingTuple(0.75f, 10.4f));
        tuples.add(new SimilarityRatingTuple(0.35f, 1.00f));

        Collections.sort(tuples, (s1, s2) -> {
            if (s1.similarity < s2.similarity) {
                return 1;
            } else if (s1.similarity > s2.similarity) {
                return -1;
            } else {
                return 0;
            }
        });

        for (SimilarityRatingTuple similarityRatingTuple : tuples.subList(0,3)) {
            System.out.println(similarityRatingTuple.similarity);
        }
*/
        //System.out.println(predictSmartestRating(1, 2, 1));
        //System.out.println(predictSlopeOne(1, 3));

    }

}
