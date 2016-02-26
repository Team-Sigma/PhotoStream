package org.sigma.photostream.stream;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.sigma.photostream.R;

/**
 * Created by mattress on 2/25/2016.
 */
public class FlotsamAdapter extends ArrayAdapter<Flotsam> {

    public FlotsamAdapter(Context context, int resource) {
        super(context, resource);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.flotsam, parent, false);
        }
        Flotsam f = this.getItem(position);
        LinearLayout root = (LinearLayout) convertView;
        ProgressBar progress = (ProgressBar) root.findViewById(R.id.barFlotsam);
        ImageView img = (ImageView) root.findViewById(R.id.imgFlotsam);
        if(f.getImage() == null){
            progress.setVisibility(View.VISIBLE);
            img.setVisibility(View.INVISIBLE);
        }else{
            img.setImageBitmap(f.getImage());
            progress.setVisibility(View.INVISIBLE);
            img.setVisibility(View.VISIBLE);
        }
        return convertView;
    }
}
