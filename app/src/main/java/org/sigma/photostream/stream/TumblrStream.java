package org.sigma.photostream.stream;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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


    private static final String CONSUMER_KEY = "QS4RzgCb9Eby8YYTX9y3LcqqkOP18fNG20G133AsIOs3aVVCds";
    private static final String SEARCH_TERM = "Anime";

    private static final int LOW_BUFFER = 10;

    private static long generateID(){
        DatabaseManager dbm = DatabaseManager.getInstance();
        return dbm.save((TumblrStream) null);
    }

    //Anywhere from 1 to 20
    public int postBatchSize = 20;

    public boolean doGeocode = false;

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
            if(this.getStatus() != Stream.DOWNLOADING_IMAGES) {
                fetchMore();
            }
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
    public View getEditView(Context context, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.edit_tumblr, parent, false);
        //TODO add in functionality
        return root;
    }

    @Override
    protected void onReceiveFlotsam(Flotsam img) {
        buffer.add(img);
        this.sendUpdateToListeners(img);
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues vals = new ContentValues();
        vals.put(DatabaseManager.ID, getID());
        vals.put(DatabaseManager.NAME, name);
        vals.put(DatabaseManager.TUS_POSTBATCHSIZE, postBatchSize);
        vals.put(DatabaseManager.TUS_DOGEOCODE, doGeocode);
        vals.put(DatabaseManager.TUS_QUERY, query.getID());
        return vals;
    }

    @Override
    public String getTable() {
        return DatabaseManager.TUMBLR_STREAM;
    }

    @Override
    public String nullColumn() {
        return null;
    }

    public enum Attitude{
        POSITIVE(":)"), NEGATIVE(":(");
        public final String face;
        Attitude(String face){
            this.face = face;
        }
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

            RetrievePostsWithTag choreo = new RetrievePostsWithTag(TembooStream.getSession());
            RetrievePostsWithTagInputSet inputs = choreo.newInputSet();

            inputs.set_APIKey(CONSUMER_KEY);
            inputs.set_Tag(SEARCH_TERM);
            inputs.set_Limit(20);

            parent.setStatus(FETCHING);
            try {
                RetrievePostsWithTagResultSet results = choreo.execute(inputs);
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
                JSONArray posts = root.getJSONArray("response");
                //This is a big array of tumblr posts
                System.out.println("Processing "+posts.length()+" posts...");
                for(int i=0; i < posts.length(); i++){
                    JSONObject post = posts.getJSONObject(i);
                    String user = post.getString("blog_name");
                    //Need to check if tweet contains a photo
                    //The filter should have taken care of it but better safe than sorry
                    try {

                        if(post.has("image_permalink"))
                        {
                            //Post has a photo, download it asynchronously
                            //Use post body as description
                            String name = "Post from " + user;
                            String description = post.getString("summary");
                            System.out.println("Found post! User: "+ user);
                            String urlString = post.getJSONArray("photos").getJSONObject(0).getJSONArray("alt_sizes").getJSONObject(0).getString("url");
                            URL url = new URL(urlString);
                            Flotsam.ImageUpdateListener listener = new Flotsam.ImageUpdateListener() {
                                @Override
                                public void onImageUpdate(Flotsam flotsam) {
                                    parent.endDLThread();
                                }
                            };
                            parent.receiveFlotsam(new Flotsam(url, name, description, listener));
                            parent.newDLThread();
                            //break; //<-- there's your problem!
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
