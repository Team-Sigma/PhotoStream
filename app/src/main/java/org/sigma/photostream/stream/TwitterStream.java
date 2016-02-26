package org.sigma.photostream.stream;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import com.temboo.Library.Twitter.Search.Tweets;
import com.temboo.core.TembooException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sigma.photostream.MainActivity;
import org.sigma.photostream.data.DatabaseManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Tobias Highfill
 */
public class TwitterStream extends TembooStream {
    private static final String CONSUMER_KEY = "DI9Ych7tJdLusNJBEyFgjlmmF";
    private static final String CONSUMER_KEY_SECRET = "EproeCZvr4zibBkuywPgnRmwFBMTi7LgPX4G3eax6wAFAfVpXN";
    private static final String ACCESS_TOKEN = "41432906-btJbtBnk8MsB8ch1xMfqcSL8o1jqJnNkOHK8GVBZQ";
    private static final String ACCESS_TOKEN_SECRET = "trx0y7zwMDvIZEbitTDkFnlXNgwx2GAn1zE3zEepnQsrf";

    private static final int LOW_BUFFER = 10;

    private static long generateID(){
        DatabaseManager dbm = DatabaseManager.getInstance();
        return dbm.save((TwitterStream) null);
    }

    public int tweetBatchSize = 30;

    public boolean doGeocode = false;
    private int geocodeRadius = 1;

    public TwitterQuery query;

    private int remaining = 100;
    private long resetAt = 0;

    private List<Flotsam> images = new LinkedList<>();
    private volatile List<Flotsam> buffer = new LinkedList<>();

    public TwitterStream(long id, Context context, TwitterQuery query){
        super(id, context);
        this.query = query;
    }

    public TwitterStream(long id, Context context){
        this(id, context, new TwitterQuery());
    }

    public TwitterStream(Context context, TwitterQuery query){
        this(generateID(), context, query);
        //Save right now to update the DB
        DatabaseManager.getInstance().save(this);
    }

    public TwitterStream(Context context){
        this(context, new TwitterQuery());
    }

    public TwitterStream(long id, TwitterQuery query){
        this(id, MainActivity.mainActivity, query);
    }

    public TwitterStream(long id){
        this(id, new TwitterQuery());
    }

    public TwitterStream(TwitterQuery query){
        this(generateID(), query);
        //Save right now to update the DB
        DatabaseManager.getInstance().save(this);
    }

    public TwitterStream(){
        this(new TwitterQuery());
    }

    private void fetchMore(){
        new TwitterFetcher(this).execute(query.buildQuery());
    }

    @Override
    public void refresh() {
        images = new LinkedList<>();
        buffer = new LinkedList<>();
        fetchMore();
    }

    @Override
    public boolean hasMoreImages() {
        return true;
    }

    @Override
    public int count() {
        return images.size();
    }

    @Override
    public Flotsam next() {
        Flotsam res = null;
        if(!buffer.isEmpty()){
            res = buffer.remove(0);
        }
        if(buffer.size() <= LOW_BUFFER){
            fetchMore();
            if(buffer.isEmpty()){
                while (buffer.isEmpty()){}
                res = buffer.remove(0);
            }
        }
        images.add(res);
        return res;
    }

    @Override
    public List<Flotsam> toList() {
        return new LinkedList<>(images); //Copies the list so no-one can mess with it
    }

    @Override
    protected void onReceiveFlotsam(Flotsam img) {
        buffer.add(img);
        this.sendUpdateToListeners(img);
    }

    public int getGeocodeRadius() {
        return geocodeRadius;
    }

    public void setGeocodeRadius(int geocodeRadius) {
        if(geocodeRadius <= 0){
            throw new IllegalArgumentException("Radius must be positive and non-zero!");
        }
        this.geocodeRadius = geocodeRadius;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues vals = new ContentValues();
        vals.put(DatabaseManager.ID, getID());
        vals.put(DatabaseManager.TS_TWEETBATCHSIZE, tweetBatchSize);
        vals.put(DatabaseManager.TS_DOGEOCODE, doGeocode);
        vals.put(DatabaseManager.TS_GEOCODERADIUS, getGeocodeRadius());
        vals.put(DatabaseManager.TS_QUERY, query.getID());
        return vals;
    }

    @Override
    public String getTable() {
        return DatabaseManager.TWITTER_STREAM;
    }

    @Override
    public String nullColumn() {
        return DatabaseManager.TS_GEOCODERADIUS;
    }

    public enum Attitude{
        POSITIVE(":)"), NEGATIVE(":(");
        public final String face;
        Attitude(String face){
            this.face = face;
        }
    }

    private class TwitterFetcher extends AsyncTask<String, Void, JSONObject>{
        final TwitterStream parent;

        public TwitterFetcher(TwitterStream twitterStream) {
            this.parent = twitterStream;
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            if(parent.remaining <= 0){
                //Wait for the reset
                parent.setStatus(WAITING_FOR_RESET);
                while (System.currentTimeMillis() <= parent.resetAt){}
            }
            System.out.println("Running Twitter query: \""+params[0]+'"');

            Tweets choreo = new Tweets(TembooStream.getSession());
            Tweets.TweetsInputSet inputs = choreo.newInputSet();

            inputs.set_AccessToken(ACCESS_TOKEN);
            inputs.set_AccessTokenSecret(ACCESS_TOKEN_SECRET);
            inputs.set_ConsumerKey(CONSUMER_KEY);
            inputs.set_ConsumerSecret(CONSUMER_KEY_SECRET);
            inputs.set_Query(params[0]);
            inputs.set_Count(parent.tweetBatchSize);
            if(parent.doGeocode){
                //TODO add geolookup stuff here
                inputs.set_Geocode(","+parent.geocodeRadius+"mi.");
            }

            parent.setStatus(FETCHING);
            try {
                Tweets.TweetsResultSet results = choreo.execute(inputs);
                parent.remaining = Integer.parseInt(results.get_Remaining());
                parent.resetAt = System.currentTimeMillis() + Long.parseLong(results.get_Reset());
                return new JSONObject(results.get_Response());
            } catch (JSONException | TembooException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject root) {
            parent.setStatus(NORMAL);
            try {
                JSONArray tweets = root.getJSONArray("statuses");
                //This is a big array of tweets
                System.out.println("Processing "+tweets.length()+" tweets...");
                for(int i=0; i < tweets.length(); i++){
                    JSONObject tweet = tweets.getJSONObject(i);
                    String user = tweet.getJSONObject("user").getString("name");
                    //Need to check if tweet contains a photo
                    //The filter should have taken care of it but better safe than sorry
                    JSONArray media = tweet.getJSONObject("entities").getJSONArray("media");
                    for(int j = 0; j < media.length(); j++){
                        JSONObject curr = media.getJSONObject(j);
                        if(curr.getString("type").equals("photo")){
                            //Tweet has a photo, download it asynchronously
                            //Use tweet body as description
                            String name = "Tweet from " + user;
                            String description = tweet.getString("text");
                            URL url = new URL(curr.getString("media_url_https"));
                            Flotsam.ImageUpdateListener listener = new Flotsam.ImageUpdateListener() {
                                @Override
                                public void onImageUpdate(Flotsam flotsam) {
                                    parent.endDLThread();
                                }
                            };
                            parent.receiveFlotsam(new Flotsam(url, name, description, listener));
                            parent.newDLThread();
                            break;
                        }
                    }
                }
            } catch (JSONException | MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }
}
