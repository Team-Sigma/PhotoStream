package org.sigma.photostream.util;

/**
 * @author Tobias Highfill
 */
public class CacheVal<E> {

    private E cachedVal = null;

    private boolean loaded = false;

    private Giver<E> giver;

    public CacheVal(Giver<E> giver){
        this.giver = giver;
    }

    public CacheVal(Giver<E> giver, boolean loadNow){
        this(giver);
        if(loadNow){
            load();
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void unLoad(){
        cachedVal = null;
        loaded = false;
    }

    public void load(){
        cachedVal = giver.give();
        loaded = true;
    }

    public E get(){
        if(!loaded){
            load();
        }
        return cachedVal;
    }

    public E getIfLoaded(E defaultVal){
        return loaded ? cachedVal : defaultVal;
    }
}
