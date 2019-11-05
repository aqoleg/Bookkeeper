// List with assets in ActivityMain
package space.aqoleg.bookkeeper;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class FragmentAssets extends Fragment implements AdapterView.OnItemClickListener {
    static final String TAG = "FA";
    private static final String KEY_SELECTED_ID = "KI";
    private static final String KEY_STATE_MOVE = "KM";
    private final Listener listener = new Listener(); // listener for menu buttons
    private Data data;
    private View footer;
    private Adapter adapter;
    private int selectedId;
    private boolean stateMove; // true when moving item

    static FragmentAssets newInstance() {
        return new FragmentAssets();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        data = (Data) getActivity().getApplication();
        if (savedInstanceState != null) {
            selectedId = savedInstanceState.getInt(KEY_SELECTED_ID);
            stateMove = savedInstanceState.getBoolean(KEY_STATE_MOVE);
        }
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        ListView list = view.findViewById(R.id.list);
        footer = getActivity().getLayoutInflater().inflate(R.layout.footer_assets, list, false);
        footer.findViewById(R.id.add).setTag(-1); // footer id
        footer.findViewById(R.id.add).setOnClickListener(listener);
        adapter = new Adapter(getActivity());
        list.addFooterView(footer);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        setStateAndTitle(stateMove);
        load(true);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SELECTED_ID, selectedId);
        outState.putBoolean(KEY_STATE_MOVE, stateMove);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (stateMove) {
            // If footer or the same item - deselect, else move and reload
            if (id == -1 || id == selectedId) {
                setStateAndTitle(false);
                adapter.notifyDataSetChanged();
            } else if (data.move(selectedId, (int) id)) {
                setStateAndTitle(false);
                load(false);
            }
        } else if (id != -1) {
            // Start transaction
            selectedId = (int) id;
            DialogTransaction.newInstance().show(getFragmentManager(), DialogTransaction.TAG);
        }
    }

    boolean onBackPressed() {
        if (stateMove) {
            // Deselect
            setStateAndTitle(false);
            adapter.notifyDataSetChanged();
            return true;
        }
        return false; // close app
    }

    void load(boolean withFooter) {
        if (withFooter) {
            ((TextView) footer.findViewById(R.id.value)).setText(data.getTotalValue());
        }
        adapter.load();
    }

    int getSelectedId() {
        return selectedId;
    }

    private void setStateAndTitle(boolean state) {
        stateMove = state;
        String title = stateMove ? getString(R.string.move) : Data.getCurrencyName(Data.Currencies.ID_MAIN);
        getActivity().getActionBar().setTitle(title);
    }

    private class Listener implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
        @Override
        public void onClick(View view) {
            if (stateMove) {
                // Deselect
                setStateAndTitle(false);
                adapter.notifyDataSetChanged();
            } else {
                // If footer - add asset, else open menu
                selectedId = (int) view.getTag();
                if (selectedId == -1) {
                    if (data.hasCurrencies()) {
                        DialogCurrency.newInstance().show(getFragmentManager(), DialogCurrency.TAG);
                    } else {
                        // Main currency, do not select
                        DialogString.newInstance(DialogString.TYPE_ADD_ASSET).show(getFragmentManager(), null);
                    }
                } else {
                    PopupMenu menu = new PopupMenu(getActivity(), view);
                    menu.getMenuInflater().inflate(R.menu.asset, menu.getMenu());
                    menu.setOnMenuItemClickListener(this);
                    menu.show();
                }
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.history:
                    ((ActivityMain) getActivity()).viewHistory(selectedId);
                    break;
                case R.id.move:
                    setStateAndTitle(true);
                    adapter.notifyDataSetChanged();
                    break;
                case R.id.rename:
                    DialogString.newInstance(DialogString.TYPE_RENAME_ASSET).show(getFragmentManager(), null);
                    break;
                case R.id.delete:
                    if (!data.assetIsZero(selectedId)) {
                        Toast.makeText(getActivity(), getString(R.string.assetHaveValues), Toast.LENGTH_LONG).show();
                    } else if (data.assetHasHistory(selectedId)) {
                        Toast.makeText(getActivity(), getString(R.string.assetHaveHistory), Toast.LENGTH_LONG).show();
                    } else {
                        DialogDelete.newInstance(DialogDelete.TYPE_ASSET).show(getFragmentManager(), null);
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
            return inflater.inflate(R.layout.item_assets, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            int id = Data.Assets.getId(cursor);
            ((TextView) view.findViewById(R.id.text)).setText(Data.Assets.getName(cursor));
            int currency = Data.Assets.getCurrencyId(cursor);
            // Set local value, if has, or empty string
            String localValue = "";
            if (currency != Data.Currencies.ID_MAIN) {
                localValue = Data.Assets.getLocalValue(cursor)
                        .concat(" ")
                        .concat(Data.getCurrencyName(currency));
            }
            ((TextView) view.findViewById(R.id.localValue)).setText(localValue);
            ((TextView) view.findViewById(R.id.mainValue)).setText(Data.Assets.getMainValue(cursor));
            // Select
            if (stateMove && selectedId == id) {
                view.setBackgroundColor(Color.GRAY);
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }
            // Set menu tag to item id
            view.findViewById(R.id.menu).setTag(id);
            view.findViewById(R.id.menu).setOnClickListener(listener);
        }

        private void load() {
            adapter.changeCursor(data.getAssetsCursor());
        }
    }
}