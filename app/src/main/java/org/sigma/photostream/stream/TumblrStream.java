package org.sigma.photostream.stream;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.temboo.Library.Tumblr.Tagged.RetrievePostsWithTag.RetrievePostsWithTagInputSet;
import com.temboo.Library.Tumblr.Tagged.RetrievePostsWithTag.RetrievePostsWithTagResultSet;
import com.temboo.Library.Tumblr.Tagged.RetrievePostsWithTag;
import com.temboo.core.TembooException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sigma.photostream.MainActivity;
import org.sigma.photostream.R;
import org.sigma.photostream.data.DatabaseManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mmust_000 on 3/3/2016.
 */
public class TumblrStream extends TembooStream{


   private final string CONSUMER_KEY = "QS4RzgCb9Eby8YYTX9y3LcqqkOP18fNG20G133AsIOs3aVVCds";

    private static final int LOW_BUFFER = 10;

    private static long generateID(){
        DatabaseManager dbm = DatabaseManager.getInstance();
        return dbm.save((TumblrStream) null);
    }
    
    //Anywhere from 1 to 20
    public int postBatchSize = 20;

    public boolean doGeocode = false;
    private int geocodeRadius = 1;

    public TumblrQuery query;

    private int remaining = 100;
    private long resetAt = 0;

    private List<Flotsam> images = new LinkedList<>();
    private volatile List<Flotsam> buffer = new LinkedList<>();

    public TumblrStream(long id, Context context, TumblrQuery query){
        super(id, context);
        this.query = query;
    }

    public TumblrStream(long id, Context context){
        this(id, context, new TumblrQuery());
    }

    public TumblrStream(Context context, TumblrQuery query){
        this(generateID(), context, query);
        //Save right now to update the DB
        DatabaseManager.getInstance().save(this);
    }

    public TumblrStream(Context context){
        this(context, new TumblrQuery());
    }

    public TumblrStream(long id, TumblrQuery query){
        this(id, MainActivity.mainActivity, query);
    }

    public TumblrStream(long id){
        this(id, new TumblrQuery());
    }

    public TumblrStream(TumblrQuery query){
        this(generateID(), query);
        //Save right now to update the DB
        DatabaseManager.getInstance().save(this);
    }

    public TumblrStream(){
        this(new TumblrQuery());
    }

    private void fetchMore(){
        new TumblrFetcher(this).execute(query.buildQuery());
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
    public View getEditView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.edit_tumblr, null);
        //TODO add in functionality
        return root;
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
        vals.put(DatabaseManager.TS_TUMBLRBATCHSIZE, tumblrBatchSize);
        vals.put(DatabaseManager.TS_DOGEOCODE, doGeocode);
        vals.put(DatabaseManager.TS_GEOCODERADIUS, getGeocodeRadius());
        vals.put(DatabaseManager.TS_QUERY, query.getID());
        return vals;
    }

    @Override
    public String getTable() {
        return DatabaseManager.TUMBLR_STREAM;
    }

    @Override
    public String nullColumn() {
        return DatabaseManager.TS_GEOCODERADIUS;
    }

    private class TumblrFetcher extends AsyncTask<String, Void, JSONObject> {
        final TumblrStream parent;

        public TumblrFetcher(TumblrStream tumblrStream) {
            this.parent = tumblrStream;
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            if(parent.remaining <= 0){
                //Wait for the reset
                parent.setStatus(WAITING_FOR_RESET);
                while (System.currentTimeMillis() <= parent.resetAt){}
            }
            System.out.println("Running Tumblr query: \""+params[0]+'"');

            RetrievePostsWithTag choreo = new Tweets(TembooStream.getSession());
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
                    try {
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
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            } catch (JSONException | MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }
}



