package org.sigma.photostream.stream;

import android.content.Context;

import org.sigma.photostream.R;
import org.sigma.photostream.data.Savable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tobias Highfill
 */
public abstract class Stream implements Savable {

    public static final int NORMAL = 0;
    public static final int FETCHING = 1;
    public static final int WAITING_FOR_RESET = 2;
    public static final int DOWNLOADING_IMAGES = 3;

    private final long id;

    private int status = NORMAL;
    private int downloadThreads = 0;

    private List<OnUpdateListener> onUpdateListeners = new ArrayList<>();

    private final FlotsamAdapter adapter;

    public Stream(long id, final Context context){
        this.id = id;
        adapter = new FlotsamAdapter(context, R.layout.flotsam);
    }

    /**
     * Discard all images and restart
     */
    public abstract void refresh();

    /**
     * Check if this stream has ended
     * @return true if more images can be loaded, false otherwise
     */
    public abstract boolean hasMoreImages();

    /**
     * Count the currently loaded images
     * @return The current number of images
     */
    public abstract int count();

    /**
     * Get the next image
     * @return The next image
     */
    public abstract Flotsam next();

    /**
     * Return all currently loaded images as a list
     * @return A list of all currently loaded images
     */
    public abstract List<Flotsam> toList();

    protected abstract void onReceiveFlotsam(Flotsam img);

    protected final void receiveFlotsam(Flotsam img){
        adapter.add(img);
        onReceiveFlotsam(img);
    }

    public List<Flotsam> getMany(int count){
        List<Flotsam> res = new ArrayList<>(count);
        for(int i=0; i<count && this.hasMoreImages(); i++){
            res.add(next());
        }
        return res;
    }

    public void getUntilCountIs(int count){
        while(this.count() < count && this.hasMoreImages()){
            next();
        }
    }

    public void getAsyncUntilCountIs(final int count){
        new Thread(new Runnable() {
            @Override
            public void run() {
                getUntilCountIs(count);
            }
        }).start();
    }

    protected void setStatus(int status){
        this.status = status;
    }

    public FlotsamAdapter getFlotsamAdapter(){
        return adapter;
    }

    public int getStatus(){
        return status;
    }

    protected void newDLThread(){
        status = DOWNLOADING_IMAGES;
        downloadThreads++;
    }

    protected void endDLThread(){
        downloadThreads--;
        if(downloadThreads <= 0){
            status = NORMAL;
        }
    }

    public void addOnUpdateListener(OnUpdateListener listener){
        onUpdateListeners.add(listener);
    }

    protected void sendUpdateToListeners(Flotsam img){
        for(OnUpdateListener listener : onUpdateListeners){
            listener.onUpdate(this, img);
        }
    }

    public interface OnUpdateListener{
        void onUpdate(Stream stream, Flotsam img);
    }

    @Override
    public long getID() {
        return id;
    }
}
