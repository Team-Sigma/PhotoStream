package org.sigma.photostream;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.sigma.photostream.stream.Stream;
import org.sigma.photostream.stream.TwitterStream;

public class NewStreamActivity extends AppCompatActivity {

    private static final String TWITTER = "Twitter";

    private static final String[] STREAM_TYPES = {TWITTER};

    ArrayAdapter<String> adapter;
    Spinner spinner = null;

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
        }
        if(s != null){
            MainActivity parent = (MainActivity) this.getParent();
            parent.startEditStreamActivity(s);
            this.finish();
        }
    }
}
