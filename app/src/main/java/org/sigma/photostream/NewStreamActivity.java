package org.sigma.photostream;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.sigma.photostream.stream.RedditStream;
import org.sigma.photostream.stream.Stream;
import org.sigma.photostream.stream.TumblrStream;
import org.sigma.photostream.stream.TwitterStream;

public class NewStreamActivity extends AppCompatActivity {

    private static final String TWITTER = "Twitter";
    private static final String TUMBLR = "Tumblr";
    private static final String REDDIT = "Reddit";

    private static final String[] STREAM_TYPES = {TWITTER, TUMBLR, REDDIT};

    ArrayAdapter<String> adapter;
    Spinner spinner = null;
    EditText txtStreamName = null;

    private String current = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_stream);

        adapter  = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item);

        spinner = (Spinner) findViewById(R.id.spnStreamSelect);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                current = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                current = null;
            }
        });

        for(String type : STREAM_TYPES){
            adapter.add(type);
        }

        Button btnCreate = (Button) findViewById(R.id.btnNewStream);
        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                create();
            }
        });

        txtStreamName = (EditText) findViewById(R.id.txtStreamName);
        txtStreamName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    MainActivity.mainActivity.checkName(txtStreamName, null);
                }
            }
        });
    }

    private void create(){
        Stream s = null;
        if(current == null){
            Toast.makeText(
                    getApplicationContext(),
                    "Please select a type of stream first",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if(current.equals(TWITTER)){
            s = new TwitterStream();
        }else if(current.equals(TUMBLR)){
            s = new TumblrStream();
        }else if(current.equals(REDDIT)){
            s = new RedditStream();
        }
        //TODO: Add more as they are created
        if(s != null){
            s.name = txtStreamName.getText().toString();
            MainActivity.mainActivity.availableStreams.add(s);
            MainActivity.mainActivity.startEditStreamActivity(s);
            this.finish();
        }
    }
}
