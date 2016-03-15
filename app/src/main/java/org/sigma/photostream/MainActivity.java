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
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.PopupWindow;

import org.sigma.photostream.data.DatabaseManager;
import org.sigma.photostream.stream.Flotsam;
import org.sigma.photostream.stream.Stream;
import org.sigma.photostream.stream.StreamList;
import org.sigma.photostream.stream.TumblrQuery;
import org.sigma.photostream.stream.TumblrStream;
import org.sigma.photostream.stream.TwitterQuery;
import org.sigma.photostream.stream.TwitterStream;
import org.sigma.photostream.util.Receiver;

import java.util.ArrayList;
import java.util.Collections;
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
    public Menu streamMenu = null;

    public int minimumImageCount = 30;

    private Stream currentStream = null;
    private boolean refilling = false;

    public StreamList availableStreams;

    private List<OnPauseListener> onPauseListeners = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        assert mainActivity == null;
        mainActivity = this;

        databaseManager = DatabaseManager.getInstance(getApplicationContext());

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        streamMenu = navigationView.getMenu();
        String title = getString(R.string.drawer_streams);
        for(int i=0; i<streamMenu.size(); i++){
            MenuItem item = streamMenu.getItem(i);
            if(item.getTitle().equals(title)){
                streamMenu = item.getSubMenu();
            }
        }
        availableStreams = new StreamList(this);

        gridView = (GridView) findViewById(R.id.gridView);
        final int SAFETY = 10, BATCH_SIZE = 30;
        final Receiver<List<Flotsam>> onComplete = new Receiver<List<Flotsam>>() {
            @Override
            public void receive(List<Flotsam> flotsams) {
                refilling = false;
                System.out.println("Finished refill");
            }
        };
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(visibleItemCount != 0
                        && firstVisibleItem >= totalItemCount - visibleItemCount - SAFETY
                        && !refilling){
                    System.out.println("Running getManyAsync("+BATCH_SIZE+")...");
                    refilling = true;
                    currentStream.getManyAsync(BATCH_SIZE, onComplete);
                }
            }
        });

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

        //DANGER!!! For testing purposes only!!
//        databaseManager.nukeDB();
        //DANGER!!

        fetchAvailableStreams();
        if(availableStreams.isEmpty()) {
            TwitterQuery query = new TwitterQuery();
//            TumblrQuery query = new TumblrQuery();
            query.exactPhrases.add("food");
            TwitterStream test = new TwitterStream(this, query);
//            TumblrStream test = new TumblrStream(this, query);
            databaseManager.save(test);
            availableStreams.add(test);
        }
        setCurrentStream(availableStreams.get(0));

        testCSV();
    }

    private void testCSV(){
        List<String> orig = new LinkedList<>();
        Collections.addAll(orig, "foo", "bar", "foo,bar", "\"foo\",bar");
        String csv = DatabaseManager.toCSV(orig);
        System.out.println("CSV = "+csv);
        List<String> parsed = DatabaseManager.parseCSV(csv);
        assert orig.size() == parsed.size();
        for(int i=0; i<parsed.size();i++){
            assert orig.get(i).equals(parsed.get(i));
        }
    }

    private <E extends Stream> void addAll(Map<Long, E> map){
        availableStreams.addAll(map.values());
    }

    private void fetchAvailableStreams(){
        addAll(databaseManager.getAllTwitterStreams());
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
