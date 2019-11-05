// Dialog with restore file list
package space.aqoleg.bookkeeper;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DialogRestore extends DialogFragment implements AdapterView.OnItemClickListener {
    private static final String ATTRIBUTE_NAME = "AN";
    private ArrayList<Map<String, String>> fileList = new ArrayList<>();

    static DialogRestore newInstance() {
        DialogRestore dialog = new DialogRestore();
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_restore, container, false);
        File[] files = new File(Environment.getExternalStorageDirectory(), Data.BACKUP_FOLDER).listFiles();
        if (files != null) {
            Map<String, String> map;
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith(Data.BACKUP_PREFIX) && name.endsWith(Data.BACKUP_SUFFIX)) {
                    map = new HashMap<>();
                    map.put(ATTRIBUTE_NAME, name);
                    fileList.add(map);
                }
            }
            ((ListView) view.findViewById(R.id.list)).setAdapter(new Adapter(getActivity()));
            ((ListView) view.findViewById(R.id.list)).setOnItemClickListener(this);
        }
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String name = fileList.get(position).get(ATTRIBUTE_NAME);
        if (((ActivityMain) getActivity()).restore(name)) {
            dismiss();
        }
    }

    private class Adapter extends SimpleAdapter {
        private final SimpleDateFormat sdf;

        Adapter(Context context) {
            super(context, fileList, R.layout.item_simple, new String[]{ATTRIBUTE_NAME}, new int[]{R.id.text});
            sdf = new SimpleDateFormat(getString(R.string.dateAndTimeSdf), Locale.getDefault());
        }

        @Override
        public void setViewText(TextView view, String text) {
            try {
                view.setText(sdf.format(Data.BACKUP_SDF.parse(text)));
            } catch (ParseException e) {
                super.setViewText(view, text);
            }
        }
    }
}