package org.sigma.photostream.stream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.transition.Fade;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.sigma.photostream.MainActivity;
import org.sigma.photostream.R;
import org.sigma.photostream.util.CacheVal;
import org.sigma.photostream.util.Giver;
import org.sigma.photostream.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public static final boolean DEFAULT_LAZINESS = false;

    private static File DUMP_FOLDER = null;

    private static File getDumpFolder(){
        if(DUMP_FOLDER != null){
            return DUMP_FOLDER;
        }
        File res = new File(MainActivity.mainActivity.getFilesDir(),"img_dump");
        System.out.println("Dump folder: " + res.getAbsolutePath());
        Util.debugAssert(res.mkdirs() || res.isDirectory());
        for(File file : res.listFiles()){
            if(!file.delete()){
                System.out.println("Could not delete: "+file.getAbsolutePath());
            }
        }
        DUMP_FOLDER = res;
        return res;
    }

    private static final Map<URL, File> KNOWN_IMAGES = new HashMap<>();

    private File src = null;
    private CacheVal<Bitmap> img = new CacheVal<>(new Giver<Bitmap>() {
        @Override
        public Bitmap give() {
            if(src == null)
                return null;
            return BitmapFactory.decodeFile(src.getAbsolutePath());
        }
    });
    public String name = "";
    public String description = "";

    public boolean lazy = DEFAULT_LAZINESS;

    private Downloader downloader = null;
    private URL source = null;

    private View popupView = null;

    private List<ImageUpdateListener> imageUpdateListeners = new LinkedList<>();

    public Flotsam(URL src, boolean lazy){
        this.lazy = lazy;
        downloader = new Downloader(this);
        source = src;
        if(!lazy){
            downloader.execute(src);
        }
    }

    public Flotsam(URL src, String name, boolean lazy){
        this(src, lazy);
        this.name = name;
    }

    public Flotsam(URL src, String name, String description, boolean lazy){
        this(src, name, lazy);
        this.description = description;
    }

    public Flotsam(URL src, String name, String description, ImageUpdateListener listener, boolean lazy){
        this(src, name, description, lazy);
        addImageUpdateListener(listener);
    }

    public Flotsam(URL src){
        this(src, DEFAULT_LAZINESS);
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
        return img.get();
    }

    public void addImageUpdateListener(ImageUpdateListener listener){
        imageUpdateListeners.add(listener);
    }

    protected View getPopupView(Context context){
        if(popupView != null){
            return popupView;
        }
        View.OnKeyListener dismiss = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK){
                    MainActivity.mainActivity.popupWindow.dismiss();
                    return true;
                }
                return false;
            }
        };

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popupView = inflater.inflate(R.layout.flotsam_popup, null);
        popupView.setVisibility(View.VISIBLE);
        popupView.setOnKeyListener(dismiss);

        TextView txtName = (TextView) popupView.findViewById(R.id.txtFlotsamName);
        txtName.setText(name);
        txtName.setOnKeyListener(dismiss);

        TextView txtDesc = (TextView) popupView.findViewById(R.id.txtFlotsamDesc);
        txtDesc.setText(description);
        txtDesc.setOnKeyListener(dismiss);

        final ImageView imgFlotsam = (ImageView) popupView.findViewById(R.id.imgFlotsamPopup);
        this.addImageUpdateListener(new ImageUpdateListener() {
            @Override
            public void onImageUpdate(Flotsam flotsam) {
                imgFlotsam.setImageBitmap(flotsam.getImage());
            }
        });
        imgFlotsam.setImageBitmap(this.getImage());
        imgFlotsam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mainActivity.popupWindow.dismiss();
            }
        });
        imgFlotsam.setOnKeyListener(dismiss);

        return popupView;
    }

    public PopupWindow popup(Context context){
        View v = getPopupView(context);
        PopupWindow res = new PopupWindow(v,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        res.setFocusable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            res.setEnterTransition(new Fade(Fade.IN));
            res.setExitTransition(new Fade(Fade.OUT));
        }
        return res;
    }

    public interface ImageUpdateListener{
        void onImageUpdate(Flotsam flotsam);
    }

    private class Downloader extends AsyncTask<URL, Integer, File>{

        private Flotsam parent;

        private Downloader(Flotsam parent){
            this.parent = parent;
        }

        @Override
        protected File doInBackground(URL... params) {
            if(KNOWN_IMAGES.containsKey(params[0])){
                return KNOWN_IMAGES.get(params[0]);
            }
            String[] dotParts = params[0].toString().split("\\.");
            String suffix = dotParts[dotParts.length-1];
            File dest;
            try {
                dest = File.createTempFile("img"+Math.random()*10000000, suffix, getDumpFolder());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            System.out.println("Downloading image at "+params[0].toString()+" to "+dest.getAbsolutePath());
            try {
                InputStream input = params[0].openStream();
                FileOutputStream output = new FileOutputStream(dest);
                byte data[] = new byte[4096];
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    output.write(data, 0, count);
                }
                input.close();
                output.close();
                KNOWN_IMAGES.put(params[0], dest);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return dest;
        }

        @Override
        protected void onPostExecute(File src) {
            parent.src = src;
            parent.img.load();
            for(ImageUpdateListener listener : imageUpdateListeners){
                listener.onImageUpdate(parent);
            }
        }
    }
}
