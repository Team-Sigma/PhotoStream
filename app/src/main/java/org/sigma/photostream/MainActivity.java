package org.sigma.photostream;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.PopupWindow;

import org.sigma.photostream.data.DatabaseManager;
import org.sigma.photostream.stream.Stream;
import org.sigma.photostream.stream.TumblrQuery;
import org.sigma.photostream.stream.TumblrStream;
import org.sigma.photostream.stream.TwitterQuery;
import org.sigma.photostream.stream.TwitterStream;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    public static final String EXTRA_STREAM_ID = "EXTRA_STREAM_ID";
    public static final String EXTRA_STREAM_TYPE = "EXTRA_STREAM_TYPE";
    public static MainActivity mainActivity = null;

    public DatabaseManager databaseManager = null;
    public PopupWindow popupWindow = null;

    public GridView gridView = null;
    public Toolbar toolbar = null;
    public FloatingActionButton fab = null;
    public DrawerLayout drawer = null;
    public NavigationView navigationView = null;

    public int minimumImageCount = 30;

    private Stream currentStream = null;

    public List<Stream> availableStreams = new ArrayList<>();

    private List<OnPauseListener> onPauseListeners = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        assert mainActivity == null;
        mainActivity = this;

        databaseManager = DatabaseManager.getInstance(getApplicationContext());

        gridView = (GridView) findViewById(R.id.gridView);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final MainActivity me = this;

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(me, NewStreamActivity.class);
                startActivity(intent);
            }
        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fetchAvailableStreams();
        if(availableStreams.isEmpty()) {
            //TwitterQuery query = new TwitterQuery();
            TumblrQuery query = new TumblrQuery();
            query.exactPhrases.add("food");
            //TwitterStream test = new TwitterStream(this, query);
            TumblrStream test = new TumblrStream(this, query);
            databaseManager.save(test);
            availableStreams.add(test);
        }
        setCurrentStream(availableStreams.get(0));
    }

    private <E extends Stream> void addAll(Map<Long, E> map){
        availableStreams.addAll(map.values());
    }

    private void fetchAvailableStreams(){
        //addAll(databaseManager.getAllTwitterStreams());
        addAll(databaseManager.getAllTumblrStreams());
        //TODO add more streams here
    }

    @Override
    public void onBackPressed() {
        System.out.println("Back pressed!");
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startEditStreamActivity(currentStream);
            return true;
        }else if(id == R.id.action_refresh){
            refresh();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refresh(){
        currentStream.getFlotsamAdapter().removeAll();
        currentStream.refresh();
        startFetching();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_add) {
            Intent intent = new Intent(this, NewStreamActivity.class);
            startActivity(intent);
        }else if( id == R.id.nav_settings){
            Intent intent = new Intent(this, GlobalSettingsActivity.class);
            startActivity(intent);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void startEditStreamActivity(Stream stream){
        assert stream != null;
        Intent intent = new Intent(this, EditStreamActivity.class);
        databaseManager.save(stream);
        intent.putExtra(EXTRA_STREAM_ID, stream.getID());
        intent.putExtra(EXTRA_STREAM_TYPE, stream.getClass().toString());
        startActivity(intent);
    }

    public Stream getCurrentStream() {
        return currentStream;
    }

    private void startFetching(){
        currentStream.getAsyncUntilCountIs(minimumImageCount);
    }

    public void setCurrentStream(Stream stream) {
        if(stream != null) {
            this.currentStream = stream;
            gridView.setAdapter(stream.getFlotsamAdapter());
            startFetching();
        }else{
            throw new NullPointerException("CurrentStream should not be null!");
        }
    }

    public void addOnPauseListener(OnPauseListener listener){
        onPauseListeners.add(listener);
    }

    @Override
    protected void onPause() {
        for(OnPauseListener listener : onPauseListeners){
            listener.onPause(this);
        }
        super.onPause();
    }

    public interface OnPauseListener{
        void onPause(MainActivity mainActivity);
    }
}
