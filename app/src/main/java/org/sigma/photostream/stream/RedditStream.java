package org.sigma.photostream.stream;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;

import org.sigma.photostream.EditStreamActivity;
import org.sigma.photostream.MainActivity;
import org.sigma.photostream.data.DatabaseManager;
import org.sigma.photostream.util.Util;

import java.net.MalformedURLException;

/**
 * @author Tobias Highfill
 */
public class RedditStream extends BufferedStream {

    private static long generateID(){
        return DatabaseManager.getInstance().save((RedditStream) null);
    }

    public static final String DEFAULT_SUBREDDIT = "pics";
    public static final Sorting DEFAULT_SORTING = Sorting.HOT;

    public static final String[] IMAGE_SUFFIXES = {
            ".jpg", ".jpeg", ".gif", ".png", ".bmp"
    };

    private static final UserAgent USER_AGENT = UserAgent.of("android", "org.sigma.photostream", "v1.0.0", "team-sigma");
    private static final RedditClient REDDIT_CLIENT = new RedditClient(USER_AGENT);
    private static final Credentials CREDENTIALS = Credentials.installedApp("", "http://localhost:9281"); //TODO: Fill this in
    private static final OAuthData OAUTH_DATA = initOAuthData();

    private static OAuthData initOAuthData(){
        try {
            OAuthData res = REDDIT_CLIENT.getOAuthHelper().easyAuth(CREDENTIALS);
            REDDIT_CLIENT.authenticate(res);
            return res;
        } catch (OAuthException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String subreddit = DEFAULT_SUBREDDIT;
    public boolean allowNSFW = false;
    public Sorting sorting = DEFAULT_SORTING;

    SubredditPaginator paginator = null;

    int maxPages = 5;

    public RedditStream(long id, Context context) {
        super(id, context);
    }

    public RedditStream(long id){
        this(id, MainActivity.mainActivity);
    }

    public RedditStream(){
        this(generateID());
        //Save immediately!
        DatabaseManager.getInstance().save(this);
    }

    @Override
    protected void fetchMore() {
        new RedditFetcher().execute();
    }

    @Override
    public boolean hasMoreImages() {
        return false;
    }

    @Override
    public View getEditView(EditStreamActivity activity, ViewGroup parent) {
        return null;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues vals = new ContentValues();
        vals.put(DatabaseManager.ID, getID());
        vals.put(DatabaseManager.NAME, name);
        vals.put(DatabaseManager.RED_SUBREDDIT, subreddit);
        vals.put(DatabaseManager.RED_ALLOW_NSFW, allowNSFW);
        vals.put(DatabaseManager.RED_SORTING, sorting.toString());
        return vals;
    }

    @Override
    public String getTable() {
        return DatabaseManager.REDDIT_STREAM;
    }

    @Override
    public String nullColumn() {
        return DatabaseManager.RED_ALLOW_NSFW;
    }

    private class RedditFetcher extends AsyncTask<Void, Flotsam, Void> {

        @Override
        protected void onProgressUpdate(Flotsam... values) {
            receiveFlotsam(values[0]);
            newDLThread();
        }

        private void processSubmission(Submission submission){
            if((submission.isNsfw() && !allowNSFW) || submission.isSelfPost())
                return;
            String urlstr = submission.getUrl();
            for(String suffix : IMAGE_SUFFIXES){
                if(urlstr.endsWith(suffix)){
                    try {
                        Flotsam.ImageUpdateListener listener = new Flotsam.ImageUpdateListener() {
                            @Override
                            public void onImageUpdate(Flotsam flotsam) {
                                endDLThread();
                            }
                        };
                        Flotsam flotsam = new Flotsam(urlstr, submission.getTitle(),
                                String.format("Posted by %s on %s", submission.getAuthor(), subreddit), listener);
                        publishProgress(flotsam);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            if(paginator == null){
                Util.debugAssert(subreddit != null);
                paginator = new SubredditPaginator(REDDIT_CLIENT, subreddit);
                paginator.setSorting(sorting);
            }
            if(!paginator.hasStarted()){
                for(Submission submission : paginator.accumulateMerged(maxPages)){
                    processSubmission(submission);
                }
            }else{
                for(Submission submission : paginator.next(false)){
                    processSubmission(submission);
                }
            }
            return null;
        }
    }
}
