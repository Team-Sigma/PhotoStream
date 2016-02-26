package org.sigma.photostream.stream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Tobias Highfill
 */
public class Flotsam {

    private static final Map<URL, Bitmap> KNOWN_IMAGES = new HashMap<>();

    private Bitmap img;
    public String name = "";
    public String description = "";

    private List<ImageUpdateListener> imageUpdateListeners = new LinkedList<>();

    public Flotsam(Bitmap img){
        this.img = img;
    }

    public Flotsam(Bitmap img, String name){
        this(img);
        this.name = name;
    }

    public Flotsam(Bitmap img, String name, String description){
        this(img, name);
        this.description = description;
    }

    public Flotsam(URL src){
        new Downloader(this).execute(src);
    }

    public Flotsam(URL src, String name){
        this(src);
        this.name = name;
    }

    public Flotsam(URL src, String name, String description){
        this(src, name);
        this.description = description;
    }

    public Flotsam(URL src, String name, String description, ImageUpdateListener listener){
        this(src, name, description);
        addImageUpdateListener(listener);
    }

    public Flotsam(String src) throws MalformedURLException {
        this(new URL(src));
    }

    public Flotsam(String src, String name) throws MalformedURLException {
        this(src);
        this.name = name;
    }

    public Flotsam(String src, String name, String description) throws MalformedURLException {
        this(src, name);
        this.description = description;
    }

    public Bitmap getImage() {
        return img;
    }

    public void addImageUpdateListener(ImageUpdateListener listener){
        imageUpdateListeners.add(listener);
    }

    public interface ImageUpdateListener{
        void onImageUpdate(Flotsam flotsam);
    }

    private class Downloader extends AsyncTask<URL, Integer, Bitmap>{

        private Flotsam parent;

        private Downloader(Flotsam parent){
            this.parent = parent;
        }

        @Override
        protected Bitmap doInBackground(URL... params) {
            if(KNOWN_IMAGES.containsKey(params[0])){
                return KNOWN_IMAGES.get(params[0]);
            }
            System.out.println("Downloading image at "+params[0].toString());
            Bitmap res = null;
            try {
                res = BitmapFactory.decodeStream(params[0].openStream());
                KNOWN_IMAGES.put(params[0], res);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return res;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            parent.img = bitmap;
            for(ImageUpdateListener listener : imageUpdateListeners){
                listener.onImageUpdate(parent);
            }
        }
    }
}
