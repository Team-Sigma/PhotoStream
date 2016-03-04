package org.sigma.photostream.stream;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.sigma.photostream.MainActivity;
import org.sigma.photostream.R;

/**
 * @author Tobias Highfill
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
        final Flotsam f = this.getItem(position);
        LinearLayout root = (LinearLayout) convertView;
        ProgressBar progress = (ProgressBar) root.findViewById(R.id.barFlotsam);
        final ImageView img = (ImageView) root.findViewById(R.id.imgFlotsam);
        if(f.getImage() == null){
            progress.setVisibility(View.VISIBLE);
            img.setVisibility(View.INVISIBLE);
        }else{
            img.setImageBitmap(f.getImage());
            progress.setVisibility(View.GONE);
            img.setVisibility(View.VISIBLE);
        }
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (f.getImage() == null) {
                    Toast.makeText(getContext(), R.string.wait_for_popup, Toast.LENGTH_SHORT).show();
                } else {
                    MainActivity main = MainActivity.mainActivity;
                    if (main.popupWindow != null) {
                        main.popupWindow.dismiss();
                    }
                    main.popupWindow = f.popup(getContext());
                    main.popupWindow.showAtLocation(main.gridView, Gravity.CENTER, 0, 0);
                }
            }
        });
        return convertView;
    }

    public void removeAll(){
        while(!this.isEmpty()){
            this.remove(this.getItem(0));
        }
    }
}
