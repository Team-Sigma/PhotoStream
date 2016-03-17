package org.sigma.photostream.util;

import android.os.AsyncTask;

/**
 * Created by mattress on 3/17/2016.
 */
public class TimedCacheVal<E> extends CacheVal<E> {

    public static final long DEFAULT_LIFETIME = 1000*60; //One minute

    private long lifetime = DEFAULT_LIFETIME;
    private long lastAccess = -1;
    private UnloadTask task = new UnloadTask();

    public TimedCacheVal(Giver<E> giver) {
        super(giver);
        task.execute();
    }

    public TimedCacheVal(Giver<E> giver, long lifetime) {
        this(giver);
        this.lifetime = lifetime;
    }

    public TimedCacheVal(Giver<E> giver, long lifetime, boolean loadNow) {
        this(giver, lifetime);
        if(loadNow){
            load();
        }
    }

    public long getLifetime() {
        return lifetime;
    }

    public void setLifetime(long lifetime) {
        if(lifetime > 0) {
            this.lifetime = lifetime;
        }else{
            throw new IllegalArgumentException("Lifetime must be positive and non-zero!");
        }
    }

    @Override
    public void load() {
        lastAccess = System.currentTimeMillis();
        super.load();
    }

    private class UnloadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            while(!isCancelled()){
                while(!isLoaded()){
                    if(isCancelled())
                        return null;
                }
                while(System.currentTimeMillis() - lastAccess <= lifetime){
                    if(isCancelled())
                        return null;
                }
                unLoad();
            }
            return null;
        }
    }
}
