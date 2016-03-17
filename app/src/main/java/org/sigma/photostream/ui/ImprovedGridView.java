package org.sigma.photostream.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Tobias Highfill
 */
public class ImprovedGridView extends GridView{

    List<OnScrollListener> onScrollListeners = new LinkedList<>();

    private void init(){
        super.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                List<OnScrollListener> copy = new ArrayList<>(onScrollListeners);
                for (OnScrollListener listener : copy) {
                    listener.onScrollStateChanged(view, scrollState);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                List<OnScrollListener> copy = new ArrayList<>(onScrollListeners);
                for (OnScrollListener listener : copy) {
                    listener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
            }
        });
    }

    public ImprovedGridView(Context context) {
        super(context);
        init();
    }

    public ImprovedGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImprovedGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ImprovedGridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void addOnScrollListener(AbsListView.OnScrollListener onScrollListener){
        onScrollListeners.add(onScrollListener);
    }

    public void removeOnScrollListener(AbsListView.OnScrollListener onScrollListener){
        onScrollListeners.remove(onScrollListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void setOnScrollListener(OnScrollListener l) {
        super.setOnScrollListener(l);
    }
}
