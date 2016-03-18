package org.sigma.photostream.stream;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.sigma.photostream.MainActivity;
import org.sigma.photostream.R;
import org.sigma.photostream.ui.ImprovedGridView;

import java.util.Collection;

/**
 * @author Tobias Highfill
 */
public class FlotsamAdapter extends ArrayAdapter<Flotsam> {

    public FlotsamAdapter(Context context, int resource) {
        super(context, resource);
    }

    public void bindToView(ImprovedGridView view){
        view.setAdapter(this);
        view.addOnScrollListener(new AbsListView.OnScrollListener() {

            boolean idle = true;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                idle = scrollState == SCROLL_STATE_IDLE;
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(!idle)
                    return;
                System.out.println(String.format("FirstVisible: %d, visCount: %d, total: %d",
                        firstVisibleItem, visibleItemCount, totalItemCount));
                boolean visible = false;
                for (int i = 0; i < totalItemCount; i++) {
                    if (i == firstVisibleItem) {
                        visible = true;
                    } else if (i >= firstVisibleItem + visibleItemCount) {
                        visible = false;
                    }
                    Flotsam f = getItem(i);
                    if (visible && !f.isLoaded()) {
                        f.load();
                    }else if(!visible && f.isLoaded()) {
                        f.unLoad();
                    }
                }
            }
        });
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Flotsam f = this.getItem(position);
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.flotsam, parent, false);
            LinearLayout root = (LinearLayout) convertView;
            final ProgressBar progress = (ProgressBar) root.findViewById(R.id.barFlotsam);
            final ImageView img = (ImageView) root.findViewById(R.id.imgFlotsam);
            Flotsam.ImageUpdateListener updateListener = new Flotsam.ImageUpdateListener() {
                @Override
                public void onImageUpdate(Flotsam flotsam) {
                    if(!flotsam.isLoaded()){
                        progress.setVisibility(View.VISIBLE);
                        img.setVisibility(View.INVISIBLE);
                    }else {
                        img.setImageBitmap(flotsam.getImage());
                        img.setMinimumWidth(img.getWidth());
                        img.setMinimumHeight(img.getHeight());
                        progress.setVisibility(View.INVISIBLE);
                        img.setVisibility(View.VISIBLE);
                    }
                }
            };
            img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            img.setPadding(3, 3, 3, 3);
            f.addImageUpdateListener(updateListener);
            updateListener.onImageUpdate(f);
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!f.isLoaded()) {
                        Toast.makeText(getContext(), R.string.wait_for_popup, Toast.LENGTH_SHORT).show();
                    } else {
                        MainActivity main = MainActivity.mainActivity;
                        if (main.popupWindow != null) {
                            main.popupWindow.dismiss();
                        }
                        main.popupWindow = f.popup(getContext());
                        main.popupWindow.showAtLocation(main.gridView, Gravity.CENTER, 0, 0);
                        System.out.println("Flotsam weight: "+ f.getWeight()+ " position: "+position);
                    }
                }
            });
        }
//        f.getImage();
        return convertView;
    }

    public void removeAll(){
        while(!this.isEmpty()){
            this.remove(this.getItem(0));
        }
    }

//    @Override
//    public void add(Flotsam object) {
//        double weight = object.getWeight();
//        final int length = getCount();
//        int start = 0, end = length;
//        while(start < end && start < length){
//            int i = (end - start) / 2;
//            double here = this.getItem(i).getWeight();
//            if(here == weight){
//                this.insert(object, i+1);
//            }else if(weight < here){
//                end = i;
//            }else{
//                start = i+1;
//            }
//        }
//        if(start >= length){
//            super.add(object);
//        }else{
//            insert(object, start);
//        }
//    }
//
//    @Override
//    public void addAll(Collection<? extends Flotsam> collection) {
//        for(Flotsam f : collection){
//            add(f);
//        }
//    }
//
//    @Override
//    public void addAll(Flotsam... items) {
//        for(Flotsam f : items){
//            add(f);
//        }
//    }
}
