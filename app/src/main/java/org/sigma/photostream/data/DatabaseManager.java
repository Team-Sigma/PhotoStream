package org.sigma.photostream.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.sigma.photostream.stream.TwitterQuery;
import org.sigma.photostream.stream.TwitterStream;
import org.sigma.photostream.util.Converter;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Singleton class that handles all the SQL and other database stuff.
 *
 * <h1>Versioning</h1>
 * <em>Current version: 1</em>
 * I will maintain a big table of version changes here, to make upgrading as painless as possible.
 *
 * @author Tobias Highfill
 */
public class DatabaseManager {

    private static DatabaseManager dbmInstance = null;


    /**
     * Fetches the DatabaseManager instance
     * @return The DatabaseManager instance
     */
    public static DatabaseManager getInstance(){
        if(dbmInstance == null){
            throw new DBManagerNotInitializedException();
        }
        return dbmInstance;
    }

    /**
     * Fetches the instance if it exists or initializes it if it does not
     * @param context The Context from which to create the database
     * @return The DatabaseManager instance
     */
    public static DatabaseManager getInstance(Context context){
        if(dbmInstance == null){
            dbmInstance = new DatabaseManager(context);
        }
        return dbmInstance;
    }

    /**
     * The name of the database.
     */
    public static final String DATABASE_NAME = "PhotoStreamDB";

    /**
     * The current database version. See the versioning info above.
     */
    public static final int DATABASE_VERSION = 1;

    /**
     * The full type definition for primary keys in this DB including constraints.
     */
    public static final String PRIMARY_KEY_DEF = "INTEGER PRIMARY KEY ASC ON CONFLICT FAIL AUTOINCREMENT";

    private static final String ID = "id";

    /**
     * The name of the table storing the Twitter Queries
     */
    public static final String TWITTER_QUERY = "TwitterQuery";
    //The column names
    private static final String TQ_FROMUSER = "fromUser";
    private static final String TQ_FROMLIST = "fromList";
    private static final String TQ_QUESTION = "question";
    private static final String TQ_SINCEDATE = "sinceDate";
    private static final String TQ_UNTILDATE = "untilDate";
    private static final String TQ_ATTITUDE = "attitude";
    private static final String TQ_EXACTPHRASES = "exactPhrases";
    private static final String TQ_REMOVE = "remove";
    private static final String TQ_HASHTAGS = "hashtags";
    //The creation query
    private static final String CREATE_TWITTER_QUERY = String.format(
            "CREATE TABLE IF NOT EXISTS %s (%s %s , %s TEXT, %s TEXT," +
                    "%s INTEGER, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT)",
            TWITTER_QUERY,
            ID,
            PRIMARY_KEY_DEF,
            TQ_FROMUSER,
            TQ_FROMLIST,
            TQ_QUESTION,
            TQ_SINCEDATE,
            TQ_UNTILDATE,
            TQ_ATTITUDE,
            TQ_EXACTPHRASES,
            TQ_REMOVE,
            TQ_HASHTAGS);

    /**
     * The name of the table storing the Twitter Streams
     */
    public static final String TWITTER_STREAM = "TwitterStream";
    //The column names
    private static final String TS_TWEETBATCHSIZE = "tweetBatchSize";
    private static final String TS_DOGEOCODE = "doGeocode";
    private static final String TS_GEOCODERADIUS = "geocodeRadius";
    private static final String TS_QUERY = "query";
    //The creation query
    private static final String CREATE_TWITTER_STREAM = String.format(
            "CREATE TABLE IF NOT EXISTS %s (%s %s, %s INTEGER NOT NULL, %s INTEGER," +
                    "%s INTEGER, %s INTEGER REFERENCES %s MATCH %s ON UPDATE SET NULL)",
            TWITTER_STREAM,
            ID,
            PRIMARY_KEY_DEF,
            TS_TWEETBATCHSIZE,
            TS_DOGEOCODE,
            TS_GEOCODERADIUS,
            TS_QUERY,
            TWITTER_QUERY,
            ID);

    /**
     * This is the DateFormat used for storing and parsing Date objects
     */
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.FULL);

    /**
     * Builds TwitterQuery objects from cursors
     */
    private static final Converter<Cursor, TwitterQuery> CURSOR_TWITTER_QUERY_CONVERTER = new Converter<Cursor, TwitterQuery>() {
        @Override
        public TwitterQuery convert(Cursor cur) {
            long qID = cur.getLong(cur.getColumnIndex("id"));
            TwitterQuery res = new TwitterQuery(qID);
            int column = cur.getColumnIndex(TQ_FROMUSER);
            res.fromUser = getString(cur, column);
            column = cur.getColumnIndex(TQ_FROMLIST);
            res.fromList = getString(cur, column);
            column = cur.getColumnIndex(TQ_QUESTION);
            res.question = getBoolean(cur, column, false);
            column = cur.getColumnIndex(TQ_SINCEDATE);
            res.sinceDate = parseDate(getString(cur, column));
            column = cur.getColumnIndex(TQ_UNTILDATE);
            res.untilDate = parseDate(getString(cur, column));
            column = cur.getColumnIndex(TQ_ATTITUDE);
            res.attitude = cur.isNull(column)?null:TwitterStream.Attitude.valueOf(cur.getString(column));
            column = cur.getColumnIndex(TQ_EXACTPHRASES);
            res.exactPhrases = parseCSV(getString(cur, column));
            column = cur.getColumnIndex(TQ_REMOVE);
            res.remove = parseCSV(getString(cur, column));
            column = cur.getColumnIndex(TQ_HASHTAGS);
            res.hashtags = parseCSV(getString(cur, column));
            return res;
        }
    };

    private static final Converter<TwitterQuery, ContentValues> TWITTER_QUERY_CONTENT_VALUES_CONVERTER = new Converter<TwitterQuery, ContentValues>() {
        @Override
        public ContentValues convert(TwitterQuery query) {
            ContentValues vals = new ContentValues();
            if(query != null) {
                vals.put(ID, query.getID());
                vals.put(TQ_FROMUSER, query.fromUser);
                vals.put(TQ_FROMLIST, query.fromList);
                vals.put(TQ_QUESTION, query.question);
                vals.put(TQ_SINCEDATE, dateToString(query.sinceDate));
                vals.put(TQ_UNTILDATE, dateToString(query.untilDate));
                vals.put(TQ_ATTITUDE, query.attitude.name());
                vals.put(TQ_EXACTPHRASES, toCSV(query.exactPhrases));
                vals.put(TQ_REMOVE, toCSV(query.remove));
                vals.put(TQ_HASHTAGS, toCSV(query.hashtags));
            }
            return vals;
        }
    };

    /**
     * Converter for TwitterStreams. Stored locally due to reliance on non-static methods
     */
    private final Converter<Cursor, TwitterStream> CURSOR_TWITTER_STREAM_CONVERTER = new Converter<Cursor, TwitterStream>() {
        @Override
        public TwitterStream convert(Cursor cur) {
            long thisID = cur.getLong(cur.getColumnIndex("id"));
            int column = cur.getColumnIndex(TS_QUERY);
            TwitterQuery query;
            if(cur.isNull(column)){
                query = new TwitterQuery();
            }else{
                int qID = cur.getInt(column);
                query = twitterQueryFromID(qID);
            }
            TwitterStream res = new TwitterStream(thisID, query);
            column = cur.getColumnIndex(TS_TWEETBATCHSIZE);
            res.tweetBatchSize = cur.getInt(column);
            column = cur.getColumnIndex(TS_DOGEOCODE);
            res.doGeocode = getBoolean(cur, column, false);
            column = cur.getColumnIndex(TS_GEOCODERADIUS);
            if(!cur.isNull(column)) {
                res.setGeocodeRadius(cur.getInt(column));
            }
            return res;
        }
    };

    private boolean saveQuery = true; //Hacky workaround. Need to think of a better way
    private final Converter<TwitterStream, ContentValues> TWITTER_STREAM_CONTENT_VALUES_CONVERTER = new Converter<TwitterStream, ContentValues>() {
        @Override
        public ContentValues convert(TwitterStream stream) {
            ContentValues vals = new ContentValues();
            if(stream != null) {
                vals.put(ID, stream.getID());
                vals.put(TS_TWEETBATCHSIZE, stream.tweetBatchSize);
                vals.put(TS_DOGEOCODE, stream.doGeocode);
                vals.put(TS_GEOCODERADIUS, stream.getGeocodeRadius());
                long qID = stream.query.getID();
                if(saveQuery){
                    qID = save(stream.query);
                }
                vals.put(TS_QUERY, qID);
            }
            return vals;
        }
    };

    /**
     * Convenience method for extracting a boolean from a column
     * @param cur The Cursor pointing at the row
     * @param column The column to extract from
     * @param def The value to return if the column is NULL
     * @return If the column is NULL, the default is returned, otherwise returns true if the value
     *          is non-zero, false otherwise
     */
    private static boolean getBoolean(Cursor cur, int column, boolean def){
        if(cur.isNull(column)){
            return def;
        }
        return cur.getInt(column) != 0;
    }

    /**
     * Convenience method for pulling out text fro a column
     * @param cur The Cursor pointing at the row
     * @param column The column to extract from
     * @return null if the column is NULL, the text otherwise
     */
    private static String getString(Cursor cur, int column){
        return cur.isNull(column)?null:cur.getString(column);
    }

    /**
     * Parses Comma Separated Values (CSV) lists. This is my personal take on CSV, because I
     * hate the "" escape thing. It's ugly and makes for bad code. Mine uses simple backslash
     * escape sequences for commas, double quotes and backslashes.
     * @param csv The CSV source to parse
     * @return A List of strings from the source
     */
    private static List<String> parseCSV(String csv){
        if(csv == null){
            return null;
        }
        List<String> res = new LinkedList<>();
        StringTokenizer tokenizer = new StringTokenizer(csv,",\"\\", true);
        StringBuilder buffer = new StringBuilder();
        boolean inQuotes = false, escape = false;
        while(tokenizer.hasMoreTokens()){
            String curr = tokenizer.nextToken();
            if(escape){
                switch(curr.charAt(0)){
                    case ',':case '"':case '\\':
                        buffer.append(curr);
                        break;
                    default:
                        throw new RuntimeException("Illegal Escape Sequence: \\"+curr);
                }
                escape = false;
            }else{
                switch (curr.charAt(0)){
                    case '\\':
                        escape = true;
                        break;
                    case ',':
                        if(!inQuotes){
                            res.add(buffer.toString());
                            buffer = new StringBuilder();
                        }else{
                            buffer.append(curr);
                        }
                        break;
                    case '"':
                        inQuotes = !inQuotes;
                        break;
                    default:
                        buffer.append(curr);
                        break;
                }
            }
        }
        return res;
    }

    /**
     * Converts a list of strings into a Comma Separated Values (CSV) String according to my
     * escaping rules (see parseCSV(String) for details).
     * @param list The Strings to use
     * @return A CSV string of the listed strings
     */
    private static String toCSV(List<String> list){
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for(String s : list){
            if(!first){
                builder.append(',');
            }
            //Escape those backslashes!
            s = s.replace("\\", "\\\\");
            if(s.contains(",")){
                //Wrap in quotes, escape interior quotes
                builder.append('"').append(s.replace("\"", "\\\"")).append('"');
            }else{
                builder.append(s);
            }
            first = false;
        }
        return builder.toString();
    }

    /**
     * Parses date info from a string
     * @param date The string to parse
     * @return The Date object corresponding to that string
     */
    private static Date parseDate(String date){
        if(date != null) {
            try {
                return DATE_FORMAT.parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Converts a Date object into a String for storage.
     * @param date The Date to convert
     * @return The String corresponding to that Date
     */
    private static String dateToString(Date date){
        if(date == null){
            return null;
        }
        return DATE_FORMAT.format(date);
    }

    private final Context context;
    private final DBOpenHelper openHelper;

    /**
     * Initializes the DatabaseManager
     * @param context The Context in which to start the database
     */
    private DatabaseManager(Context context){
        if(dbmInstance != null){
            throw new RuntimeException("Only one database manager can exist!");
        }
        this.context = context;
        this.openHelper = new DBOpenHelper(context);
    }

    /**
     * Gets all entries from the table and places them in a map, keyed by their IDs
     * @param table The table to pull values from
     * @param converter The converter to turn a cursor object into an entry
     * @param <E> The entry type (e.g. TwitterStream)
     * @return A map from IDs to corresponding entries
     */
    private <E extends Identifiable> Map<Long, E> getAll(String table, Converter<Cursor, E> converter){
        SQLiteDatabase db = this.openHelper.getReadableDatabase();
        Cursor cur = db.query(table, null, null, null, null, null, null);
        Map<Long, E> res = new HashMap<>();
        cur.moveToFirst();
        final int length = cur.getCount();
        for(int i=0; i<length; i++){
            E e = converter.convert(cur);
            res.put(e.getID(), e);
            cur.moveToNext();
        }
        return res;
    }

    /**
     * Fetches a single entry from a table by its ID
     * @param id The ID of the entry you want
     * @param table The name of the table to look into
     * @param conv The converter to turn a cursor object into an entry
     * @param <E> The entry type (e.g. TwitterStream)
     * @return The matching entry or null if it wasn't found
     */
    private <E> E fetchByID(long id, String table, Converter<Cursor, E> conv){
        SQLiteDatabase db = this.openHelper.getReadableDatabase();
        Cursor cur = db.query(table, null, "id=" + id, null, null, null, null);
        if(cur.getCount() == 0){
            return null;
        }
        cur.moveToFirst();
        return conv.convert(cur);
    }

    /**
     * Insert the values into the table
     *
     * @param table The name of the table to insert into
     * @param nullColumnHack optional; may be null. SQL doesn't allow inserting a completely empty
     *                       row without naming at least one column name. If your provided
     *                       initialValues is empty, no column names are known and an empty row
     *                       can't be inserted. If not set to null, the nullColumnHack parameter
     *                       provides the name of nullable column name to explicitly insert a NULL
     *                       into in the case where your initialValues is empty.
     * @param vals The values to insert
     * @return The ID of the entry or -1 if there was an error
     */
    private long insert(String table, String nullColumnHack, ContentValues vals){
        SQLiteDatabase db = openHelper.getWritableDatabase();
        return db.insertWithOnConflict(table, nullColumnHack, vals, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Save the given object into the given table
     *
     * @param table The name of the table to insert into
     * @param nullColumnHack optional; may be null. SQL doesn't allow inserting a completely empty
     *                       row without naming at least one column name. If your provided
     *                       initialValues is empty, no column names are known and an empty row
     *                       can't be inserted. If not set to null, the nullColumnHack parameter
     *                       provides the name of nullable column name to explicitly insert a NULL
     *                       into in the case where your initialValues is empty.
     * @param obj The object to save
     * @param converter The converter to turn it into ContentValues
     * @param <E> The entry type (e.g. TwitterStream)
     * @return The ID of the entry or -1 if there was an error
     */
    private <E extends Identifiable> long save(String table, String nullColumnHack, E obj, Converter<E,ContentValues> converter){
        ContentValues vals = converter.convert(obj);
        return insert(table, nullColumnHack, vals);
    }

    private Map<Long, TwitterQuery> twitterQueryMap = null;

    /**
     * Fetches the TwitterQuery with the corresponding ID
     * @param id The ID of the query to fetch
     * @return The corresponding TwitterQuery
     */
    public TwitterQuery twitterQueryFromID(long id){
        if(twitterQueryMap == null) {
            return fetchByID(id, TWITTER_QUERY, CURSOR_TWITTER_QUERY_CONVERTER);
        }
        return twitterQueryMap.get(id);
    }

    /**
     * Maps all Twitter Queries by their ID
     * @return A Map connecting queries to their IDs
     */
    public Map<Long, TwitterQuery> getAllTwitterQueries(){
        if(twitterQueryMap == null){
            twitterQueryMap = getAll(TWITTER_QUERY, CURSOR_TWITTER_QUERY_CONVERTER);
        }
        return twitterQueryMap;
    }

    /**
     * Save the query in the database and return it's id
     * @param query the query to save
     * @return The ID of the entry or -1 if there was an error
     */
    public long save(TwitterQuery query){
        long res = save(TWITTER_QUERY, TQ_ATTITUDE, query, TWITTER_QUERY_CONTENT_VALUES_CONVERTER);
        if(twitterQueryMap != null){
            twitterQueryMap.put(res, query);
        }
        return res;
    }

    private Map<Long, TwitterStream> twitterStreamMap = null;

    public TwitterStream twitterStreamFromID(long id){
        if(twitterStreamMap == null){
            return fetchByID(id, TWITTER_STREAM, CURSOR_TWITTER_STREAM_CONVERTER);
        }
        return twitterStreamMap.get(id);
    }

    public Map<Long, TwitterStream> getAllTwitterStreams(){
        if(twitterStreamMap == null){
            twitterStreamMap = getAll(TWITTER_STREAM, CURSOR_TWITTER_STREAM_CONVERTER);
        }
        return twitterStreamMap;
    }

    /**
     *
     * @param stream
     * @return The ID of the entry or -1 if there was an error
     */
    public long save(TwitterStream stream){
        return save(stream, true);
    }

    /**
     *
     * @param stream
     * @param saveQuery
     * @return The ID of the entry or -1 if there was an error
     */
    public long save(TwitterStream stream, boolean saveQuery){
        this.saveQuery = saveQuery;
        long res = save(TWITTER_STREAM, TS_DOGEOCODE, stream, TWITTER_STREAM_CONTENT_VALUES_CONVERTER);
        if(twitterStreamMap != null){
            twitterStreamMap.put(res, stream);
        }
        return res;
    }

    private class DBOpenHelper extends SQLiteOpenHelper{

        private DBOpenHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TWITTER_QUERY);
            db.execSQL(CREATE_TWITTER_STREAM);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
