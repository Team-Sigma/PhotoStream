package org.sigma.photostream.stream;

import org.sigma.photostream.data.Identifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tobias Highfill
 */
public abstract class Stream implements Identifiable{

    public static final int NORMAL = 0;
    public static final int FETCHING = 1;
    public static final int WAITING_FOR_RESET = 2;
    public static final int DOWNLOADING_IMAGES = 3;

    private final long id;

    private int status = NORMAL;
    private int downloadThreads = 0;

    private List<OnUpdateListener> onUpdateListeners = new ArrayList<>();

    public Stream(long id){
        this.id = id;
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

    protected abstract void receiveFlotsam(Flotsam img);

    protected void setStatus(int status){
        this.status = status;
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
