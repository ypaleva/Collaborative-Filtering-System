import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SlopeOne {
    final String database_filename = "comp3208-eval.db";
    final String trainingset_tablename = "NEWTRAININGSET";
    public SQLiteConnection c;
    final String new_pred_table = "PREDICTIONSSLOPEONE";
    final String testset_tablename = "NEWTESTSET";
    public HashMap<Integer, Float> averageRatings = new HashMap<>();
    public HashMap<ItemTuple, Float> differencesTable = new HashMap<>();
    public HashMap<ItemTuple, Float> similarityTable = new HashMap<>();

    int userAvgUseCounter = 0;

    /**
     * The userHM is stored in a HashMap, which allows fast access.
     */
    public HashMap<Integer, HashMap<Integer, Float>> userHM;
    public HashMap<Integer, HashMap<Integer, Float>> itemHM;

    //ItemID -> (ItemID, Similarity)
    public HashMap<Integer, ArrayList<ItemSimilarityTuple>> predictionsCache = new HashMap<>();

    /**
     * Open an existing database.
     */
    public SlopeOne() {
        c = new SQLiteConnection(new File(database_filename));
        try {
            c.open(false);
            System.out.println("Opened database successfully");
        } catch (SQLiteException e) {
            error(e);
        }
    }

    public void populateItemHM() {
        System.out.println("Loading itemHM from table " + trainingset_tablename);
        int count = 0;
        try {
            SQLiteStatement stat = c.prepare("SELECT * FROM " + trainingset_tablename);

            itemHM = new HashMap<>();
            while (stat.step()) {
                Integer user = stat.columnInt(0);
                Integer item = stat.columnInt(1);
                Float rating = (float) stat.columnDouble(2);

                HashMap<Integer, Float> itemRatings = itemHM.get(item);

                if (itemRatings == null) {
                    itemRatings = new HashMap<>();
                    itemHM.put(item, itemRatings);
                }
                itemRatings.put(user, rating);
                count++;
            }
            System.out.println("Loaded " + count + " ratings from " + itemHM.size() + " items.");
            stat.dispose();

        } catch (SQLiteException e) {
            error(e);
        }
    }

    public void populateUserHM() {
        System.out.println("Loading userHM from table " + trainingset_tablename);
        int count = 0;
        try {
            SQLiteStatement stat = c.prepare("SELECT * FROM " + trainingset_tablename);

            userHM = new HashMap<>();
            while (stat.step()) {
                Integer user = stat.columnInt(0);
                Integer item = stat.columnInt(1);
                Float rating = (float) stat.columnDouble(2);

                HashMap<Integer, Float> userRatings = userHM.get(user);

                if (userRatings == null) {
                    userRatings = new HashMap<>();
                    userHM.put(user, userRatings);
                }
                userRatings.put(item, rating);
                count++;
            }
            stat.dispose();
            System.out.println("Loaded " + count + " ratings from " + userHM.size() + " users.");
        } catch (SQLiteException e) {
            error(e);
        }
    }

    // Returns a HM<userID, RatingTuple> containing all users that have rated both items and their ratings.
    public HashMap<Integer, RatingTuple> getUserRatingsForTwoItems(int item1, int item2) {
        HashMap<Integer, Float> userRatingsForItem1 = itemHM.get(item1);
        HashMap<Integer, Float> userRatingsForItem2 = itemHM.get(item2);
        //int count = 0;
        HashMap<Integer, RatingTuple> userRatingsForTwoItems = new HashMap<>();
        //System.out.println("Item 1: " + item1 + " Item 2: " + item2);
        //System.out.println("User ratings for 2 items size: " + userRatingsForTwoItems.size());

        for (Integer user : userRatingsForItem1.keySet()) {
            if (userRatingsForItem2.containsKey(user)) {
                userRatingsForTwoItems.put(user, new RatingTuple(userRatingsForItem1.get(user), userRatingsForItem2.get(user)));
                //System.out.println("User : " + user + " Rating for item 1: " + userRatingsForItem1.get(user) + " Rating for item 2: " + userRatingsForItem2.get(user));
                //count++;

            }
        }
        //System.out.println("Number of users who have rated the same item: " + userRatingsForTwoItems.size());
        return userRatingsForTwoItems;
    }

    public void populateAveragesInMap() {
        for (Integer userID : userHM.keySet()) {
            //System.out.println(userID);
            HashMap<Integer, Float> userRatings = userHM.get(userID);
            Float average = calculateAverageHelper(userRatings);
            averageRatings.put(userID, average);
        }
        System.out.println("Averages populated in map");
    }

    public Float calculateAverageHelper(HashMap<Integer, Float> userRatings) {
        Float average = 0.0f;
        Float sum = 0.0f;
        for (Entry<Integer, Float> entry : userRatings.entrySet()) {
            //Integer itemID = entry.getKey();
            Float rating = entry.getValue();
            //System.out.println(rating);
            sum += rating;
        }
        average = sum / userRatings.size();
        return average;
    }

    public Float calculateDifferenceBetweenTwoItems(Integer item1, Integer item2) {
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

    public void calculateAllDifferences(String tablename) throws SQLiteException {
        createDifferenceTable(tablename);

        SQLiteStatement statSim = c.prepare("INSERT INTO " + tablename + "  VALUES (?,?,?)");

        int stepSize = 1000000;
        int counter = 0;
        c.exec("BEGIN");

        for (Integer item1 : itemHM.keySet()) {
            for (Integer item2 : itemHM.keySet()) {
                if (!item1.equals(item2)) {
                    counter++;

                    Float difference = calculateDifferenceBetweenTwoItems(item1, item2);

                    statSim.bind(1, item1);
                    statSim.bind(2, item2);
                    statSim.bind(3, difference);
                    statSim.stepThrough();
                    statSim.reset();


                    if (counter % stepSize == 0) {
                        System.out.println("Difference between " + item1 + " and " + item2 + " is " + difference);
                        System.out.println("Num of differences so far: " + counter);
                    }
                }
            }
        }
        c.exec("COMMIT");

    }

    public void createDifferenceTable(String tablename){
        System.out.println("Creating/clearing difference table " + tablename);
        // create the table if it does not exist
        try {
            c.exec("CREATE TABLE IF NOT EXISTS " + tablename + "(Item1ID INT, Item2ID INT, Difference REAL)");
            // delete entries from table in case it does exist
            c.exec("DELETE FROM " + tablename);

            System.out.println("Done");
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }


    public Float predictSlopeOne(Integer userID, Integer itemID) {
        Float sum = 0.0f;
        int counter = 0;
        Float pred = 0.0f;

        for (Integer item : itemHM.keySet()) {
            if (!item.equals(itemID)) {
                HashMap<Integer, Float> allUserRatingForItem = itemHM.get(item);
                Float difference = 0.0f;
                if (allUserRatingForItem.containsKey(userID)) {
                    ItemTuple tuple = new ItemTuple(item, itemID);
                    //System.out.println("item 1 " + item + " and item 2: " + itemID);
                    if (differencesTable.containsKey(tuple)) {
                        difference = differencesTable.get(tuple);
                        sum += difference;
                        counter++;
                        //System.out.println("Dif" + difference);
                    }
                    else{
                        continue;
                    }
                }
            }
        }
        //System.out.println("Counter: " + counter);
        pred = sum / counter;

        if (pred < 1.0f) {
            pred = 1.0f;
        } else if (pred > 5.0f) {
            pred = 5.0f;
        }

        if (Float.isNaN(pred)) {
            userAvgUseCounter++;
            return averageRatings.get(userID);

        }
        else{
            pred += averageRatings.get(userID);
            return pred;
        }
    }

    public Float predictWeightedSlopeOne(Integer userID, Integer itemID) {
        Float sum = 0.0f;
        int d = 0;
        Float pred = 0.0f;
        for (Integer item : itemHM.keySet()) {
            if (!item.equals(itemID)) {
                HashMap<Integer, Float> allUserRatingForItem = itemHM.get(item);
                Float difference = 0.0f;
                if (allUserRatingForItem.containsKey(userID)) {


                    ItemTuple tuple = new ItemTuple(item, itemID);

                    if (similarityTable.containsKey(tuple)) {
                        difference = differencesTable.get(tuple);
                        //System.out.println("Difference smarter for items: " + item + " and " + itemID + " : " + difference);
                        int size = getUserRatingsForTwoItems(item, itemID).size();
                        //System.out.println("Size: " + size);
                        sum += difference * size;
                        //System.out.println("Weighted Sum: " + sum);
                        d += size;
                    } else {
                        continue;
                    }
                }
            }
        }
        //System.out.println("Denom: " + d);
        pred = sum / d;

        if (pred < 1.0f) {
            pred = 1.0f;
        } else if (pred > 5.0f) {
            pred = 5.0f;
        }

        if (Float.isNaN(pred)) {
            return averageRatings.get(userID);
        }
        else{
            pred += averageRatings.get(userID);
            return pred;
        }

    }

    public Float predictWeightedSlopeOneBasedOnSimilarity(Integer userID, Integer itemID) {
        Float sum = 0.0f;
        Float d = 0.0f;
        ItemTuple myTuple = null;
        Float pred = 0.0f;

        for (Integer item : itemHM.keySet()) {
            if (!item.equals(itemID)) {
                HashMap<Integer, Float> allUserRatingForItem = itemHM.get(item);
                Float difference = 0.0f;

                if (allUserRatingForItem.containsKey(userID)) {

                    ItemTuple tuple = new ItemTuple(item, itemID);
                    ItemTuple tuple2 = new ItemTuple(itemID, item);

                    Float rating = allUserRatingForItem.get(userID);
                    if (similarityTable.containsKey(tuple)) {
                        myTuple = tuple;
                    } else if (similarityTable.containsKey(tuple2)) {
                        myTuple = tuple2;
                    } else {
                        continue;
                    }
                    Float similarity = similarityTable.get(myTuple);

                    if (similarity > 0) {
                        difference = differencesTable.get(tuple) + rating;
                        sum += difference * similarity;
                        d += similarity;
                    }
                }
            }

        }
        //System.out.println("Denom: " + d);
        pred = sum / d;

        if (Float.isNaN(pred)) {
            userAvgUseCounter++;
            return averageRatings.get(userID);
        }
        else
        {
            //System.out.println("sum: " + sum);
            //System.out.println("average: " + averageRatings.get(userID));
            //System.out.println("Sim table size " + similarityTable.size());
            return pred;
        }
    }


    public void populateDifferenceHM() {
        try {
            SQLiteStatement stat = c.prepare("SELECT Item1ID, Item2ID, Difference FROM DIFFERENCEEVAL");

            while (stat.step()) {
                Integer item1 = stat.columnInt(0);
                Integer item2 = stat.columnInt(1);
                Float diff = (float) stat.columnDouble(2);

                ItemTuple tuple = new ItemTuple(item1, item2);

                differencesTable.put(tuple, diff);
            }
            System.out.println("Loaded difference table HM from database. Size: " + differencesTable.size());
            stat.dispose();

        } catch (SQLiteException e) {
            error(e);
        }
    }


    public void createPredictionTable() {
        System.out.println("Creating/clearing similarity table " + new_pred_table);
        // create the table if it does not exist
        try {
            c.exec("CREATE TABLE IF NOT EXISTS " + new_pred_table + "(UserID INT, ItemID INT, Predicted REAL)");
            // delete entries from table in case it does exist
            c.exec("DELETE FROM " + new_pred_table);

            System.out.println("Done");
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }
    public void calculatePredictedRatingsAndPopulateTable() throws SQLiteException {

        createPredictionTable();
        int stepSize = 1000;
        int counter = 0;
        ArrayList<RatingTuple> predicted = new ArrayList<>();

        try {
            SQLiteStatement stat = c.prepare("SELECT UserID, ItemID, Rating FROM " + testset_tablename + " limit 15000");
            //String query = "UPDATE TESTSET SET Predicted = ? WHERE UserID = ? AND ItemID = ?";
            String query = "INSERT INTO " + new_pred_table + " VALUES (?,?,?)";
            SQLiteStatement statUpdate = c.prepare(query);

            c.exec("BEGIN TRANSACTION;");
            while (stat.step()) {
                counter++;

                Integer userID = stat.columnInt(0);
                Integer itemID = stat.columnInt(1);
                Float realRating = (float) stat.columnDouble(2);

                Float predictedRating = predictSlopeOne(userID, itemID);
                //Float predictedRating = predictWeightedSlopeOne(userID, itemID);
                //Float predictedRating = predictWeightedSlopeOneBasedOnSimilarity(userID, itemID);

                RatingTuple ratingTuple = new RatingTuple(predictedRating, realRating);
                predicted.add(ratingTuple);

                statUpdate.bind(3, predictedRating);
                statUpdate.bind(1, userID);
                statUpdate.bind(2, itemID);

                if (counter % stepSize == 0) {
                    System.out.println(("Prediction: " + predictedRating) + " and num of pred ratings so far: " + counter);
                }

                statUpdate.stepThrough();
                statUpdate.reset();
            }
            stat.dispose();

            System.out.println("Number of times average user rating was used: " + userAvgUseCounter);
            System.out.println("Calculated predictions and populated prediction table.");
            calculateMSE(predicted);

        } catch (SQLiteException e) {
            error(e);
        }
        c.exec("COMMIT;");
    }

    public void calculateMSE(ArrayList<RatingTuple> predicted) {
        Float errorSum = 0.0f;
        for (RatingTuple tuple : predicted) {
            errorSum += (float) Math.pow((tuple.r1 - tuple.r2), 2);
        }
        Float mse = errorSum / predicted.size();
        System.out.println("MSE: " + mse);
    }


    /**
     * Show error message.
     *
     * @param e
     */
    public void error(SQLiteException e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
        System.exit(0);
    }

    /**
     * Make sure to disconnect to the database at the end for a "clean" finish.
     */
    public void finish() {
        c.dispose();
    }

    public static void main(String[] args) throws SQLiteException {
        long startTime = System.currentTimeMillis();
        SlopeOne db = new SlopeOne();
        db.populateItemHM();
        db.populateUserHM();
        db.populateAveragesInMap();
        db.calculateAllDifferences("DIFFERENCEEVAL");
        db.populateDifferenceHM();
        db.calculatePredictedRatingsAndPopulateTable();
        db.finish();
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("RUN TIME: " + elapsedTime);
    }
}