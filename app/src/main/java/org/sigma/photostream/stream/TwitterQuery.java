package org.sigma.photostream.stream;

import android.content.ContentValues;

import org.sigma.photostream.data.DatabaseManager;
import org.sigma.photostream.data.Savable;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Tobias Highfill
 */
public class TwitterQuery implements Savable {

    private static long generateID(){
        DatabaseManager dbm = DatabaseManager.getInstance();
        return dbm.save((TwitterQuery) null);
    }

    public String fromUser = null;
    public String fromList = null;
    public boolean question = false;
    public Date sinceDate = null;
    public Date untilDate = null;
    public TwitterStream.Attitude attitude = null;
    public List<String> exactPhrases = new LinkedList<>();
    public List<String> remove = new LinkedList<>();
    public List<String> hashtags = new LinkedList<>();

    //TODO add the rest of these properties to edit_twitter.xml

    private final long id;

    public TwitterQuery(long id){
        this.id = id;
    }

    public TwitterQuery(){
        this(generateID());
        //Save the defaults as they are
        DatabaseManager.getInstance().save(this);
    }

    private static String ISOformat(Date d){
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        StringBuilder res = new StringBuilder();
        res.append(cal.get(Calendar.YEAR))
                .append('-').append(cal.get(Calendar.MONTH))
                .append('-').append(cal.get(Calendar.DATE));
        return res.toString();
    }

    public String buildQuery(){
        StringBuilder res = new StringBuilder("filter:images ");
        if(fromUser != null && !fromUser.isEmpty()) {
            res.append("from:").append(fromUser.trim()).append(' ');
        }
        if(fromList != null && !fromList.isEmpty()) {
            res.append("list:").append(fromList.trim()).append(' ');
        }
        if(question) {
            res.append("? ");
        }
        if(sinceDate != null){
            res.append("since:").append(ISOformat(sinceDate)).append(' ');
        }
        if(untilDate != null){
            res.append("until:").append(ISOformat(untilDate)).append(' ');
        }
        if(attitude != null){
            res.append(attitude.face).append(' ');
        }
        //Keywords and phrases
        for(String phrase : exactPhrases){
            res.append('"').append(phrase).append("\" ");
        }
        for(String rem : remove){
            res.append('-').append(rem).append(' ');
        }
        for(String hashtag : hashtags){
            res.append('#').append(hashtag).append(' ');
        }
        return res.toString().trim();
    }

    @Override
    public long getID() {
        return id;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues vals = new ContentValues();
        vals.put(DatabaseManager.ID, getID());
        vals.put(DatabaseManager.TQ_FROMUSER, fromUser);
        vals.put(DatabaseManager.TQ_FROMLIST, fromList);
        vals.put(DatabaseManager.TQ_QUESTION, question);
        vals.put(DatabaseManager.TQ_SINCEDATE, DatabaseManager.dateToString(sinceDate));
        vals.put(DatabaseManager.TQ_UNTILDATE, DatabaseManager.dateToString(untilDate));
        vals.put(DatabaseManager.TQ_ATTITUDE, attitude==null?null:attitude.name());
        vals.put(DatabaseManager.TQ_EXACTPHRASES, DatabaseManager.toCSV(exactPhrases));
        vals.put(DatabaseManager.TQ_REMOVE, DatabaseManager.toCSV(remove));
        vals.put(DatabaseManager.TQ_HASHTAGS, DatabaseManager.toCSV(hashtags));
        return vals;
    }

    @Override
    public String getTable() {
        return DatabaseManager.TWITTER_QUERY;
    }

    @Override
    public String nullColumn() {
        return DatabaseManager.TQ_ATTITUDE;
    }
}
