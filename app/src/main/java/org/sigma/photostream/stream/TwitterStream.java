package org.sigma.photostream.stream;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.temboo.Library.Twitter.Search.Tweets;
import com.temboo.core.TembooException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sigma.photostream.EditStreamActivity;
import org.sigma.photostream.MainActivity;
import org.sigma.photostream.R;
import org.sigma.photostream.data.DatabaseManager;
import org.sigma.photostream.ui.ListEditorDialogFragment;
import org.sigma.photostream.util.Receiver;
import org.sigma.photostream.util.Transceiver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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

    private View editView = null;

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
//        fetchMore();
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

    private void saveToast(Context context){
        Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show();
    }

    private void updatePreview(TextView lblPreview){
        lblPreview.setText(String.format("Preview: \"%s\"", query.buildQuery()));
    }

    private void setUpDatePicker(final View root, int chkID, int lblID, int btnID, Date initial, final Receiver<Date> receiver){
        final CheckBox chkEnable = (CheckBox) root.findViewById(chkID);
        final TextView lblPre = (TextView) root.findViewById(lblID);
        final Button btnChange = (Button) root.findViewById(btnID);
        chkEnable.setChecked(initial != null);
        View.OnClickListener enableListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chkEnable.isChecked()){
                    Date newDate = new Date();
                    receiver.receive(newDate);
                    lblPre.setText(newDate.toString());
                    lblPre.setVisibility(View.VISIBLE);
                    btnChange.setVisibility(View.VISIBLE);
                }else{
                    receiver.receive(null);
                    lblPre.setVisibility(View.GONE);
                    btnChange.setVisibility(View.GONE);
                }
            }
        };
        chkEnable.setOnClickListener(enableListener);
        enableListener.onClick(null);
        final DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Date newDate = new GregorianCalendar(year, monthOfYear, dayOfMonth).getTime();
                receiver.receive(newDate);
                lblPre.setText(newDate.toString());
            }
        };
        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GregorianCalendar cal = new GregorianCalendar();
                DatePickerDialog dialog = new DatePickerDialog(root.getContext(), onDateSetListener,
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });
    }

    private void setUpListEditor(final EditStreamActivity activity, final String tag, final View root, int btnID, final Transceiver<List<String>> trans){
        Button btnEdit = (Button) root.findViewById(btnID);
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.fragment = new ListEditorDialogFragment();
                activity.fragment.setReceiver(trans);
                activity.fragment.setItems(trans.give());
                activity.fragment.show(activity.getSupportFragmentManager(), tag);
            }
        });
    }

    @Override
    public View getEditView(EditStreamActivity activity, ViewGroup parent) {
        if(editView != null){ //This ensures that we only do this set-up once per stream
            return editView;
        }
        final Context context = activity.getApplicationContext();
        final TwitterStream me = this;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        editView = inflater.inflate(R.layout.edit_twitter, parent, true);

        final EditText txtName = (EditText) editView.findViewById(R.id.txtStreamName);
        txtName.setText(this.name);
        txtName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus && MainActivity.mainActivity.checkName(txtName, me)) {
                    me.name = txtName.getText().toString();
                    saveSelf();
                    saveToast(context);
                }
            }
        });

        final EditText numBatchSize = (EditText) editView.findViewById(R.id.numTweetBatchSize);
        numBatchSize.setText(String.format("%d", tweetBatchSize));
        numBatchSize.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String input = numBatchSize.getText().toString();
                    int newval = input.isEmpty() ? tweetBatchSize : Integer.parseInt(input);
                    if(newval > 0 && newval <=100) {
                        tweetBatchSize = newval;
                        saveSelf();
                        saveToast(context);
                    }else{
                        Toast.makeText(context,
                                "Illegal batch size! Must be greater than 0 and less than 101",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        final CheckBox chkDoGeocode = (CheckBox) editView.findViewById(R.id.chkDoGeocode);
        final EditText numRadius = (EditText) editView.findViewById(R.id.numRadius);
        chkDoGeocode.setChecked(doGeocode);
        chkDoGeocode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doGeocode = chkDoGeocode.isChecked();
                numRadius.setEnabled(doGeocode);
                saveSelf();
                saveToast(context);
            }
        });
        numRadius.setEnabled(doGeocode);
        numRadius.setText(String.format("%d", geocodeRadius));
        numRadius.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String input = numRadius.getText().toString();
                    geocodeRadius = input.isEmpty() ? 0 : Integer.parseInt(input);
                    saveSelf();
                    saveToast(context);
                }
            }
        });

        final TextView lblPreview = (TextView) editView.findViewById(R.id.lblPreview);
        updatePreview(lblPreview);

        final EditText txtFromUser = (EditText) editView.findViewById(R.id.txtFromUser);
        txtFromUser.setText(query.fromUser);
        txtFromUser.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    me.query.fromUser = txtFromUser.getText().toString();
                    me.query.saveSelf();
                    saveToast(context);
                    updatePreview(lblPreview);
                }
            }
        });

        final EditText txtFromList = (EditText) editView.findViewById(R.id.txtFromList);
        txtFromList.setText(query.fromList);
        txtFromList.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    me.query.fromList = txtFromList.getText().toString();
                    me.query.saveSelf();
                    saveToast(context);
                    updatePreview(lblPreview);
                }
            }
        });

        final CheckBox chkQuestion = (CheckBox) editView.findViewById(R.id.chkQuestion);
        chkQuestion.setChecked(query.question);
        chkQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.query.question = chkQuestion.isChecked();
                me.query.saveSelf();
                saveToast(context);
                updatePreview(lblPreview);
            }
        });

        setUpDatePicker(editView,
                R.id.chkEnableSinceDate,
                R.id.lblSinceDatePre,
                R.id.btnSinceDateChange,
                me.query.sinceDate,
                new Receiver<Date>() {
                    @Override
                    public void receive(Date newval) {
                        me.query.sinceDate = newval;
                        me.query.saveSelf();
                        saveToast(context);
                        updatePreview(lblPreview);
                    }
                });

        setUpDatePicker(editView,
                R.id.chkEnableUntilDate,
                R.id.lblUntilDatePre,
                R.id.btnUntilDateChange,
                me.query.untilDate,
                new Receiver<Date>() {
                    @Override
                    public void receive(Date newval) {
                        me.query.untilDate = newval;
                        me.query.saveSelf();
                        saveToast(context);
                        updatePreview(lblPreview);
                    }
                });

        Spinner spnAttitude = (Spinner) editView.findViewById(R.id.spnAttitude);
        final ArrayAdapter<Attitude> attAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
        for(Attitude att : new Attitude[]{Attitude.NONE, Attitude.POSITIVE, Attitude.NEGATIVE}){
            if(att == me.query.attitude){
                attAdapter.insert(att, 0);//Put in front
            }else{
                attAdapter.add(att);
            }
        }
        spnAttitude.setAdapter(attAdapter);
        spnAttitude.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                me.query.attitude = attAdapter.getItem(position);
                me.query.saveSelf();
                saveToast(context);
                updatePreview(lblPreview);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                me.query.attitude = null;
                me.query.saveSelf();
                saveToast(context);
                updatePreview(lblPreview);
            }
        });

        setUpListEditor(activity, "exactPhrases", editView, R.id.btnEditExactPhrases, new Transceiver<List<String>>() {
            @Override
            public List<String> give() {
                return me.query.exactPhrases;
            }

            @Override
            public void receive(List<String> list) {
                me.query.exactPhrases = list;
                me.query.saveSelf();
                saveToast(context);
                updatePreview(lblPreview);
            }
        });

        setUpListEditor(activity, "remove", editView, R.id.btnEditRemove, new Transceiver<List<String>>() {
            @Override
            public List<String> give() {
                return me.query.remove;
            }

            @Override
            public void receive(List<String> list) {
                me.query.exactPhrases = list;
                me.query.saveSelf();
                saveToast(context);
                updatePreview(lblPreview);
            }
        });

        setUpListEditor(activity, "hashtags",editView, R.id.btnEditHashtags, new Transceiver<List<String>>() {
            @Override
            public List<String> give() {
                return me.query.hashtags;
            }

            @Override
            public void receive(List<String> list) {
                me.query.exactPhrases = list;
                me.query.saveSelf();
                saveToast(context);
                updatePreview(lblPreview);
            }
        });
        return editView;
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
    public ContentValues toContentValues() {
        ContentValues vals = new ContentValues();
        vals.put(DatabaseManager.ID, getID());
        vals.put(DatabaseManager.NAME, name);
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
        POSITIVE(":)"), NEGATIVE(":("), NONE("");
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
                                parent.receiveFlotsam(new Flotsam(url, name, description, listener, false));
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
