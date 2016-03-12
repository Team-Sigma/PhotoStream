package org.sigma.photostream.util;

import java.util.LinkedList;
import java.util.List;

/**
 * Neat utility class for a variable that drives events whenever it is accessed or set
 *
 * @author Tobias Highfill
 * @param <E> The type of data to store
 */
public class AlertVar<E> {

    private E data;
    public final boolean immutable;

    //Listeners
    private List<OnGetListener<E>> onGetListeners = new LinkedList<>();
    private List<OnSetListener<E>> onSetListeners = new LinkedList<>();

    /**
     * Create new AlertVar
     * @param data Data to store
     * @param immutable Set to true to disallow any set operations
     */
    public AlertVar(E data, boolean immutable){
        this.data = data;
        this.immutable = immutable;
    }

    /**
     * Create new mutable AlertVar
     * @param data Data to store
     */
    public AlertVar(E data){
        this(data, false);
    }

    /**
     * Create new mutable AlertVar initialised to null.
     */
    public AlertVar(){
        this(null);
    }

    /**
     * Get the data. Triggers all {@link org.sigma.photostream.util.AlertVar.OnGetListener}s.
     * @return The data stored here
     */
    public E get() {
        for(OnGetListener<E> listener : onGetListeners){
            listener.onGet(this);
        }
        return data;
    }

    /**
     * Set the data. Triggers all {@link org.sigma.photostream.util.AlertVar.OnSetListener}s.
     * @param data The new data to store
     */
    public void set(E data) {
        if(immutable){
            throw new RuntimeException("This variable is immutable!");
        }
        E old = this.data;
        this.data = data;
        for(OnSetListener<E> listener : onSetListeners){
            listener.onSet(this, old, data);
        }
    }

    /**
     * Add a new {@link org.sigma.photostream.util.AlertVar.OnGetListener}
     * @param listener {@link org.sigma.photostream.util.AlertVar.OnGetListener} to add
     */
    public void addOnGetListener(OnGetListener<E> listener){
        onGetListeners.add(0, listener); //Insert in front for constant time (I hope)
    }

    /**
     * Add a new {@link org.sigma.photostream.util.AlertVar.OnSetListener}
     * @param listener {@link org.sigma.photostream.util.AlertVar.OnSetListener} to add
     */
    public void addOnSetListener(OnSetListener<E> listener){
        onSetListeners.add(0, listener); //Insert in front for constant time (I hope)
    }

    /**
     * Add a new {@link org.sigma.photostream.util.AlertVar.OnChangeListener}
     * @param listener {@link org.sigma.photostream.util.AlertVar.OnChangeListener} to add
     */
    public void addOnChangeListener(OnChangeListener<E> listener){
        addOnGetListener(listener);
        addOnSetListener(listener);
    }

    /**
     * Interface for receiving get events
     * @param <E> The type of data in the {@link AlertVar}
     */
    public interface OnGetListener<E>{
        void onGet(AlertVar<E> var);
    }

    /**
     * Interface for receiving set events
     * @param <E> The type of data in the {@link AlertVar}
     */
    public interface OnSetListener<E>{
        void onSet(AlertVar<E> var, E oldVal, E newVal);
    }

    /**
     * Interface for receiving both get and set events
     * @param <E> The type of data in the {@link AlertVar}
     */
    public interface OnChangeListener<E> extends OnGetListener<E>, OnSetListener<E>{
        //Nothing!!
    }

}
