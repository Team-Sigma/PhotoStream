package org.sigma.photostream.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.sigma.photostream.stream.Stream;
import org.sigma.photostream.stream.TumblrQuery;
import org.sigma.photostream.stream.TumblrStream;
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
 * <em>Current version: 2</em>
 * I will maintain a big table of version changes here, to make upgrading as painless as possible.
 * <table>
 *     <tr>
 *         <th>Number</th>
 *         <th>Notes</th>
 *     </tr>
 *     <tr>
 *         <td>1</td>
 *         <td>Added Twitter Query and Twitter Stream tables</td>
 *     </tr>
 *     <tr>
 *         <td>2</td>
 *         <td>Added name column to both tables</td>
 *     </tr>
 *     <tr>
 *         <td>3</td>
 *         <td>Added Tumblr Stream, Tumblr query tables</td>
 *     </tr>
 * </table>
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
    public static final int DATABASE_VERSION = 3;

    /**
     * The full type definition for primary keys in this DB including constraints.
     */
    public static final String PRIMARY_KEY_DEF = "INTEGER PRIMARY KEY ASC ON CONFLICT FAIL AUTOINCREMENT";

    public static final String ID = "id";
    public static final String NAME = "name";

    /**
     * The name of the table storing the Twitter Queries
     */
    public static final String TWITTER_QUERY = "TwitterQuery";

    //The column names
    public static final String TQ_FROMUSER = "fromUser";
    public static final String TQ_FROMLIST = "fromList";
    public static final String TQ_QUESTION = "question";
    public static final String TQ_SINCEDATE = "sinceDate";
    public static final String TQ_UNTILDATE = "untilDate";
    public static final String TQ_ATTITUDE = "attitude";
    public static final String TQ_EXACTPHRASES = "exactPhrases";
    public static final String TQ_REMOVE = "remove";
    public static final String TQ_HASHTAGS = "hashtags";
    //The creation query
    private static final String CREATE_TWITTER_QUERY = String.format(
            "CREATE TABLE IF NOT EXISTS %s (%s %s, %s TEXT , %s TEXT, %s TEXT," +
                    "%s INTEGER, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT)",
            TWITTER_QUERY,
            ID,
            PRIMARY_KEY_DEF,
            NAME,
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
     * The name of the table storing the Tumblr Queries
     */
    public static final String TUMBLR_QUERY = "TumblrQuery";

    //The column names
    public static final String TUQ_FROMUSER = "fromUser";
    public static final String TUQ_FROMLIST = "fromList";
    public static final String TUQ_QUESTION = "question";
    public static final String TUQ_SINCEDATE = "sinceDate";
    public static final String TUQ_UNTILDATE = "untilDate";
    public static final String TUQ_ATTITUDE = "attitude";
    public static final String TUQ_EXACTPHRASES = "exactPhrases";
    public static final String TUQ_REMOVE = "remove";
    public static final String TUQ_HASHTAGS = "hashtags";
    //The creation query
    private static final String CREATE_TUMBLR_QUERY = String.format(
            "CREATE TABLE IF NOT EXISTS %s (%s %s, %s TEXT , %s TEXT, %s TEXT," +
                    "%s INTEGER, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT)",
            TUMBLR_QUERY,
            ID,
            PRIMARY_KEY_DEF,
            NAME,
            TUQ_FROMUSER,
            TUQ_FROMLIST,
            TUQ_QUESTION,
            TUQ_SINCEDATE,
            TUQ_UNTILDATE,
            TUQ_ATTITUDE,
            TUQ_EXACTPHRASES,
            TUQ_REMOVE,
            TUQ_HASHTAGS);

    /**
     * The name of the table storing the Twitter Streams
     */
    public static final String TWITTER_STREAM = "TwitterStream";
    //The column names
    public static final String TS_TWEETBATCHSIZE = "tweetBatchSize";
    public static final String TS_DOGEOCODE = "doGeocode";
    public static final String TS_GEOCODERADIUS = "geocodeRadius";
    public static final String TS_QUERY = "query";
    //The creation query
    private static final String CREATE_TWITTER_STREAM = String.format(
            "CREATE TABLE IF NOT EXISTS %s (%s %s, %s TEXT, %s INTEGER NOT NULL, %s INTEGER," +
                    "%s INTEGER, %s INTEGER REFERENCES %s MATCH %s ON UPDATE SET NULL)",
            TWITTER_STREAM,
            ID,
            PRIMARY_KEY_DEF,
            NAME,
            TS_TWEETBATCHSIZE,
            TS_DOGEOCODE,
            TS_GEOCODERADIUS,
            TS_QUERY,
            TWITTER_QUERY,
            ID);

    /**
     * The name of the table storing the Tumblr Streams
     */
    public static final String TUMBLR_STREAM = "TumblrStream";
    //The column names
    public static final String TUS_POSTBATCHSIZE = "postBatchSize";
    public static final String TUS_DOGEOCODE = "doGeocode";
    public static final String TUS_QUERY = "query";
    //The creation query
    private static final String CREATE_TUMBLR_STREAM = String.format(
            "CREATE TABLE IF NOT EXISTS %s (%s %s, %s TEXT, %s INTEGER NOT NULL, %s INTEGER," +
                    "%s INTEGER REFERENCES %s MATCH %s ON UPDATE SET NULL)",
            TUMBLR_STREAM,
            ID,
            PRIMARY_KEY_DEF,
            NAME,
            TUS_POSTBATCHSIZE,
            TUS_DOGEOCODE,
            TUS_QUERY,
            TUMBLR_QUERY,
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
            column = cur.getColumnIndex(NAME);
            res.name = getString(cur, column, Stream.defaultName(res));
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

    /**
     * Builds TumblrQuery objects from cursors
     */
    private static final Converter<Cursor, TumblrQuery> CURSOR_TUMBLR_QUERY_CONVERTER = new Converter<Cursor, TumblrQuery>() {
        @Override
        public TumblrQuery convert(Cursor cur) {
            long qID = cur.getLong(cur.getColumnIndex("id"));
            TumblrQuery res = new TumblrQuery(qID);
            int column = cur.getColumnIndex(TUQ_FROMUSER);
            res.fromUser = getString(cur, column);
            column = cur.getColumnIndex(TUQ_FROMLIST);
            res.fromList = getString(cur, column);
            column = cur.getColumnIndex(NAME);
            res.name = getString(cur, column, Stream.defaultName(res));
            column = cur.getColumnIndex(TUQ_QUESTION);
            res.question = getBoolean(cur, column, false);
            column = cur.getColumnIndex(TUQ_SINCEDATE);
            res.sinceDate = parseDate(getString(cur, column));
            column = cur.getColumnIndex(TUQ_UNTILDATE);
            res.untilDate = parseDate(getString(cur, column));
            column = cur.getColumnIndex(TUQ_ATTITUDE);
            res.attitude = cur.isNull(column)?null:TumblrStream.Attitude.valueOf(cur.getString(column));
            column = cur.getColumnIndex(TUQ_EXACTPHRASES);
            res.exactPhrases = parseCSV(getString(cur, column));
            column = cur.getColumnIndex(TUQ_REMOVE);
            res.remove = parseCSV(getString(cur, column));
            column = cur.getColumnIndex(TUQ_HASHTAGS);
            res.hashtags = parseCSV(getString(cur, column));
            return res;
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
            column = cur.getColumnIndex(NAME);
            res.name = getString(cur, column, Stream.defaultName(res));
            column = cur.getColumnIndex(TS_DOGEOCODE);
            res.doGeocode = getBoolean(cur, column, false);
            column = cur.getColumnIndex(TS_GEOCODERADIUS);
            if(!cur.isNull(column)) {
                res.setGeocodeRadius(cur.getInt(column));
            }
            return res;
        }
    };

    /**
     * Converter for TumblrStreams. Stored locally due to reliance on non-static methods
     */
    private final Converter<Cursor, TumblrStream> CURSOR_TUMBLR_STREAM_CONVERTER = new Converter<Cursor, TumblrStream>() {
        @Override
        public TumblrStream convert(Cursor cur) {
            long thisID = cur.getLong(cur.getColumnIndex("id"));
            int column = cur.getColumnIndex(TUS_QUERY);
            TumblrQuery query;
            if(cur.isNull(column)){
                query = new TumblrQuery();
            }else{
                int qID = cur.getInt(column);
                query = tumblrQueryFromID(qID);
            }
            TumblrStream res = new TumblrStream(thisID, query);
            column = cur.getColumnIndex(TUS_POSTBATCHSIZE);
            res.postBatchSize = cur.getInt(column);
            column = cur.getColumnIndex(NAME);
            res.name = getString(cur, column, Stream.defaultName(res));
            column = cur.getColumnIndex(TS_DOGEOCODE);
            res.doGeocode = getBoolean(cur, column, false);
            return res;
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
    public static boolean getBoolean(Cursor cur, int column, boolean def){
        if(cur.isNull(column)){
            return def;
        }
        return cur.getInt(column) != 0;
    }

    /**
     * Convenience method for pulling out text from a column
     * @param cur The Cursor pointing at the row
     * @param column The column to extract from
     * @return null if the column is NULL, the text otherwise
     */
    public static String getString(Cursor cur, int column){
        return getString(cur, column, null);
    }

    /**
     * Convenience method for pulling out text from a column with a default
     * @param cur The Cursor pointing at the row
     * @param column The column to extract from
     * @param def The value to return if it is NULL
     * @return The text if the column is not NULL, the default otherwise
     */
    public static String getString(Cursor cur, int column, String def){
        return cur.isNull(column)?def:cur.getString(column);
    }

    /**
     * Parses Comma Separated Values (CSV) lists. This is my personal take on CSV, because I
     * hate the "" escape thing. It's ugly and makes for bad code. Mine uses simple backslash
     * escape sequences for commas, double quotes and backslashes.
     * @param csv The CSV source to parse
     * @return A List of strings from the source
     */
    public static List<String> parseCSV(String csv){
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
    public static String toCSV(List<String> list){
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
    public static Date parseDate(String date){
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
    public static String dateToString(Date date){
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

    /**
     * Saves any savable object.
     * @param obj The object to save
     * @return The ID of the entry or -1 if there was an error
     */
    public long save(Savable obj){
        final Converter<Savable,ContentValues> conv = new Converter<Savable, ContentValues>() {
            @Override
            public ContentValues convert(Savable src) {
                if(src != null) {
                    return src.toContentValues();
                }
                return new ContentValues();
            }
        };
        return save(obj.getTable(), obj.nullColumn(), obj, conv);
    }

    private <E> Converter<E, ContentValues> basicConverter(E e, final ContentValues retVals){
        return new Converter<E, ContentValues>() {
            @Override
            public ContentValues convert(E src) {
                return retVals;
            }
        };
    }

    /**
     * Convenience function for creating empty {@link ContentValues} objects
     * @param e The source object
     * @param <E> The type of the source object
     * @return A converter that only returns an empty {@link ContentValues}
     */
    private <E> Converter<E, ContentValues> basicConverter(E e){
        return basicConverter(e, new ContentValues());
    }

    private Map<Long, TwitterQuery> twitterQueryMap = null;
    private Map<Long, TumblrQuery> tumblrQueryMap = null;

    /**
     * Fetches the {@link TwitterQuery} with the corresponding ID
     * @param id The ID of the query to fetch
     * @return The corresponding {@link TwitterQuery}
     */
    public TwitterQuery twitterQueryFromID(long id){
        if(twitterQueryMap == null) {
            return fetchByID(id, TWITTER_QUERY, CURSOR_TWITTER_QUERY_CONVERTER);
        }
        return twitterQueryMap.get(id);
    }

    /**
     * Fetches the {@link TumblrQuery} with the corresponding ID
     * @param id The ID of the query to fetch
     * @return The corresponding {@link TumblrQuery}
     */
    public TumblrQuery tumblrQueryFromID(long id){
        if(tumblrQueryMap == null) {
            return fetchByID(id, TUMBLR_QUERY, CURSOR_TUMBLR_QUERY_CONVERTER);
        }
        return tumblrQueryMap.get(id);
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
     * Maps all Tumblr Queries by their ID
     * @return A Map connecting queries to their IDs
     */
    public Map<Long, TumblrQuery> getAllTumblrQueries(){
        if(tumblrQueryMap == null){
            tumblrQueryMap = getAll(TUMBLR_QUERY, CURSOR_TUMBLR_QUERY_CONVERTER);
        }
        return tumblrQueryMap;
    }

    /**
     * Save the query in the database and return it's id
     * @param query the query to save
     * @return The ID of the entry or -1 if there was an error
     */
    public long save(TwitterQuery query){
        if(query == null){
            return save(TWITTER_QUERY, TQ_ATTITUDE, null, basicConverter(query));
        }
        long res = save((Savable) query);
        if(twitterQueryMap != null){
            twitterQueryMap.put(res, query);
        }
        return res;
    }

    /**
     * Save the query in the database and return it's id
     * @param query the query to save
     * @return The ID of the entry or -1 if there was an error
     */
    public long save(TumblrQuery query){
        if(query == null){
            return save(TUMBLR_QUERY, TUQ_ATTITUDE, null, basicConverter(query));
        }
        long res = save((Savable) query);
        if(tumblrQueryMap != null){
            tumblrQueryMap.put(res, query);
        }
        return res;
    }

    private Map<Long, TwitterStream> twitterStreamMap = null;

    /**
     * Fetch a single Twitter Stream by its ID
     * @param id The ID of the Twitter Stream to fetch
     * @return The corresponding TwitterStream or null if it can't be found
     */
    public TwitterStream twitterStreamFromID(long id){
        if(twitterStreamMap == null){
            return fetchByID(id, TWITTER_STREAM, CURSOR_TWITTER_STREAM_CONVERTER);
        }
        return twitterStreamMap.get(id);
    }

    private Map<Long, TumblrStream> tumblrStreamMap = null;

    /**
     * Fetch a single Tumblr Stream by its ID
     * @param id The ID of the Tumblr Stream to fetch
     * @return The corresponding TumblrStream or null if it can't be found
     */
    public TumblrStream tumblrStreamFromID(long id){
        if(tumblrStreamMap == null){
            return fetchByID(id, TUMBLR_STREAM, CURSOR_TUMBLR_STREAM_CONVERTER);
        }
        return tumblrStreamMap.get(id);
    }

    /**
     * Maps all Twitter Streams by their IDs
     * @return A mapping of longs to Twitter Streams
     */
    public Map<Long, TwitterStream> getAllTwitterStreams(){
        if(twitterStreamMap == null){
            //Cache the queries
            getAllTwitterQueries();
            //Fetch and cache the streams
            twitterStreamMap = getAll(TWITTER_STREAM, CURSOR_TWITTER_STREAM_CONVERTER);
        }
        return twitterStreamMap;
    }

    /**
     * Maps all Tumblr Streams by their IDs
     * @return A mapping of longs to Tumblr Streams
     */
    public Map<Long, TumblrStream> getAllTumblrStreams(){
        if(tumblrStreamMap == null){
            //Cache the queries
            getAllTumblrQueries();
            //Fetch and cache the streams
            tumblrStreamMap = getAll(TUMBLR_STREAM, CURSOR_TUMBLR_STREAM_CONVERTER);
        }
        return tumblrStreamMap;
    }

    /**
     * Saves the stream and its query
     * @param stream The stream to save
     * @return The ID of the entry or -1 if there was an error
     */
    public long save(TumblrStream stream){
        return save(stream, true);
    }

    /**
     * Saves the stream and its query
     * @param stream The stream to save
     * @return The ID of the entry or -1 if there was an error
     */
    public long save(TwitterStream stream){
        return save(stream, true);
    }

    /**
     * Saves the stream and, optionally, its query
     * @param stream The stream to save
     * @param saveQuery True if you want to save the query as well
     * @return The ID of the entry or -1 if there was an error
     */
    public long save(TwitterStream stream, boolean saveQuery){
        if(stream == null){
            ContentValues vals = new ContentValues();
            vals.put(TS_TWEETBATCHSIZE, 30);
            return save(TWITTER_STREAM, TS_DOGEOCODE, null, basicConverter(stream, vals));
        }
        if(saveQuery){
            save(stream.query);
        }
        long res = save((Savable) stream);
        if(twitterStreamMap != null){
            twitterStreamMap.put(res, stream);
        }
        return res;
    }

    /**
     * Saves the stream and, optionally, its query
     * @param stream The stream to save
     * @param saveQuery True if you want to save the query as well
     * @return The ID of the entry or -1 if there was an error
     */
    public long save(TumblrStream stream, boolean saveQuery){
        if(stream == null){
            ContentValues vals = new ContentValues();
            vals.put(TUS_POSTBATCHSIZE, 30);
            return save(TUMBLR_STREAM, TUS_DOGEOCODE, null, basicConverter(stream, vals));
        }
        if(saveQuery){
            save(stream.query);
        }
        long res = save((Savable) stream);
        if(tumblrStreamMap != null){
            tumblrStreamMap.put(res, stream);
        }
        return res;
    }

    /**
     * Helper class to open the SQLite DB and handle table creation and version upgrades
     *
     * @author Tobias Highfill
     */
    private class DBOpenHelper extends SQLiteOpenHelper{

        private DBOpenHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        private void createTumblrTables(SQLiteDatabase db){
            db.execSQL(CREATE_TUMBLR_QUERY);
            db.execSQL(CREATE_TUMBLR_STREAM);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TWITTER_QUERY);
            db.execSQL(CREATE_TWITTER_STREAM);
            createTumblrTables(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if(oldVersion <= 1 && newVersion >= 2){
                //Add name column to twitter query, twitter stream tables
                for(String table : new String[]{TWITTER_QUERY, TWITTER_STREAM}) {
                    db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT", table, NAME));
                }
                //Add name column to tumblr query, tumblr stream tables
                for(String table : new String[]{TUMBLR_QUERY, TUMBLR_STREAM}) {
                    db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT", table, NAME));
                }
            }
            if(oldVersion <= 2 && newVersion >= 3){
                //Add Tumblr stream, Tumblr query
                createTumblrTables(db);
            }
        }
    }
}
