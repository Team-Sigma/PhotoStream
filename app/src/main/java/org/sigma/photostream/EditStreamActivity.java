package org.sigma.photostream;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.sigma.photostream.data.DatabaseManager;
import org.sigma.photostream.stream.Stream;
import org.sigma.photostream.stream.TwitterStream;

public class EditStreamActivity extends AppCompatActivity {

    Stream stream = null;
    DatabaseManager databaseManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_stream);

        databaseManager = DatabaseManager.getInstance(getApplicationContext());

        Intent intent = getIntent();
        long id = intent.getLongArrayExtra(MainActivity.EXTRA_STREAM_ID)[0];
        String type = intent.getStringExtra(MainActivity.EXTRA_STREAM_TYPE);
        assert type != null;

        if(type.equals(TwitterStream.class.toString())){
            stream = databaseManager.twitterStreamFromID(id);
        }
        //TODO add tests for other streams


    }
}
