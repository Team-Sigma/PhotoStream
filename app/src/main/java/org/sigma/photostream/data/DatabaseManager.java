package org.sigma.photostream.data;

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
 * Created by mattress on 2/18/2016.
 */
public class DatabaseManager {

    public static final String DATABASE_NAME = "PhotoStreamDB";
    public static final int DATABASE_VERSION = 1;

    public static final String PRIMARY_KEY_DEF = "INTEGER PRIMARY KEY ASC ON CONFLICT FAIL AUTOINCREMENT";
    public static final String TWITTER_QUERY = "TwitterQuery";
    private static final String CREATE_TWITTER_QUERY = String.format(
            "CREATE TABLE IF NOT EXISTS %s (id %s , fromUser TEXT, fromList TEXT," +
                    "question INTEGER, sinceDate TEXT, untilDate TEXT, attitude TEXT," +
                    "exactPhrases TEXT, remove TEXT, hashtags TEXT)",
            TWITTER_QUERY,
            PRIMARY_KEY_DEF);
    public static final String TWITTER_STREAM = "TwitterStream";
    private static final String CREATE_TWITTER_STREAM = String.format(
            "CREATE TABLE IF NOT EXISTS %s (id %s, tweetBatchSize INTEGER, doGeocode INTEGER," +
                    "geocodeRadius INTEGER, query INTEGER REFERENCES %s MATCH id ON UPDATE SET NULL)",
            TWITTER_STREAM,
            PRIMARY_KEY_DEF,
            TWITTER_QUERY);

    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.FULL);

    private static final Converter<Cursor, TwitterQuery> CURSOR_TWITTER_QUERY_CONVERTER = new Converter<Cursor, TwitterQuery>() {
        @Override
        public TwitterQuery convert(Cursor cur) {
            int qID = cur.getInt(cur.getColumnIndex("id"));
            TwitterQuery res = new TwitterQuery(qID);
            int column = cur.getColumnIndex("fromUser");
            res.fromUser = getString(cur, column);
            column = cur.getColumnIndex("fromList");
            res.fromList = getString(cur, column);
            column = cur.getColumnIndex("question");
            res.question = !cur.isNull(column) && cur.getInt(column) != 0;
            column = cur.getColumnIndex("sinceDate");
            res.sinceDate = parseDate(getString(cur, column));
            column = cur.getColumnIndex("untilDate");
            res.untilDate = parseDate(getString(cur, column));
            column = cur.getColumnIndex("attitude");
            res.attitude = cur.isNull(column)?null:TwitterStream.Attitude.valueOf(cur.getString(column));
            column = cur.getColumnIndex("exactPhrases");
            res.exactPhrases = parseCSV(getString(cur, column));
            column = cur.getColumnIndex("remove");
            res.remove = parseCSV(getString(cur, column));
            column = cur.getColumnIndex("hashtags");
            res.hashtags = parseCSV(getString(cur, column));
            return res;
        }
    };

    private static String getString(Cursor cur, int column){
        return cur.isNull(column)?null:cur.getString(column);
    }

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

    private static String dateToString(Date date){
        if(date == null){
            return null;
        }
        return DATE_FORMAT.format(date);
    }

    private final Context context;
    private final DBOpenHelper openHelper;

    public DatabaseManager(Context context){
        this.context = context;
        this.openHelper = new DBOpenHelper(context);
    }

    private <E extends Identifiable> Map<Integer, E> getAll(String table, Converter<Cursor, E> conv){
        SQLiteDatabase db = this.openHelper.getReadableDatabase();
        Cursor cur = db.query(table, null, null, null, null, null, null);
        Map<Integer, E> res = new HashMap<>();
        cur.moveToFirst();
        final int length = cur.getCount();
        for(int i=0; i<length; i++){
            E e = conv.convert(cur);
            res.put(e.getID(), e);
            cur.moveToNext();
        }
        return res;
    }

    private <E> E fromID(int id, String table, Converter<Cursor, E> conv){
        SQLiteDatabase db = this.openHelper.getReadableDatabase();
        Cursor cur = db.query(table, null, "id=" + id, null, null, null, null);
        cur.moveToFirst();
        return conv.convert(cur);
    }

    public TwitterQuery twitterQueryFromID(int id){
        return fromID(id, TWITTER_QUERY, CURSOR_TWITTER_QUERY_CONVERTER);
    }

    public Map<Integer, TwitterQuery> getAllTwitterQueries(){
        return getAll(TWITTER_QUERY, CURSOR_TWITTER_QUERY_CONVERTER);
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
