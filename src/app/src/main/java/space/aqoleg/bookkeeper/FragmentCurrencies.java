// List with currencies in ActivityMain
package space.aqoleg.bookkeeper;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class FragmentCurrencies extends Fragment implements AdapterView.OnItemClickListener {
    static final String TAG = "FC";
    private static final String KEY_SELECTED_ID = "KI";
    private final Listener listener = new Listener(); // listener for menu buttons
    private Data data;
    private Adapter adapter;
    private int selectedId;

    static FragmentCurrencies newInstance() {
        return new FragmentCurrencies();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        data = (Data) getActivity().getApplication();
        if (savedInstanceState != null) {
            selectedId = savedInstanceState.getInt(KEY_SELECTED_ID);
        }
        getActivity().getActionBar().setTitle(getString(R.string.currencies));
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        ListView list = view.findViewById(R.id.list);
        View footer = getActivity().getLayoutInflater().inflate(R.layout.footer_currencies, list, false);
        footer.findViewById(R.id.add).setTag(-1); // footer id
        footer.findViewById(R.id.add).setOnClickListener(listener);
        adapter = new Adapter(getActivity());
        list.addFooterView(footer);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        load();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SELECTED_ID, selectedId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // If main currency - rename, else - change course
        selectedId = (int) id;
        if (selectedId == Data.Currencies.ID_MAIN) {
            DialogString.newInstance(DialogString.TYPE_RENAME_CURRENCY).show(getFragmentManager(), null);
        } else if (selectedId != -1) {
            DialogCourse.newInstance().show(getFragmentManager(), null);
        }
    }

    void load() {
        adapter.load();
    }

    int getSelectedId() {
        return selectedId;
    }

    private class Listener implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
        @Override
        public void onClick(View view) {
            // If footer - add currency, else if main currency - rename, else open menu
            selectedId = (int) view.getTag();
            if (selectedId == -1) {
                DialogString.newInstance(DialogString.TYPE_ADD_CURRENCY).show(getFragmentManager(), null);
            } else if (selectedId == Data.Currencies.ID_MAIN) {
                DialogString.newInstance(DialogString.TYPE_RENAME_CURRENCY).show(getFragmentManager(), null);
            } else {
                PopupMenu menu = new PopupMenu(getActivity(), view);
                menu.getMenuInflater().inflate(R.menu.currency, menu.getMenu());
                menu.setOnMenuItemClickListener(this);
                menu.show();
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.rename:
                    DialogString.newInstance(DialogString.TYPE_RENAME_CURRENCY).show(getFragmentManager(), null);
                    break;
                case R.id.delete:
                    if (data.currencyHasAssets(selectedId)) {
                        Toast.makeText(getActivity(), getString(R.string.currencyHasAssets), Toast.LENGTH_LONG).show();
                    } else {
                        DialogDelete.newInstance(DialogDelete.TYPE_CURRENCY).show(getFragmentManager(), null);
                    }
                    break;
            }
            return true;
        }
    }

    private class Adapter extends CursorAdapter {
        private final LayoutInflater inflater;

        Adapter(Context context) {
            super(context, null, 0);
            inflater = getActivity().getLayoutInflater();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(R.layout.item_currencies, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            int id = Data.Currencies.getId(cursor);
            ((TextView) view.findViewById(R.id.text)).setText(Data.Currencies.getName(cursor));
            String course = ""; // empty course for main currency
            if (id != Data.Currencies.ID_MAIN) {
                course = Data.Currencies.getCourse(cursor);
            }
            ((TextView) view.findViewById(R.id.value)).setText(course);
            // Set menu tag to item id
            view.findViewById(R.id.menu).setTag(id);
            view.findViewById(R.id.menu).setOnClickListener(listener);
        }

        void load() {
            changeCursor(data.getCurrenciesCursor());
        }
    }
}