// Dialog for prompt when deleting something
package space.aqoleg.bookkeeper;

import android.app.DialogFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DialogDelete extends DialogFragment implements View.OnClickListener {
    static final int TYPE_CURRENCY = 1;
    static final int TYPE_ASSET = 2;
    static final int TYPE_HISTORY = 3;
    private static final String ARG_TYPE = "AT";
    private Data data;

    static DialogDelete newInstance(int type) {
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        DialogDelete dialog = new DialogDelete();
        dialog.setArguments(args);
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        data = (Data) getActivity().getApplication();
        View view = inflater.inflate(R.layout.dialog_delete, container, false);
        view.findViewById(R.id.cancel).setOnClickListener(this);
        view.findViewById(R.id.ok).setOnClickListener(this);
        String text = getString(R.string.deleteS);
        switch (getArguments().getInt(ARG_TYPE)) {
            case TYPE_CURRENCY:
                int currencyId = ((FragmentCurrencies) getFragmentManager()
                        .findFragmentByTag(FragmentCurrencies.TAG))
                        .getSelectedId();
                text = String.format(text, Data.getCurrencyName(currencyId));
                break;
            case TYPE_ASSET:
                int assetId = ((FragmentAssets) getFragmentManager()
                        .findFragmentByTag(FragmentAssets.TAG))
                        .getSelectedId();
                Cursor assetCursor = data.getAssetCursor(assetId);
                assetCursor.moveToFirst();
                text = String.format(text, Data.Assets.getName(assetCursor));
                assetCursor.close();
                break;
            case TYPE_HISTORY:
                int historyId = ((FragmentHistory) getFragmentManager()
                        .findFragmentByTag(FragmentHistory.TAG))
                        .getSelectedId();
                Cursor historyCursor = data.getHistoryCursor(historyId);
                historyCursor.moveToFirst();
                text = String.format(text, Data.History.getDescription(historyCursor));
                historyCursor.close();
                break;
        }
        ((TextView) view.findViewById(R.id.text)).setText(text);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().bringToFront(); // Fix
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.cancel) {
            dismiss();
            return;
        }
        switch (getArguments().getInt(ARG_TYPE)) {
            case TYPE_CURRENCY:
                FragmentCurrencies fragmentCurrencies = (FragmentCurrencies) getFragmentManager()
                        .findFragmentByTag(FragmentCurrencies.TAG);
                if (data.deleteCurrency(fragmentCurrencies.getSelectedId())) {
                    fragmentCurrencies.load();
                    dismiss();
                }
                break;
            case TYPE_ASSET:
                FragmentAssets fragmentAssets = (FragmentAssets) getFragmentManager()
                        .findFragmentByTag(FragmentAssets.TAG);
                if (data.deleteAsset(fragmentAssets.getSelectedId())) {
                    fragmentAssets.load(false);
                    dismiss();
                }
                break;
            case TYPE_HISTORY:
                FragmentHistory fragmentHistory = (FragmentHistory) getFragmentManager()
                        .findFragmentByTag(FragmentHistory.TAG);
                if (data.removeHistoryItem(fragmentHistory.getSelectedId())) {
                    fragmentHistory.reload();
                    ((DialogFragment) getFragmentManager().findFragmentByTag(DialogHistory.TAG)).dismiss();
                    dismiss();
                }
                break;
        }
    }
}