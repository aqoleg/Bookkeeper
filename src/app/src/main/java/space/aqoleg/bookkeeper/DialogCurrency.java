// Dialog for select currency while creating new asset
package space.aqoleg.bookkeeper;

import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DialogCurrency extends DialogFragment implements AdapterView.OnItemClickListener {
    static final String TAG = "DC";
    private static final String KEY_CURRENCY = "KC";
    private int currency;

    static DialogCurrency newInstance() {
        DialogCurrency dialog = new DialogCurrency();
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_currency, container, false);
        ListView list = view.findViewById(R.id.list);
        list.setAdapter(new Adapter(getActivity()));
        list.setOnItemClickListener(this);
        if (savedInstanceState != null) {
            currency = savedInstanceState.getInt(KEY_CURRENCY);
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_CURRENCY, currency);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        currency = (int) id;
        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction()
                .addToBackStack(null);
        DialogString.newInstance(DialogString.TYPE_ADD_ASSET).show(transaction, null);
    }

    int getCurrency() {
        return currency;
    }

    private class Adapter extends CursorAdapter {
        Adapter(Context context) {
            super(context, ((Data) getActivity().getApplication()).getCurrenciesCursor(), 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater().inflate(R.layout.item_simple, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.text)).setText(Data.Currencies.getName(cursor));
        }
    }
}