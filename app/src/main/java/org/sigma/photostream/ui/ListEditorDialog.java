package org.sigma.photostream.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import org.sigma.photostream.R;
import org.sigma.photostream.util.Receiver;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a dialog for editing a list of Strings
 * @author TObias Highfill
 */
public class ListEditorDialog extends AlertDialog {

    List<String> items = null;

    Receiver<List<String>> receiver = null;

    protected ListEditorDialog(Context context) {
        super(context);
    }

    protected ListEditorDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    protected ListEditorDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public ListEditorDialog(Context context, List<String> items) {
        this(context);
        this.items = items;
    }

    public ListEditorDialog(Context context, boolean cancelable, OnCancelListener cancelListener, List<String> items) {
        this(context, cancelable, cancelListener);
        this.items = items;
    }

    public ListEditorDialog(Context context, int themeResId, List<String> items) {
        this(context, themeResId);
        this.items = items;
    }

    public void setReceiver(Receiver<List<String>> receiver) {
        this.receiver = receiver;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_editor_dialog);

        assert items != null;

        ImageButton btnAddItem = (ImageButton) findViewById(R.id.btnAddItem);
        final EditText txtNewItem = (EditText) findViewById(R.id.txtNewItem);
        ListView lstItems = (ListView) findViewById(R.id.lstItems);

        final ListEditorAdapter adapter = new ListEditorAdapter(getContext());
        adapter.addAll(items);
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

        setIcon(0);

        setButton(AlertDialog.BUTTON_POSITIVE, "Done", new OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(receiver != null){
                    ArrayList<String> res = new ArrayList<>(adapter.getCount());
                    for(int i=0; i<adapter.getCount(); i++){
                        res.add(adapter.getItem(i));
                    }
                    receiver.receive(res);
                }
                dismiss();
            }
        });
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
    }
}
