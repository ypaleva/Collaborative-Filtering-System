import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
public class SlopeOne {
    final String database_filename = "comp3208.db";
    //final String trainingset_tablename = "TRAININGSET";
    final String trainingset_tablename = "TRAININGSET";
    final String predictedRatings_tablename = "TESTSET";
    final String similarity_tablename = "SIMILARITY";
    public SQLiteConnection c;
    public HashMap<Integer, Float> averageRatings = new HashMap<>();
    public List<ItemTuple> itemTuples = new ArrayList<>();
    public HashMap<ItemTuple, Float> differencesTable = new HashMap<>();
    public HashMap<ItemUserTuple, Float> predictedRatings = new HashMap<>();
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
    public  HashMap<Integer, ArrayList<ItemSimilarityTuple>> predictionsCache = new HashMap<>();

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
        for (ItemTuple tuple : itemTuples) {
            Float difference = calculateDifferenceBetweenTwoItems(tuple.item1, tuple.item2);
            differencesTable.put(tuple, difference);
            System.out.println("Similarity between item " + tuple.item1 + " and item " + tuple.item2 + ": " + difference + " # of differences calculated" + differencesTable.size());
        }
    }

    public Float predict(Integer userID, Integer itemID) {
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


    public void createDifferenceTable(String tablename) {
        System.out.println("Creating/clearing similarity table " + tablename);
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

    public void populateDifferenceTable(String tablename) throws SQLiteException {
        createDifferenceTable(tablename);

        SQLiteStatement statSim = c.prepare("INSERT INTO " + tablename + "  VALUES (?,?,?)");

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

    public void populateDifferenceHM(String tablename) {
        try {
            SQLiteStatement stat = c.prepare("SELECT * FROM " + tablename);

            while (stat.step()) {
                Integer item1 = stat.columnInt(0);
                Integer item2 = stat.columnInt(1);
                Float diff = (float) stat.columnDouble(2);

                ItemTuple tuple = new ItemTuple(item1, item2);

                differencesTable.put(tuple, diff);
                //System.out.println("Similarity: " + similarity);
            }
            System.out.println("Loaded similarity table HM from database. Size: " + differencesTable.size());
            stat.dispose();

        } catch (SQLiteException e) {
            error(e);
        }
    }

    public void getAllItemTuples() {
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

        db.finish();
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("RUN TIME: " + elapsedTime);
    }
}