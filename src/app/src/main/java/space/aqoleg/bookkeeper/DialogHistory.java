// Dialog with history item
package space.aqoleg.bookkeeper;

import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DialogHistory extends DialogFragment implements View.OnClickListener {
    public static final String TAG = "DH";
    private Data data;

    static DialogHistory newInstance() {
        DialogHistory dialog = new DialogHistory();
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        data = (Data) getActivity().getApplication();
        View view = inflater.inflate(R.layout.dialog_history, container, false);
        view.findViewById(R.id.delete).setOnClickListener(this);
        view.findViewById(R.id.rename).setOnClickListener(this);
        Cursor cursor = data.getHistoryCursor(((FragmentHistory) getFragmentManager()
                .findFragmentByTag(FragmentHistory.TAG))
                .getSelectedId());
        cursor.moveToFirst();
        int assetId = Data.History.getAssetId(cursor);

        Cursor assetCursor = data.getAssetCursor(assetId);
        assetCursor.moveToFirst();
        ((TextView) view.findViewById(R.id.title)).setText(Data.Assets.getName(assetCursor));
        int currencyId = Data.Assets.getCurrencyId(assetCursor);
        assetCursor.close();
        // Add description and time
        StringBuilder builder = new StringBuilder();
        String name = Data.History.getDescription(cursor);
        if (!name.isEmpty()) {
            builder.append(name);
            builder.append("\n");
        }
        name = new SimpleDateFormat(getString(R.string.dateAndTimeSecSdf), Locale.getDefault())
                .format(Data.History.getTime(cursor));
        builder.append(name);
        builder.append("\n");
        // Add delta and result for main currency
        String delta = Data.History.getMainValueDelta(cursor);
        if (!delta.equals("0")) {
            if (delta.startsWith("-")) {
                int pos = builder.length() + 2;
                builder.append("- ").append(delta).deleteCharAt(pos);
            } else {
                builder.append("+ ").append(delta);
            }
            builder.append(" = ");
        }
        builder.append(Data.History.getMainValueResult(cursor));
        builder.append(" ").append(Data.getCurrencyName(Data.Currencies.ID_MAIN));
        // Add delta and result for local currency, if has
        if (currencyId != Data.Currencies.ID_MAIN) {
            builder.append("\n");
            delta = Data.History.getLocalValueDelta(cursor);
            if (delta.startsWith("-")) {
                int pos = builder.length() + 2;
                builder.append("- ").append(delta).deleteCharAt(pos);
            } else {
                builder.append("+ ").append(delta);
            }
            builder.append(" = ");
            builder.append(Data.History.getLocalValueResult(cursor));
            builder.append(" ").append(Data.getCurrencyName(currencyId));
        }
        cursor.close();
        ((TextView) view.findViewById(R.id.text)).setText(builder.toString());
        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.delete:
                FragmentHistory fragmentHistory = ((FragmentHistory) getFragmentManager()
                        .findFragmentByTag(FragmentHistory.TAG));
                if (data.hasLaterHistory(fragmentHistory.getSelectedId())) {
                    Toast.makeText(getActivity(), getString(R.string.hasHistory), Toast.LENGTH_LONG).show();
                } else {
                    FragmentTransaction transaction = getFragmentManager()
                            .beginTransaction()
                            .addToBackStack(null);
                    DialogDelete.newInstance(DialogDelete.TYPE_HISTORY).show(transaction, null);
                }
                break;
            case R.id.rename:
                FragmentTransaction transaction = getFragmentManager()
                        .beginTransaction()
                        .addToBackStack(null);
                DialogString.newInstance(DialogString.TYPE_CHANGE_DESCRIPTION).show(transaction, null);
                break;
        }
    }
}