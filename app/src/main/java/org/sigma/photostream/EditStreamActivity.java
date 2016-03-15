package org.sigma.photostream;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import org.sigma.photostream.data.DatabaseManager;
import org.sigma.photostream.stream.Stream;
import org.sigma.photostream.stream.TwitterStream;
import org.sigma.photostream.ui.ListEditorDialogFragment;

public class EditStreamActivity extends AppCompatActivity {

    Stream stream = null;
    DatabaseManager databaseManager = null;

    View content = null;

    public ListEditorDialogFragment fragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_stream);

        databaseManager = DatabaseManager.getInstance(getApplicationContext());

        Intent intent = getIntent();
        long id = intent.getLongExtra(MainActivity.EXTRA_STREAM_ID, -1);
        assert id >= 0;
        String type = intent.getStringExtra(MainActivity.EXTRA_STREAM_TYPE);
        assert type != null;

        if(type.equals(TwitterStream.class.toString())){
            stream = databaseManager.twitterStreamFromID(id);
        }
        //TODO add tests for other streams

        if(stream != null){
            ViewGroup root = (ViewGroup) findViewById(R.id.EditStreamRoot);
            content = stream.getEditView(this, root);
//            root.addView(content);
        }
    }
}
