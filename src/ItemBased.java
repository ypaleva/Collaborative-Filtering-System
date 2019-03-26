import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.lang.*;

/**
 * A few simple database manipulations using the SQLite4Java wrapper for the
 * Recommender System coursework.
 * <p>
 * It assumes that a database exists which is named "comp3208.db" and this
 * database is already loaded with all the training userHM set into a table called
 * "TRAININGSET", which is assumed to have the following columns: "UserID", "ItemID" and "Rating"
 * Note that SQL is largely case insensitive (except the
 * userHM itself if these are strings).
 *
 * @author Enrico Gerding
 */
public class ItemBased {
    final String database_filename = "comp3208.db";
    final String trainingset_tablename = "TRAININGSET";
    //final String trainingset_tablename = "BIGTRAININGSET";
    // thissss final String trainingset_tablename = "NEWTRAININGSET";
    final String predictedRatings_tablename = "TESTSET";
    // thisss final String predictedRatings_tablename = "NEWTESTSET";
    //final String predictions_tablename = "PREDICTION2";
    final String predictions_tablename = "PREDICTIONSSMALL";
    //final String similarity_tablename = "SIMILARITYBIG";
    //final String new_similarity_table = "SIMILARITYBIG2";
    //final String similarity_tablename = "SIMILARITYEVALUATE";
    final String new_similarity_table = "SIMILARITYEVALUATE2";
    final String similarity_tablename = "SIMILARITYCOPY2";
    final String new_pred_table = "PREDICTIONSSMALL";


    int userAvgUseCounter = 0;


    public SQLiteConnection c;
    public HashMap<Integer, Float> averageRatings = new HashMap<>();
    public List<ItemTuple> itemTuples = new ArrayList<>();
    public HashMap<ItemTuple, Float> similarityTable = new HashMap<>();
    public HashMap<ItemUserTuple, RatingTuple> predictedRatings = new HashMap<>();
    public HashMap<ItemUserTuple, Float> realRatings = new HashMap<>();
    //public HashMap<ItemTuple, Float> similarityTable1 = new HashMap<>();
    public ArrayList<RatingTuple> ratingTuples = new ArrayList<>();
    //int count2 = 0;
    int count = 0;
    public HashMap<Integer, ArrayList> predictionTuples = new HashMap<Integer, ArrayList>();

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
    public ItemBased() {
        c = new SQLiteConnection(new File(database_filename));

        try {
            c.open(false);
            System.out.println("Opened database successfully");
        } catch (SQLiteException e) {
            error(e);
        }
    }

    /**
     * Load training userHM.
     * <p>
     * The userHM is loaded into a HashMap where the key is the user, and the
     * value is another HashMap where the key is the item. This makes it very
     * fast to look up all the items belonging to a particular user. If you need
     * to look up items, this is the other way around. Note that you can also
     * use a TreeMap.
     */
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
            // don't forget to dispose any prepared statements
            stat.dispose();
            System.out.println("Loaded " + count + " ratings from " + userHM.size() + " users.");
        } catch (SQLiteException e) {
            error(e);
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
                Float predictedRating = predictSmarterRating(userID, itemID);

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

    public HashMap<Integer, RatingTuple> getUserRatingsForTwoItems(int item1, int item2) {
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
        //System.out.println("Number of users who have rated the same item: " + userRatingsForTwoItems.size());
        return userRatingsForTwoItems;
    }

    public Float calculateAverageHelper(HashMap<Integer, Float> userRatings) {
        Float average = 0.0f;
        Float sum = 0.0f;
        for (Map.Entry<Integer, Float> entry : userRatings.entrySet()) {
            //Integer itemID = entry.getKey();
            Float rating = entry.getValue();
            //System.out.println(rating);
            sum += rating;
        }
        average = sum / userRatings.size();
        return average;
    }

    public void populateAveragesInMap() {
        for (Integer userID : userHM.keySet()) {
            //System.out.println(userID);
            HashMap<Integer, Float> userRatings = userHM.get(userID);
            Float average = calculateAverageHelper(userRatings);
            averageRatings.put(userID, average);
        }
        userHM.clear();
        System.out.println("Averages populated in map");
    }

    // NEED TO FINISH
    public Float averageItemRating() {
        Float f = 0.0f;


        return f;
    }

    public Float calculateSimilarityBetweenTwoItems(Integer item1, Integer item2) {
        HashMap<Integer, RatingTuple> userRatingsForTwoItems = getUserRatingsForTwoItems(item1, item2);
        Float numerator = 0.0f;
        Float sum1Denom = 0.0f;
        Float sum2Denom = 0.0f;
        Float denominator = 0.0f;

        for (Integer user : userRatingsForTwoItems.keySet()) {
            Float userAverageRating = averageRatings.get(user);
            Float f1 = userRatingsForTwoItems.get(user).getRating1() - userAverageRating;
            Float f2 = userRatingsForTwoItems.get(user).getRating2() - userAverageRating;
            Float p = f1 * f2;
            numerator += p;

            sum1Denom += (float) Math.pow(f1, 2);
            sum2Denom += (float) Math.pow(f2, 2);
        }

        denominator = (float) Math.sqrt(sum1Denom) * (float) Math.sqrt(sum2Denom);

        Float similarity = numerator / denominator;
        return similarity;
    }


    public void calculateSimilarities(String tablename) throws SQLiteException {
        createSimilarityTable(tablename);
        SQLiteStatement statSim = c.prepare("INSERT INTO " + tablename + "  VALUES (?,?,?)");


        int stepSize = 1000000;
        int counter = 0;
        c.exec("BEGIN");
        for (Integer item1 : itemHM.keySet()) {
            for (Integer item2 : itemHM.keySet()) {
                if (!item1.equals(item2)) {
                    if (item1 < item2) {
                        counter++;

                        Float similarity = calculateSimilarityBetweenTwoItems(item1, item2);

                        statSim.bind(1, item1);
                        statSim.bind(2, item2);
                        statSim.bind(3, similarity);
                        statSim.stepThrough();
                        statSim.reset();


                        if (counter % stepSize == 0) {
                            System.out.println("Similarity between " + item1 + " and " + item2 + " is " + similarity);
                            System.out.println("Num of similarities so far: " + counter);
                        }

                    }
                }
            }
        }
        c.exec("COMMIT");
    }



//    public void predictAllSmarterRatings() throws SQLiteException {
//        int stepSize = 1000;
//        int counter = 0;
//        for (Entry<ItemUserTuple, RatingTuple> entry : predictedRatings.entrySet()) {
//            Integer userID = entry.getKey().userID;
//            Integer itemID = entry.getKey().itemID;
//            Float pred = predictSmarterRating(userID, itemID);
//                if (pred < 1.0f) {
//                   pred = 1.0f;
//               } else if (pred > 5.0f) {
//                    pred = 5.0f;
//                }
//            counter++;
//            predictedRatings.replace(entry.getKey(), new RatingTuple(entry.getValue().r1, pred));
//            if (counter % stepSize == 0) {
//                System.out.println(("Prediction: " + pred) + " and num of pred ratings so far: " + counter);
//            }
//        }
//        System.out.println("Size: " + predictedRatings.size());
//        calculateMSE();
//        populatePredictedRatingsTable();
//    }


    public Float predictSmarterRating(Integer userID, Integer itemID) {
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

                    //System.out.println("Item for which we calculate rating: " + itemID + ", other item " + item);

                    ItemTuple tuple1 = new ItemTuple(item, itemID);
                    ItemTuple tuple2 = new ItemTuple(itemID, item);
                    if (similarityTable.containsKey(tuple1)) {
                        myTuple = tuple1;
                    } else if (similarityTable.containsKey(tuple2)) {
                        myTuple = tuple2;
                    } else {
                        continue;
                    }
                    similarity = similarityTable.get(myTuple);
                    //System.out.println("Similarity: " + similarity);
                    //if(similarity > 0) {
                    rating = allUserRatingForItem.get(userID);
                    n = n + (similarity * rating);
                    d = d + similarity;
                    //System.out.println("Nominator: " + n);
                    //System.out.println("Denominator " + d);
                    //}

                }
            }
            pred = n / d;

            if (Float.isNaN(pred)) {
                pred = averageRatings.get(userID);
                userAvgUseCounter++;
            }
        }
        //System.out.println("Prediction " + pred);

        return pred;
    }

    public void predictAllSmartestRatings() throws SQLiteException {
        int stepSize = 1000;
        int counter = 0;
        for (Entry<ItemUserTuple, RatingTuple> entry : predictedRatings.entrySet()) {
            Integer userID = entry.getKey().userID;
            Integer itemID = entry.getKey().itemID;
            Float pred = predictSmartestRating(userID, itemID, 10);
            //if (pred < 1.0f) {
            //pred = 1.0f;
            //} else if (pred > 5.0f) {
            //pred = 5.0f;
            //}
            counter++;
            predictedRatings.replace(entry.getKey(), new RatingTuple(entry.getValue().r1, pred));
            if (counter % stepSize == 0) {
                System.out.println(("Prediction: " + pred) + " and num of pred ratings so far: " + counter);
            }
        }
        System.out.println("Size: " + predictedRatings.size());
        calculateMSE();
        populatePredictedRatingsTable();
    }

    public Float predictSmartestRating(Integer userID, Integer itemID, int K) {
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
                    //System.out.println("Tuples size: " + tuples.size());
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

            //System.out.println("Similarity is " + tuple.similarity + " and rating is " + tuple.rating);
            n = n + (tuple.similarity * tuple.rating);
            d = d + tuple.similarity;
            //System.out.println("nominator: " + n);
            //System.out.println("Denom: " + d);
            pred = n / d;


            if (Float.isNaN(pred)) {
                pred = averageRatings.get(userID);
            }
        }

        //System.out.println("Prediction " + pred);
        return pred;
    }

    public void populatePredictedRatingsTable() throws SQLiteException {
        createPredictionTable();
        SQLiteStatement statSim = c.prepare("INSERT INTO " + new_pred_table + "  VALUES (?,?,?,?)");

        c.exec("BEGIN");

        System.out.println("Size" + predictedRatings.size());

        for (Entry<ItemUserTuple, RatingTuple> entry : predictedRatings.entrySet()) {

            Integer userID = entry.getKey().userID;
            Integer itemID = entry.getKey().itemID;
            Float rating = entry.getValue().r1;
            Float prediction = entry.getValue().r2;

            //System.out.println(userID + " ," + itemID + " ," + prediction);

            statSim.bind(1, userID);
            statSim.bind(2, itemID);
            statSim.bind(3, rating);

            if (prediction == null) {
                statSim.bind(4, 0);

            } else {
                statSim.bind(4, prediction);
            }
            statSim.stepThrough();
            statSim.reset();
        }
        System.out.println("Predicted ratings inserted into table");
        // now do the commit part to save the changes to file
        c.exec("COMMIT");

    }


//    //Populates list with True - Predicted rating tuples from table
//    public void populateRatingTuples() {
//        try {
//            SQLiteStatement stat = c.prepare("SELECT (Rating, Predicted) FROM " + "NEWTESTSET");
//
//            while (stat.step()) {
//                Float rating = (float) stat.columnDouble(2);
//                Float predicted = (float) stat.columnDouble(3);
//
//                ratingTuples.add(new RatingTuple(rating, predicted));
//            }
//            System.out.println("Populated rating tuples from database.");
//            stat.dispose();
//
//        } catch (SQLiteException e) {
//            error(e);
//        }
//
//    }

    public void calculateMSE() {
        Float errorSum = 0.0f;
        for (RatingTuple tuple : predictedRatings.values()) {
            errorSum += (float) Math.pow((tuple.r1 - tuple.r2), 2);
        }
        Float mse = errorSum / predictedRatings.values().size();
        System.out.println("MSE: " + mse);
    }

    public void populateSimilarityHM() {
        try {
            SQLiteStatement stat = c.prepare("SELECT * FROM " + similarity_tablename);

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

    public void populatePredictedRatingsHM() {
        int count = 0;
        try {
            SQLiteStatement stat = c.prepare("SELECT * FROM " + predictedRatings_tablename);
            while (stat.step()) {
                Integer user = stat.columnInt(0);
                Integer item = stat.columnInt(1);
                Float rating = (float) stat.columnDouble(2);
                Float predicted = (float) stat.columnDouble(3);
                ItemUserTuple tuple = new ItemUserTuple(user, item);
                predictedRatings.put(tuple, new RatingTuple(rating, predicted));
                //realRatings.put(tuple, rating);
            }
            stat.dispose();
            System.out.println("Predicted rating HM populated");
        } catch (SQLiteException e) {
            error(e);
        }
    }


    public void predictAllSmarterRatings() throws SQLiteException {
        int stepSize = 1000;
        int counter = 0;
        for (Entry<ItemUserTuple, RatingTuple> entry : predictedRatings.entrySet()) {
            Integer userID = entry.getKey().userID;
            Integer itemID = entry.getKey().itemID;
            Float pred = predictSmarterRating(userID, itemID);
            if (pred < 1.0f) {
                pred = 1.0f;
            } else if (pred > 5.0f) {
                pred = 5.0f;
            }
            counter++;
            predictedRatings.replace(entry.getKey(), new RatingTuple(entry.getValue().r1, pred));
            if (counter % stepSize == 0) {
                System.out.println(("Prediction: " + pred) + " and num of pred ratings so far: " + counter);
            }
        }
        System.out.println("Size: " + predictedRatings.size());
        calculateMSE();
        populatePredictedRatingsTable();
    }

    /**
     * Create a table or clear it if it already exists.
     *
     * @param tablename
     */
    public void createTable(String tablename) {
        try {
            System.out.println("Creating/clearing table " + tablename);

            // create the table if it does not exist
            c.exec("CREATE TABLE IF NOT EXISTS " + tablename + "(UserID INT, ItemID INT, Rating REAL)");

            // delete entries from table in case it does exist
            c.exec("DELETE FROM " + tablename);

            System.out.println("Done");
        } catch (SQLiteException e) {
            error(e);
        }
    }

    public void createSimilarityTable(String tablename) {
        System.out.println("Creating/clearing similarity table " + tablename);
        // create the table if it does not exist
        try {
            c.exec("CREATE TABLE IF NOT EXISTS " + tablename + "(Item1ID INT, Item2ID INT, Similarity REAL)");
            // delete entries from table in case it does exist
            c.exec("DELETE FROM " + tablename);

            System.out.println("Done");
        } catch (SQLiteException e) {
            e.printStackTrace();
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

//    public void gettAllItemTuplesFromTable() throws SQLiteException {
//        createSimilarityTable(new_similarity_table);
//        //System.out.println("Loading item tuples from table " + similarity_tablename);
//        //Integer low = 0;
//        //Integer high = 1000000;
//        int stepSize = 10000000;
//        int counter = 0;
//        HashMap<ItemTuple, Float> mySimilarityTable = new HashMap<>();
//
//        try {
//            SQLiteStatement stat = c.prepare("SELECT ItemID1, ItemID2 FROM " + similarity_tablename);
//
//            //SQLiteStatement statSim = c.prepare("INSERT INTO " + new_similarity_table + "  VALUES (?,?,?)");
//
//            //c.exec("BEGIN");
//
//            while (stat.step()) {
//
//                Integer itemID1 = stat.columnInt(0);
//                Integer itemID2 = stat.columnInt(1);
//                ItemTuple itemTuple = new ItemTuple(itemID1, itemID2);
//
//                Float similarity = calculateSimilarityBetweenTwoItems(itemID1, itemID2);
//
//                mySimilarityTable.put(itemTuple, similarity);
//                counter++;
//
//                //statSim.bind(1, itemID1);
//                //statSim.bind(2, itemID2);
//                //statSim.bind(3, similarity);
//                //statSim.stepThrough();
//                //statSim.reset();
//                //counter++;
//
////                if (counter % stepSize == 0) {
////                    populateSimilarityTable(new_similarity_table, mySimilarityTable);
////                    System.out.println("Similarities computed so far: " + counter);
////                    mySimilarityTable.clear();
////                }
//            }
//            //c.exec("COMMIT");
//
//
//            //itemTuples.add(itemTuple);
//            //counter++;
//
//            //if (counter % stepSize == 0) {
//
//            //populateSimilarityTable(new_similarity_table);
//            //System.out.println("Number of similarities populated: " + counter);
//            //itemTuples.clear();
//            //counter = 0;
//            //}
//            //System.out.println("Added " + itemID1 + " and " + itemID2 + " with total of " + itemTuples.size());
//            stat.dispose();
//            //System.out.println("Loaded " + itemTuples.size() + "tuples");
//
//        } catch (SQLiteException e) {
//            error(e);
//        }
//
//    }

//    public void populateSimilarityTable(String tablename) throws SQLiteException {
//        createSimilarityTable("CORRECTSIMILARITY");
//
//        SQLiteStatement statSim = c.prepare("INSERT INTO " + tablename + "  VALUES (?,?,?)");
//
//        c.exec("BEGIN");
//        for (Entry<ItemTuple, Float> entry : similarityTable.entrySet()) {
//
//            ItemTuple tuple = entry.getKey();
//            Float similarity = entry.getValue();
//            // select whether to put it in the getUserRatingsForTwoItems or training set
//            statSim.bind(1, tuple.item1);
//            statSim.bind(2, tuple.item2);
//            statSim.bind(3, similarity);
//            statSim.stepThrough();
//            statSim.reset();
//        }
//        c.exec("COMMIT");
//    }

    /**
     * An example of how you can create your own getUserRatingsForTwoItems/training set from the userHM
     * you get, so you can evaluate the recommender system.
     * <p>
     * Note that this can be done completely by SQL commands. The main purpose
     * of this code is to demonstrates a couple of features of the SQLite4Java
     * wrapper.
     */
    public void createTestTrainingSet() {
        // first create new tables
        String newTrainingset = "NEWTRAININGSET";
        String newTestset = "NEWTESTSET";

        // ratio of values that go to the getUserRatingsForTwoItems set (e.g. 1 in 10)
        int ratio = 20;

        createTable(newTrainingset);
        createTable(newTestset);
        try {
            System.out.println("Populating training and getUserRatingsForTwoItems sets");

            // Not strictly necessary, but it's faster to use a prepared
            // statement when repeating a similar action many times
            // This statement has 3 values which can set at a later stage by
            // using the "bind" method
            SQLiteStatement statTrain = c.prepare("INSERT INTO " + newTrainingset + "  VALUES (?,?,?)");
            SQLiteStatement statTest = c.prepare("INSERT INTO " + newTestset + " VALUES (?,?,?)");

            // loop over all ratings
            int count = 0;
            for (Integer user : userHM.keySet()) {
                System.out.println("Processing user " + user);
                // Writing every single entry to the database is time consuming
                // since writing to file is slow
                // Instead, you can group together such actions by using "BEGIN"
                // and "COMMIT" constructs
                // In this case the inserts of each user are grouped together
                c.exec("BEGIN");

                for (Entry<Integer, Float> itemRatingPair : userHM.get(user).entrySet()) {
                    // select whether to put it in the getUserRatingsForTwoItems or training set
                    if (count % ratio == 0) {
                        // insert in getUserRatingsForTwoItems set
                        statTest.bind(1, user);
                        statTest.bind(2, itemRatingPair.getKey());
                        statTest.bind(3, itemRatingPair.getValue());
                        statTest.stepThrough();
                        statTest.reset();
                    } else {
                        // insert in training set
                        statTrain.bind(1, user);
                        statTrain.bind(2, itemRatingPair.getKey());
                        statTrain.bind(3, itemRatingPair.getValue());
                        statTrain.stepThrough();
                        statTrain.reset();
                    }
                    count++;
                }

                // now do the commit part to save the changes to file
                c.exec("COMMIT");
            }
            System.out.println("Done");
        } catch (SQLiteException e) {
            error(e);
        }
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
        ItemBased db = new ItemBased();
        db.populateItemHM();
        db.populateUserHM();
        //db.createTestTrainingSet();
        db.populateAveragesInMap();
        db.populateSimilarityHM();

        db.calculatePredictedRatingsAndPopulateTable();

        // * db.populatePredictedRatingsHM();
        // * db.predictAllSmarterRatings();

        // * db.calculateSimilarities("CORRECTSIMILARITY");

        //db.gettAllItemTuplesFromTable();
        //db.calculateSimilarities("CORRECTSIMILARITY");
        //db.predictSmarterRating(101337, 3369);
        //db.calculateSimilarities();
        //db.populateSimilarityTable("CORRECTSIMILARITY");
        //db.populatePredictionsCacheHM();
        //db.predictAllSmarterRatings();
        //System.out.println("Predicted Rating: " + db.predictRating(1, 12332));
        //db.populatePredictedRatingsTable("TESTSET");
        //db.updatePredictedRatingsTable();
        //db.getUserRatingsForTwoItems(4, 5);
        db.finish();
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("RUN TIME: " + elapsedTime);
    }
}