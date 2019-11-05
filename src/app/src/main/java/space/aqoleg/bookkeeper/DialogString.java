// Dialog for rename something
package space.aqoleg.bookkeeper;

import android.app.DialogFragment;
import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class DialogString extends DialogFragment implements View.OnClickListener, TextView.OnEditorActionListener {
    static final int TYPE_ADD_CURRENCY = 1;
    static final int TYPE_RENAME_CURRENCY = 2;
    static final int TYPE_ADD_ASSET = 3;
    static final int TYPE_RENAME_ASSET = 4;
    static final int TYPE_ADD_DESCRIPTION = 5;
    static final int TYPE_CHANGE_DESCRIPTION = 6;
    private static final String ARG_TYPE = "AT";
    private Data data;
    private EditText editText;
    private String defaultValue;
    private int type;

    static DialogString newInstance(int type) {
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        DialogString dialog = new DialogString();
        dialog.setArguments(args);
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        data = (Data) getActivity().getApplication();
        View view = inflater.inflate(R.layout.dialog_string, container, false);
        view.findViewById(R.id.clear).setOnClickListener(this);
        view.findViewById(R.id.save).setOnClickListener(this);
        editText = view.findViewById(R.id.editText);
        editText.setOnEditorActionListener(this);
        String title = "";
        type = getArguments().getInt(ARG_TYPE);
        switch (type) {
            case TYPE_ADD_CURRENCY:
            case TYPE_ADD_ASSET:
                title = getString(R.string.enterName);
                defaultValue = "";
                break;
            case TYPE_ADD_DESCRIPTION:
                title = getString(R.string.enterDescription);
                defaultValue = "";
                break;
            case TYPE_RENAME_CURRENCY:
                title = getString(R.string.rename);
                Cursor currencyCursor = data
                        .getCurrencyCursor(((FragmentCurrencies) getFragmentManager()
                                .findFragmentByTag(FragmentCurrencies.TAG))
                                .getSelectedId());
                currencyCursor.moveToFirst();
                defaultValue = Data.Currencies.getName(currencyCursor);
                currencyCursor.close();
                editText.setHint(defaultValue);
                break;
            case TYPE_RENAME_ASSET:
                title = getString(R.string.rename);
                Cursor assetCursor = data
                        .getAssetCursor(((FragmentAssets) getFragmentManager()
                                .findFragmentByTag(FragmentAssets.TAG))
                                .getSelectedId());
                assetCursor.moveToFirst();
                defaultValue = Data.Assets.getName(assetCursor);
                assetCursor.close();
                editText.setHint(defaultValue);
                break;
            case TYPE_CHANGE_DESCRIPTION:
                title = getString(R.string.rename);
                Cursor historyCursor = data
                        .getHistoryCursor(((FragmentHistory) getFragmentManager()
                                .findFragmentByTag(FragmentHistory.TAG))
                                .getSelectedId());
                historyCursor.moveToFirst();
                defaultValue = Data.History.getDescription(historyCursor);
                historyCursor.close();
                editText.setHint(defaultValue);
                break;
        }
        ((TextView) view.findViewById(R.id.title)).setText(title);
        editText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().bringToFront(); // fix
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.clear:
                editText.setText("");
                break;
            case R.id.save:
                save();
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            save();
            return true;
        }
        return false;
    }

    private void save() {
        String input = editText.getText().toString();
        // Can save empty string only when add description
        if (type != TYPE_ADD_DESCRIPTION && (input.isEmpty() || input.equals(defaultValue))) {
            return;
        }
        switch (type) {
            case TYPE_ADD_CURRENCY:
                if (data.addCurrency(input)) {
                    ((FragmentCurrencies) getFragmentManager()
                            .findFragmentByTag(FragmentCurrencies.TAG))
                            .load();
                    dismiss();
                }
                break;
            case TYPE_RENAME_CURRENCY:
                FragmentCurrencies fragmentCurrencies = (FragmentCurrencies) getFragmentManager()
                        .findFragmentByTag(FragmentCurrencies.TAG);
                if (data.renameCurrency(fragmentCurrencies.getSelectedId(), input)) {
                    fragmentCurrencies.load();
                    dismiss();
                }
                break;
            case TYPE_ADD_ASSET:
                Fragment fragment = getFragmentManager().findFragmentByTag(DialogCurrency.TAG);
                // Fragment does not exist if only one currency
                if (fragment == null) {
                    if (data.addAsset(input, Data.Currencies.ID_MAIN)) {
                        ((FragmentAssets) getFragmentManager()
                                .findFragmentByTag(FragmentAssets.TAG))
                                .load(false);
                        dismiss();
                    }
                } else {
                    DialogCurrency dialogCurrency = (DialogCurrency) fragment;
                    if (data.addAsset(input, dialogCurrency.getCurrency())) {
                        ((FragmentAssets) getFragmentManager()
                                .findFragmentByTag(FragmentAssets.TAG))
                                .load(false);
                        dialogCurrency.dismiss();
                        dismiss();
                    }
                }
                break;
            case TYPE_RENAME_ASSET:
                FragmentAssets fragmentAssets = (FragmentAssets) getFragmentManager()
                        .findFragmentByTag(FragmentAssets.TAG);
                if (data.renameAsset(fragmentAssets.getSelectedId(), input)) {
                    fragmentAssets.load(false);
                    dismiss();
                }
                break;
            case TYPE_ADD_DESCRIPTION:
                FragmentAssets fragmentAssets1 = (FragmentAssets) getFragmentManager()
                        .findFragmentByTag(FragmentAssets.TAG);
                DialogTransaction dialog = (DialogTransaction) getFragmentManager()
                        .findFragmentByTag(DialogTransaction.TAG);
                if (data.makeHistory(
                        fragmentAssets1.getSelectedId(),
                        dialog.getMainValueDelta(),
                        dialog.getMainValueResult(),
                        dialog.getLocalValueDelta(),
                        dialog.getLocalValueResult(),
                        input
                )) {
                    fragmentAssets1.load(true);
                    dialog.dismiss();
                    dismiss();
                }
                break;
            case TYPE_CHANGE_DESCRIPTION:
                FragmentHistory fragmentHistory = (FragmentHistory) getFragmentManager()
                        .findFragmentByTag(FragmentHistory.TAG);
                if (data.changeHistoryDescription(fragmentHistory.getSelectedId(), input)) {
                    fragmentHistory.reload();
                    ((DialogHistory) getFragmentManager()
                            .findFragmentByTag(DialogHistory.TAG))
                            .dismiss();
                    dismiss();
                }
                break;
        }
    }
}