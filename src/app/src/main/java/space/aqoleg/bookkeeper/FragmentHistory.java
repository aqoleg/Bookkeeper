// List with history in ActivityMain
package space.aqoleg.bookkeeper;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class FragmentHistory extends Fragment implements AdapterView.OnItemClickListener {
    static final String TAG = "FH";
    private static final int ALL_ASSETS = -1; // assetId for all assets
    private static final String ARG_ASSET_ID = "AI";
    private static final String KEY_SELECTED_ID = "KI";
    private Data data;
    private Loader loader;
    private Adapter adapter;
    private int assetId;
    private int selectedId;

    static FragmentHistory newInstance() {
        return FragmentHistory.newInstance(ALL_ASSETS);
    }

    static FragmentHistory newInstance(int assetId) {
        Bundle args = new Bundle();
        args.putInt(ARG_ASSET_ID, assetId);
        FragmentHistory fragment = new FragmentHistory();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        data = (Data) getActivity().getApplication();
        if (savedInstanceState != null) {
            selectedId = savedInstanceState.getInt(KEY_SELECTED_ID);
        }
        assetId = getArguments().getInt(ARG_ASSET_ID);
        if (assetId == ALL_ASSETS) {
            getActivity().getActionBar().setTitle(getString(R.string.history));
            adapter = new AdapterAll(getActivity());
        } else {
            Cursor cursor = data.getAssetCursor(assetId);
            cursor.moveToFirst();
            getActivity().getActionBar().setTitle(Data.Assets.getName(cursor));
            adapter = new AdapterOne(getActivity(), Data.Assets.getCurrencyId(cursor));
            cursor.close();
        }
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        ListView list = view.findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        loader = new Loader();
        loader.execute();
        return view;
    }

    @Override
    public void onDestroy() {
        loader.cancel(true);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SELECTED_ID, selectedId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectedId = (int) id;
        DialogHistory.newInstance().show(getFragmentManager(), DialogHistory.TAG);
    }

    void reload() {
        loader.cancel(true);
        loader = new Loader();
        loader.execute();
    }

    int getSelectedId() {
        return selectedId;
    }

    private class Loader extends AsyncTask<Void, Void, Cursor> {
        @Override
        protected Cursor doInBackground(Void... voids) {
            data.clearOldHistory(); // clear history before open
            if (assetId == ALL_ASSETS) {
                return data.getHistoryCursor();
            } else {
                return data.getHistoryCursorForAsset(assetId);
            }
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            adapter.changeCursor(cursor);
        }
    }

    private abstract class Adapter extends CursorAdapter {
        private final LayoutInflater inflater;
        private final SimpleDateFormat sdf;

        Adapter(Context context) {
            super(context, null, 0);
            inflater = getActivity().getLayoutInflater();
            sdf = new SimpleDateFormat(context.getString(R.string.dateAndTimeSdf), Locale.getDefault());
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Display description, if has, or time
            String name = Data.History.getDescription(cursor);
            if (name.isEmpty()) {
                name = sdf.format(Data.History.getTime(cursor));
            }
            ((TextView) view.findViewById(R.id.text)).setText(name);
            // Display delta and result for main currency
            StringBuilder builder = new StringBuilder();
            String delta = Data.History.getMainValueDelta(cursor);
            if (!delta.equals("0")) {
                if (delta.startsWith("-")) {
                    builder.append("- ").append(delta).deleteCharAt(2);
                } else {
                    builder.append("+ ").append(delta);
                }
                builder.append(" = ");
            }
            builder.append(Data.History.getMainValueResult(cursor));
            ((TextView) view.findViewById(R.id.mainValue)).setText(builder.toString());
        }
    }

    private class AdapterOne extends Adapter {
        private final int currencyId;

        AdapterOne(Context context, int currencyId) {
            super(context);
            this.currencyId = currencyId;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return super.inflater.inflate(R.layout.item_history_one, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            // Display delta and result for local currency, if has
            if (currencyId != Data.Currencies.ID_MAIN) {
                StringBuilder builder = new StringBuilder();
                String delta = Data.History.getLocalValueDelta(cursor);
                if (delta.startsWith("-")) {
                    builder.append("- ").append(delta).deleteCharAt(2);
                } else {
                    builder.append("+ ").append(delta);
                }
                builder.append(" = ");
                builder.append(Data.History.getLocalValueResult(cursor));
                builder.append(" ").append(Data.getCurrencyName(currencyId));
                ((TextView) view.findViewById(R.id.localValue)).setText(builder.toString());
            }
        }
    }

    private class AdapterAll extends Adapter {
        AdapterAll(Context context) {
            super(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return super.inflater.inflate(R.layout.item_history_all, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            // Display asset name
            Cursor assetCursor = data.getAssetCursor(Data.History.getAssetId(cursor));
            assetCursor.moveToFirst();
            ((TextView) view.findViewById(R.id.asset)).setText(Data.Assets.getName(assetCursor));
            assetCursor.close();
        }
    }
}