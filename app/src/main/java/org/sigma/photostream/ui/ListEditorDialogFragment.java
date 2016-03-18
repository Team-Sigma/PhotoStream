package org.sigma.photostream.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import org.sigma.photostream.R;
import org.sigma.photostream.util.Receiver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a dialog for editing a list of Strings
 * @author TObias Highfill
 */
public class ListEditorDialogFragment extends DialogFragment {

    List<String> items = null;

    Receiver<List<String>> receiver = null;
    private ImageButton btnAddItem = null;
    private EditText txtNewItem = null;
    private ListView lstItems = null;
    private ListEditorAdapter adapter = null;

    public void setReceiver(Receiver<List<String>> receiver) {
        this.receiver = receiver;
    }

    public void setItems(List<String> items) {
        if(items == null){
            items = new LinkedList<>();
        }
        this.items = items;
        if(adapter != null){
            adapter.removeAll();
            adapter.addAll(items);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.list_editor_dialog, null, false);
        assert items != null;

        btnAddItem = (ImageButton) root.findViewById(R.id.btnAddItem);
        txtNewItem = (EditText) root.findViewById(R.id.txtNewItem);
        lstItems = (ListView) root.findViewById(R.id.lstItems);

        if(adapter == null){
            adapter = new ListEditorAdapter(getContext());
        }
        setItems(items);
        lstItems.setAdapter(adapter);

        final View.OnClickListener addItem = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newItem = txtNewItem.getText().toString();
                if(!newItem.isEmpty()) {
                    adapter.add(newItem);
                    txtNewItem.setText("");
                    txtNewItem.requestFocus();
                }
            }
        };
        btnAddItem.setOnClickListener(addItem);
        txtNewItem.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    addItem.onClick(null);
                    return true;
                }
                return false;
            }
        });

        builder.setTitle("Edit list")
                .setIcon(0)
                .setView(root)
                .setPositiveButton(R.string.action_done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (receiver != null) {
                            addItem.onClick(null);
                            ArrayList<String> res = new ArrayList<>(adapter.getCount());
                            for (int i = 0; i < adapter.getCount(); i++) {
                                res.add(adapter.getItem(i));
                            }
                            receiver.receive(res);
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }

    private class ListEditorAdapter extends ArrayAdapter<String>{

        public ListEditorAdapter(Context context) {
            super(context, R.layout.list_item, R.id.lblItemContent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = super.getView(position, convertView, parent);
            ImageButton btnRemoveItem = (ImageButton) convertView.findViewById(R.id.btnRemoveItem);
            final String item = getItem(position);
            btnRemoveItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(item);
                }
            });
            return convertView;
        }

        void removeAll(){
            while(!this.isEmpty()){
                this.remove(this.getItem(0));
            }
        }
    }
}
