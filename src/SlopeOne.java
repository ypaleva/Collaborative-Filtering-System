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
    final String database_filename = "comp3208.db";
    //final String trainingset_tablename = "TRAININGSET";
    final String trainingset_tablename = "TRAININGSET";
    final String difference_tablename = "DIFFERENCECOPY";
    public SQLiteConnection c;
    final String new_pred_table = "PREDICTIONSSMALLCOMBINED";
    final String predictedRatings_tablename = "TESTSET";


    public HashMap<Integer, Float> averageRatings = new HashMap<>();
    public List<ItemTuple> itemTuples = new ArrayList<>();
    public HashMap<ItemTuple, Float> differencesTable = new HashMap<>();
    public HashMap<ItemUserTuple, Float> predictedRatings = new HashMap<>();
    public HashMap<ItemTuple, Float> similarityTable = new HashMap<>();
    final String similarity_tablename = "SIMILARITYCOPY2";
    final String predictions_tablename = "PREDICTIONSLOPEONE";
    final String sim_diff_table = "SIMDIFF2";
    //public HashMap<ItemTuple, Float> similarityTable1 = new HashMap<>();
    public ArrayList<RatingTuple> ratingTuples = new ArrayList<>();
    //int count2 = 0;
    int count = 0;
    public HashMap<Integer, ArrayList> predictionTuples = new HashMap<Integer, ArrayList>();
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

    public void populatePredictedRatingsHM() {
        int count = 0;
        try {
            SQLiteStatement stat = c.prepare("SELECT * FROM " + predictedRatings_tablename);
            while (stat.step()) {
                Integer user = stat.columnInt(0);
                Integer item = stat.columnInt(1);
                Float rating = (float) stat.columnDouble(2);
                ItemUserTuple tuple = new ItemUserTuple(user, item);
                predictedRatings.put(tuple, rating);
            }
            stat.dispose();
            System.out.println("Predicted rating HM populated");
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

    public void calculateAllDifferences() {
        // Get all item tuples from table for which differences have to be computed.
        gettAllItemTuplesFromTable();

        int stepSize = 1000000;
        int counter = 0;
        for (ItemTuple tuple : itemTuples) {
            Float difference = calculateDifferenceBetweenTwoItems(tuple.item1, tuple.item2);
            differencesTable.put(tuple, difference);
            counter++;
            if (counter % stepSize == 0) {
                System.out.println("Difference between item " + tuple.item1 + " and item " + tuple.item2 + ": " + difference + " # of differences calculated" + differencesTable.size());
            }
        }
        try {
            populateDifferenceTable();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public void predictAllSlopeOneRatings() throws SQLiteException {
        int stepSize = 1000;
        int counter = 0;
        for (Entry<ItemUserTuple, Float> entry : predictedRatings.entrySet()) {
            Integer userID = entry.getKey().userID;
            Integer itemID = entry.getKey().itemID;
            Float pred = predictWeightedSlopeOneBasedOnSimilarity(userID, itemID);
            if (pred < 1.0f) {
                pred = 1.0f;
            } else if (pred > 5.0f) {
                pred = 5.0f;
            }
            counter++;
            predictedRatings.replace(entry.getKey(), pred);
            if (counter % stepSize == 0) {
                System.out.println(("Prediction: " + pred) + " and num of pred ratings so far: " + counter);
            }
        }
        System.out.println("Size: " + predictedRatings.size());
        populatePredictedRatingsTable();
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
                    difference = differencesTable.get(tuple);
                    //System.out.println("Difference smarter for items: " + item + " and " + itemID + " : " + difference);
                    sum += difference;
                    counter++;
                }
            }
        }
        //System.out.println("Counter: " + counter);
        pred = sum / counter;

        if (Float.isNaN(pred)) {
            return averageRatings.get(userID);
        }
        else{
            pred += averageRatings.get(userID);
            return pred;
        }
        //System.out.println("sum: " + sum);
        //System.out.println("average: " + averageRatings.get(userID));
    }

    public void createPredictionTable(String tablename) {
        System.out.println("Creating/clearing similarity table " + tablename);
        // create the table if it does not exist
        try {
            c.exec("CREATE TABLE IF NOT EXISTS " + tablename + "(UserID INT, ItemID INT, Predicted REAL)");
            // delete entries from table in case it does exist
            c.exec("DELETE FROM " + tablename);

            System.out.println("Done");
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public void populatePredictedRatingsTable() throws SQLiteException {
        createPredictionTable(predictions_tablename);
        SQLiteStatement statSim = c.prepare("INSERT INTO " + predictions_tablename + "  VALUES (?,?,?)");

        c.exec("BEGIN");

        System.out.println("Size" + predictedRatings.size());

        for (Entry<ItemUserTuple, Float> entry : predictedRatings.entrySet()) {

            Integer userID = entry.getKey().userID;
            Integer itemID = entry.getKey().itemID;
            Float prediction = entry.getValue();
            System.out.println(userID + " ," + itemID + " ," + prediction);

            statSim.bind(1, userID);
            statSim.bind(2, itemID);
            if (prediction.equals(null)) {
                statSim.bind(3, 0);

            } else {
                statSim.bind(3, prediction);
            }
            statSim.stepThrough();
            statSim.reset();
        }

        System.out.println("Predicted ratings inserted into table");
        // now do the commit part to save the changes to file
        c.exec("COMMIT");

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
        }
        System.out.println("Denom: " + d);
        pred = sum / d;

        if (Float.isNaN(pred)) {
            return averageRatings.get(userID);
        }
        else{
            pred += averageRatings.get(userID);

            System.out.println("sum: " + sum);
            System.out.println("average: " + averageRatings.get(userID));

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

    public void populateSimilarityHM() {
        try {
            SQLiteStatement stat = c.prepare("SELECT Item1, Item2, Sim FROM SIMDIFF2");

            while (stat.step()) {
                Integer item1 = stat.columnInt(0);
                Integer item2 = stat.columnInt(1);
                Float similarity = (float) stat.columnDouble(2);

                ItemTuple tuple = new ItemTuple(item1, item2);

                similarityTable.put(tuple, similarity);
                //System.out.println("Similarity: " + similarity);
            }
            System.out.println("Loaded similarity table HM from database. Size: " + similarityTable.size());
            stat.dispose();

        } catch (SQLiteException e) {
            error(e);
        }
    }

    public void createDifferenceTable() {
        System.out.println("Creating/clearing similarity table " + difference_tablename);
        // create the table if it does not exist
        try {
            c.exec("CREATE TABLE IF NOT EXISTS " + difference_tablename + "(Item1ID INT, Item2ID INT, Difference REAL)");
            // delete entries from table in case it does exist
            c.exec("DELETE FROM " + difference_tablename);

            System.out.println("Done");
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public void populateDifferenceTable() throws SQLiteException {
        createDifferenceTable();

        SQLiteStatement statSim = c.prepare("INSERT INTO " + difference_tablename + "  VALUES (?,?,?)");

        c.exec("BEGIN");

        for (Entry<ItemTuple, Float> entry : differencesTable.entrySet()) {

            ItemTuple tuple = entry.getKey();
            Float diff = entry.getValue();

            // select whether to put it in the getUserRatingsForTwoItems or training set
            statSim.bind(1, tuple.item1);
            statSim.bind(2, tuple.item2);
            statSim.bind(3, diff);
            statSim.stepThrough();
            statSim.reset();
        }

        // now do the commit part to save the changes to file
        c.exec("COMMIT");

    }

    public void populateDifferenceHM() {
        try {
            SQLiteStatement stat = c.prepare("SELECT Item1, Item2, Diff FROM SIMDIFF2");

            while (stat.step()) {
                Integer item1 = stat.columnInt(0);
                Integer item2 = stat.columnInt(1);
                Float diff = (float) stat.columnDouble(2);

                ItemTuple tuple = new ItemTuple(item1, item2);

                differencesTable.put(tuple, diff);
                //System.out.println("Similarity: " + similarity);
            }
            System.out.println("Loaded difference table HM from database. Size: " + differencesTable.size());
            stat.dispose();

        } catch (SQLiteException e) {
            error(e);
        }
    }

    public void gettAllItemTuplesFromTable() {
        System.out.println("Loading item tuples from table DIFFERENCE");
        try {
            SQLiteStatement stat = c.prepare("SELECT * FROM DIFFERENCE");
            int count = 0;
            while (stat.step()) {
                Integer itemID1 = stat.columnInt(0);
                Integer itemID2 = stat.columnInt(1);
                ItemTuple itemTuple = new ItemTuple(itemID1, itemID2);
                itemTuples.add(itemTuple);
                count++;
                //System.out.println("Added " + itemID1 + " and " + itemID2 + " with total of " + itemTuples.size());
            }
            stat.dispose();
            System.out.println("Loaded " + itemTuples.size() + "tuples");

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

        try {
            SQLiteStatement stat = c.prepare("SELECT UserID, ItemID FROM TESTSET");
            //String query = "UPDATE TESTSET SET Predicted = ? WHERE UserID = ? AND ItemID = ?";
            String query = "INSERT INTO " + new_pred_table + " VALUES (?,?,?)";
            SQLiteStatement statUpdate = c.prepare(query);

            c.exec("BEGIN TRANSACTION;");
            while (stat.step()) {
                counter++;

                Integer userID = stat.columnInt(0);
                Integer itemID = stat.columnInt(1);
                Float predictedRating = predictWeightedSlopeOneBasedOnSimilarity(userID, itemID);

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

        } catch (SQLiteException e) {
            error(e);
        }
        c.exec("COMMIT;");
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
        //db.calculateAllDifferences();
        db.populateDifferenceHM();
        db.populateSimilarityHM();
        //db.populatePredictedRatingsHM();
        //db.predictAllSlopeOneRatings();
        db.calculatePredictedRatingsAndPopulateTable();
        db.finish();
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("RUN TIME: " + elapsedTime);
    }
}