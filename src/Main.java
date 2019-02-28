import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * A few simple database manipulations using the SQLite4Java wrapper for the
 * Recommender System coursework.
 *
 * It assumes that a database exists which is named "comp3208.db" and this
 * database is already loaded with all the training data set into a table called
 * "TRAININGSET", which is assumed to have the following columns: "UserID", "ItemID" and "Rating"
 * Note that SQL is largely case insensitive (except the
 * data itself if these are strings).
 *
 * @author Enrico Gerding
 *
 */
public class Main {
    final String database_filename = "comp3208.db";
    final String trainingset_tablename = "TRAININGSET";
    public SQLiteConnection connection;

    /**
     * The data is stored in a HashMap, which allows fast access.
     */
    public HashMap<Integer, HashMap<Integer, Float>> data;

    public HashMap<Pair, Float> similarityTable = new HashMap<>();
    public HashMap<Integer, Float> averageRatings = new HashMap<>();

    /**
     * Open an existing database.
     */
    public Main() {
        connection = new SQLiteConnection(new File(database_filename));
        try {
            connection.open(false);
            System.out.println("Opened database successfully");
        } catch (SQLiteException e) {
            error(e);
        }
    }

    /**
     * Load training data.
     *
     * The data is loaded into a HashMap where the key is the user, and the
     * value is another HashMap where the key is the item. This makes it very
     * fast to look up all the items belonging to a particular user. If you need
     * to look up items, this is the other way around. Note that you can also
     * use a TreeMap.
     */
    public void loadRatings() {
        System.out.println("Loading data from table " + trainingset_tablename);
        int count = 0;
        try {
            SQLiteStatement stat = connection.prepare("SELECT * FROM " + trainingset_tablename);

            data = new HashMap<>();
            while (stat.step()) {
                Integer user = stat.columnInt(0);
                Integer item = stat.columnInt(1);
                Float rating = (float) stat.columnDouble(2); // convert from
                // double to
                // float, since
                // float takes
                // up less
                // memory

                HashMap<Integer, Float> userRatings = data.get(user);
                // check if this user already exists. If not, create a new
                // HashMap for this user.
                if (userRatings == null) {
                    userRatings = new HashMap<>();
                    data.put(user, userRatings);
                }
                userRatings.put(item, rating);
                count++;
            }
            // don't forget to dispose any prepared statements
            stat.dispose();
            System.out.println("Loaded " + count + " ratings from " + data.size() + " users.");
        } catch (SQLiteException e) {
            error(e);
        }
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
            connection.exec("CREATE TABLE IF NOT EXISTS " + tablename + "(UserID INT, ItemID INT, Rating REAL)");

            // delete entries from table in case it does exist
            connection.exec("DELETE FROM " + tablename);

            System.out.println("Done");
        } catch (SQLiteException e) {
            error(e);
        }
    }

    /**
     * An example of how you can create your own test/training set from the data
     * you get, so you can evaluate the recommender system.
     *
     * Note that this can be done completely by SQL commands. The main purpose
     * of this code is to demonstrates a couple of features of the SQLite4Java
     * wrapper.
     */
    public void createTestTrainingSet() {
        // first create new tables
        String newTrainingset = "NEWTRAININGSET";
        String newTestset = "NEWTESTSET";

        // ratio of values that go to the test set (e.g. 1 in 10)
        int ratio = 20;

        createTable(newTrainingset);
        createTable(newTestset);
        try {
            System.out.println("Populating training and test sets");

            // Not strictly necessary, but it's faster to use a prepared
            // statement when repeating a similar action many times
            // This statement has 3 values which can set at a later stage by
            // using the "bind" method
            SQLiteStatement statTrain = connection.prepare("INSERT INTO " + newTrainingset + "  VALUES (?,?,?)");
            SQLiteStatement statTest = connection.prepare("INSERT INTO " + newTestset + " VALUES (?,?,?)");

            // loop over all ratings
            int count = 0;
            for (Integer user : data.keySet()) {
                System.out.println("Processing user " + user);
                // Writing every single entry to the database is time consuming
                // since writing to file is slow
                // Instead, you can group together such actions by using "BEGIN"
                // and "COMMIT" constructs
                // In this case the inserts of each user are grouped together
                connection.exec("BEGIN");

                for (Entry<Integer, Float> itemRatingPair : data.get(user).entrySet()) {
                    // select whether to put it in the test or training set
                    if (count % ratio == 0) {
                        // insert in test set
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
                connection.exec("COMMIT");
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

    public void populateAveragesInMap() {
        for (Integer userID : data.keySet()) {
            HashMap<Integer, Float> userRatings = data.get(userID);
            Float average = calculateAverageHelper(userRatings);


        }
    }

    public Float calculateAverageHelper(HashMap<Integer, Float> userRatings) {
        Float average = 0.0f;
        Float sum = 0.0f;
        for (Entry<Integer, Float> entry : userRatings.entrySet()) {
            Integer itemID = entry.getKey();
            Float rating = entry.getValue();
            sum += rating;
        }
        average = sum/userRatings.size();
        return average;
    }

    public Float calculateSimilarityHelper(Integer item1, Integer item2) {
        Float similarity = 0.0f;

        return similarity;
    }

    public void calculateAllSimilarities() {

    }

    /**
     * Make sure to disconnect to the database at the end for a "clean" finish.
     */
    public void finish() {
        connection.dispose();
    }

    public static void main(String[] args) {
        Main db = new Main();
        db.loadRatings();
        db.createTestTrainingSet();
        db.finish();
    }
}