package org.sigma.photostream.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mattress on 3/15/2016.
 */
public class CacheVal<E> {

    public static final long DEFAULT_LIFETIME = 1000*60; //One minute

    private Timer timer = new Timer();
    private long lifetime = DEFAULT_LIFETIME;
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            unLoad();
        }
    };

    private E cachedVal = null;
    private boolean loaded = false;

    private Giver<E> giver;

    public CacheVal(Giver<E> giver){
        this.giver = giver;
    }

    public CacheVal(Giver<E> giver, long lifetime){
        this(giver);
        this.lifetime = lifetime;
    }

    public CacheVal(Giver<E> giver, long lifetime, boolean loadNow){
        this(giver, lifetime);
        if(loadNow){
            load();
        }
    }

    public void unLoad(){
        cachedVal = null;
        loaded = false;
    }

    public void load(){
        timer.cancel();
        timer.purge();
        timer = new Timer();
        cachedVal = giver.give();
        loaded = true;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                unLoad();
            }
        }, lifetime);
    }

    public E get(){
        if(!loaded){
            load();
        }
        return cachedVal;
    }
}
