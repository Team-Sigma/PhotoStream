package org.sigma.photostream.stream;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.temboo.Library.Tumblr.Tagged.RetrievePostsWithTag.RetrievePostsWithTagInputSet;
import com.temboo.Library.Tumblr.Tagged.RetrievePostsWithTag.RetrievePostsWithTagResultSet;
import com.temboo.Library.Tumblr.Tagged.RetrievePostsWithTag;
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
import java.util.Set;
import java.util.TreeSet;

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
    private int geocodeRadius = 1;

   public TumblrQuery query;

    private int remaining = 100;
    private long resetAt = 0;

    private Set<String> ids = new TreeSet<>();

    private View editView = null;

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

    protected void fetchMore(){
        new TumblrFetcher(this).execute(query.buildQuery());
    }

    @Override
    public boolean hasMoreImages() {
        return true;
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
        if(editView!= null){
            return editView;
        }

        final Context context = activity.getApplicationContext();
        final TumblrStream me = this;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        editView = inflater.inflate(R.layout.edit_tumblr, parent, true);

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
        numBatchSize.setText(String.format("%d", postBatchSize));
        numBatchSize.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String input = numBatchSize.getText().toString();
                    int newval = input.isEmpty() ? postBatchSize : Integer.parseInt(input);
                    if(newval > 0 && newval <=100) {
                        postBatchSize = newval;
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
        POSITIVE(":)"), NEGATIVE(":("), NONE("");
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
            if (parent.remaining <= 0) {
                //Wait for the reset
                parent.setStatus(WAITING_FOR_RESET);
                while (System.currentTimeMillis() <= parent.resetAt) {
                }
            }
            System.out.println("Running Tumblr query: \"" + params[0] + '"');

            RetrievePostsWithTag choreo = new RetrievePostsWithTag(TembooStream.getSession());
            RetrievePostsWithTagInputSet inputs = choreo.newInputSet();

            inputs.set_APIKey(CONSUMER_KEY);
            inputs.set_Tag(SEARCH_TERM);
            inputs.set_Limit(postBatchSize);

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
            try {
                new PostProcessor(parent).execute(root.getJSONArray("response"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class PostProcessor extends AsyncTask<JSONArray, Flotsam, Void>{

        TumblrStream parent;

        PostProcessor(TumblrStream parent){
            this.parent = parent;
        }

        @Override
        protected void onProgressUpdate(Flotsam... values) {
            parent.receiveFlotsam(values[0]);
            parent.newDLThread();
        }

        @Override
        protected Void doInBackground(JSONArray... params) {
            JSONArray posts = params[0];
            parent.setStatus(NORMAL);
            try {
                //This is a big array of tumblr posts
                System.out.println("Processing "+posts.length()+" posts...");
                for(int i=0; i < posts.length(); i++){
                    JSONObject post = posts.getJSONObject(i);
                    //Need to check if tweet contains a photo
                    //The filter should have taken care of it but better safe than sorry
                    System.out.println("testing post");
                    if(post.has("image_permalink")) {
                        System.out.println("got photo");
                        processPost(post);
                    }
                }
            } catch (JSONException | MalformedURLException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void processPost(JSONObject post) throws JSONException, MalformedURLException {
            String id = post.getString("id");
            int notes = post.getInt("note_count");
            if(ids.contains(id)){
                return;
            }
            else
            {
                ids.add(id);
                //This will approach 1 for large values
                double weight = 1.0 - Math.pow(1.0 - 1 / 50.0, notes+1);
                //Tweet has a photo, download it asynchronously
                String user = post.getString("blog_name");
                String name = "Post from " + user;
                //Use tweet body as description
                String description = post.getString("summary");
                System.out.println("Found post! User: "+ user);
                String urlString = post.getJSONArray("photos").getJSONObject(0).getJSONArray("alt_sizes").getJSONObject(0).getString("url");

                URL url = new URL(urlString);
                Flotsam.ImageUpdateListener listener = new Flotsam.ImageUpdateListener() {
                    @Override
                    public void onImageUpdate(Flotsam flotsam) {
                        parent.endDLThread();
                        flotsam.removeImageUpdateListener(this);
                    }
                };
                Flotsam res = new Flotsam(url, name, description, listener, false);
                res.setWeight(weight);
                publishProgress(res);
            }
        }
    }
}
