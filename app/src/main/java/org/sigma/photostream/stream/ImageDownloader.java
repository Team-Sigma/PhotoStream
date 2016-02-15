package org.sigma.photostream.stream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.URL;

/**
 * @author Tobias Highfill
 */
public class ImageDownloader extends AsyncTask<URL, Integer, Flotsam> {

    final Stream parent;
    private String name = "";
    private String description = "";

    public ImageDownloader(Stream parent){
        this.parent = parent;
    }

    public ImageDownloader(Stream parent, String name) {
        this(parent);
        this.name = name;
    }

    public ImageDownloader(Stream parent, String name, String description){
        this(parent, name);
        this.description = description;
    }

    @Override
    protected void onPreExecute() {
        parent.newDLThread();
    }

    @Override
    protected Flotsam doInBackground(URL... params) {
        try {
            Bitmap img = BitmapFactory.decodeStream(params[0].openStream());
            return new Flotsam(img, name, description);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Flotsam bitmap) {
        parent.receiveFlotsam(bitmap);
        parent.endDLThread();
    }
}
