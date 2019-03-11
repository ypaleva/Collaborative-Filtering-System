import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.lang.*;
import java.util.concurrent.ConcurrentHashMap;

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
public class Main2 {
    final String database_filename = "comp3208.db";
    //final String trainingset_tablename = "TRAININGSET";
    final String trainingset_tablename = "TRAININGSET";
    final String similarity_tablename = "SIMILARITY";
    public SQLiteConnection c;
    public HashMap<Integer, Float> averageRatings = new HashMap<>();
    public List<ItemTuple> itemTuples = new ArrayList<>();
    public HashMap<ItemTuple, Float> similarityTable = new HashMap<>();
    //public HashMap<ItemTuple, Float> similarityTable1 = new HashMap<>();


    /**
     * The userHM is stored in a HashMap, which allows fast access.
     */
    public HashMap<Integer, HashMap<Integer, Float>> userHM;
    public HashMap<Integer, HashMap<Integer, Float>> itemHM;


    /**
     * Open an existing database.
     */
    public Main2() {
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

    public HashMap<Integer, RatingTuple> getUserRatingsForTwoItems(int item1, int item2) {
        HashMap<Integer, Float> userRatingsForItem1 = itemHM.get(item1);
        HashMap<Integer, Float> userRatingsForItem2 = itemHM.get(item2);
        //int count = 0;
        HashMap<Integer, RatingTuple> userRatingsForTwoItems = new HashMap<Integer, RatingTuple>();
        //System.out.println(userRatingsForTwoItems.size());

        for (Integer user : userRatingsForItem1.keySet()) {
            if (userRatingsForItem2.containsKey(user)) {
                userRatingsForTwoItems.put(user, new RatingTuple(userRatingsForItem1.get(user), userRatingsForItem2.get(user)));
                System.out.println("User : " + user + " Rating for item 1: " + userRatingsForItem1.get(user) + " Rating for item 2: " + userRatingsForItem2.get(user));
                //count++;

            }
        }
        System.out.println("Number of users who have rated the same item: " + userRatingsForTwoItems.size());
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

    public static Float calculateAverageHelper(HashMap<Integer, Float> userRatings) {
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

    public Float calculateNumeratorForSimilarityFunction(Integer item1, Integer item2) {
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
        System.out.println("Nominator is: " + sum);
        return sum;
    }

    public Float calculateDenominatorForSimilarityFunction(Integer item1, Integer item2) {
        HashMap<Integer, RatingTuple> userRatingsForTwoItems = getUserRatingsForTwoItems(item1, item2);
        Float sum1 = 0.0f;
        Float sum2 = 0.0f;
        Float prod = 0.0f;

        for (Integer user : userRatingsForTwoItems.keySet()) {
            Float userAverageRating = averageRatings.get(user);
            System.out.println("Average: " + userAverageRating);
            Float f1 = userRatingsForTwoItems.get(user).getRating1() - userAverageRating;
            Float f2 = userRatingsForTwoItems.get(user).getRating2() - userAverageRating;
            sum1 += (float) Math.pow(f1, 2);
            sum2 += (float) Math.pow(f2, 2);
        }

        prod = (float) Math.sqrt(sum1) * (float) Math.sqrt(sum2);
        System.out.println("Denominator is: " + prod);
        return prod;
    }

    public Float calculateSimilarityBetweenTwoItems(Integer item1, Integer item2) {
        Float numerator = calculateNumeratorForSimilarityFunction(item1, item2);
        Float denominator = calculateDenominatorForSimilarityFunction(item1, item2);

        Float similarity = numerator / denominator;
        System.out.println("Similarity bw item " + item1 + " and item " + item2 + " is: " + similarity);
        return similarity;

    }

    public void getAllItemTuples() {
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

    public void gettAllItemTuplesFromTable(){
        System.out.println("Loading item tuples from table " + similarity_tablename);
        try {
            SQLiteStatement stat = c.prepare("SELECT * FROM " + similarity_tablename);
            int count = 0;
            while (stat.step()) {
                Integer itemID1 = stat.columnInt(1);
                Integer itemID2 = stat.columnInt(2);
                ItemTuple itemTuple = new ItemTuple(itemID1, itemID2);
                itemTuples.add(itemTuple);
                count++;
                System.out.println("Added " + itemID1 + " and " + itemID2 + " with total of " + itemTuples.size());
            }
            stat.dispose();
            System.out.println("Loaded " + itemTuples.size() + "tuples");

        } catch (SQLiteException e) {
            error(e);
        }

    }

    public void calculateSimilarities() {
        for (ItemTuple tuple : itemTuples) {
            Float similarity = calculateSimilarityBetweenTwoItems(tuple.item1, tuple.item2);
            similarityTable.put(tuple, similarity);
            System.out.println("Similarity between item " + tuple.item1 + " and item " + tuple.item2 + ": "+ similarity + " # of similarities calculated" + similarityTable.size());
        }
    }


    public Float predictRating(Integer user, Integer item) {
        Float n = 0.0f;
        Float d = 0.0f;
        Float pred = 0.0f;

        for (Entry<ItemTuple, Float> entry : similarityTable.entrySet()) {

            if (entry.getKey().item1 == item || entry.getKey().item2 == item) {
                if (entry.getValue() > 0.0f) {
                    Float similarity = entry.getValue();
                    Float rating = 0.0f;
                    if (entry.getKey().item1 == item) {
                        rating = userHM.get(user).get(entry.getKey().item2);
                    } else {
                        rating = userHM.get(user).get(entry.getKey().item1);
                    }
                    n = n + (similarity * rating);
                    d = d + similarity;
                }
            }

        }
        pred = n/d;
        return pred;
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

    public void populateSimilarityTable(String tablename) throws SQLiteException {
        createSimilarityTable(tablename);

        SQLiteStatement statSim = c.prepare("INSERT INTO " + tablename + "  VALUES (?,?,?)");

        c.exec("BEGIN");

        for (Entry<ItemTuple, Float> entry : similarityTable.entrySet()) {

            ItemTuple tuple = entry.getKey();
            Float similarity = entry.getValue();

            // select whether to put it in the getUserRatingsForTwoItems or training set
            statSim.bind(1, tuple.item1);
            statSim.bind(2, tuple.item2);
            statSim.bind(3, similarity);
            statSim.stepThrough();
            statSim.reset();
        }

        // now do the commit part to save the changes to file
        c.exec("COMMIT");

    }

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
        int ratio = 5;

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
        Main2 db = new Main2();
        db.populateItemHM();
        db.populateUserHM();
        //db.createTestTrainingSet();
        //db.populateAveragesInMap();
        //System.out.println(db.calculateNumeratorForSimilarityFunction(1578, 1292));
        //System.out.println(db.calculateDenominatorForSimilarityFunction(1578, 1292));
        //db.getAllItemTuples();
        //db.gettAllItemTuplesFromTable();
        //db.calculateSimilarities();
        //db.populateSimilarityTable("SIMILARITY2");
        //db.populateSimilarityTable("SIMILARITY");
        System.out.println("Similarity: " + db.calculateSimilarityBetweenTwoItems(333, 1434));
        db.finish();
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("RUN TIME: " + elapsedTime);
    }
}