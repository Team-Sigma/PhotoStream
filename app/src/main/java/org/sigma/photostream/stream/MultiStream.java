package org.sigma.photostream.stream;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.sigma.photostream.EditStreamActivity;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by mattress on 3/18/2016.
 */
public class MultiStream extends BufferedStream {

    List<Stream> streams = new LinkedList<>();
    int index = 0;
    int batchSize = 30;

    public MultiStream(long id, Context context) {
        super(id, context);
    }

    @Override
    public boolean hasMoreImages() {
        for(Stream stream : streams){
            if(stream.hasMoreImages())
                return true;
        }
        return false;
    }

    @Override
    public View getEditView(EditStreamActivity activity, ViewGroup parent) {
        return null;
    }

    @Override
    protected void fetchMore() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i=0; i<batchSize; i++) {
                    index = index % streams.size();
                    receiveFlotsam(streams.get(index).next());
                    index++;
                }
                endDLThread();
            }
        }).start();
    }

    @Override
    public ContentValues toContentValues() {
        return null;
    }

    @Override
    public String getTable() {
        return null;
    }

    @Override
    public String nullColumn() {
        return null;
    }
}
