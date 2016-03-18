package org.sigma.photostream.stream;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Tobias Highfill
 */
public abstract class BufferedStream extends Stream {

    private static final int LOW_BUFFER = 10;

    private List<Flotsam> images = new LinkedList<>();
    private volatile List<Flotsam> buffer = new LinkedList<>();

    public BufferedStream(long id, Context context) {
        super(id, context);
    }

    protected abstract void fetchMore();

    @Override
    public void refresh() {
        images = new LinkedList<>();
        buffer = new LinkedList<>();
    }

    @Override
    public int count() {
        return images.size();
    }

    @Override
    public Flotsam next() {
        Flotsam res = null;
        List<Flotsam> copy = new ArrayList<>(buffer);
        if(!copy.isEmpty()){
            res = copy.remove(0);
        }
        if(copy.size() <= LOW_BUFFER){
            if(this.getStatus() != Stream.DOWNLOADING_IMAGES) {
                fetchMore();
            }
            if(copy.isEmpty()){
                while (copy.isEmpty()){}
                res = copy.remove(0);
            }
        }
        images.add(res);
        return res;
    }

    @Override
    public List<Flotsam> toList() {
        return new LinkedList<>(images); //Copies the list so no-one can mess with it
    }

    @Override
    protected void onReceiveFlotsam(Flotsam img) {
        buffer.add(img);
        this.sendUpdateToListeners(img);
    }
}
