// Dialog for making new transaction
package space.aqoleg.bookkeeper;

import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class DialogTransaction extends DialogFragment implements View.OnClickListener, TextView.OnEditorActionListener {
    static final String TAG = "DT";
    private static final String KEY_EDIT_RESULT = "KER";
    private static final String KEY_DELTA = "KD";
    private static final String KEY_RESULT = "KR";
    private static final String KEY_DELTA_MAIN = "KDM";
    private static final String KEY_RESULT_MAIN = "KRM";
    private boolean editResult; // if true edit result, else edit delta
    private String delta;
    private String result;
    private String deltaMain = null; // null for main currency, else not null
    private String resultMain = null; // null for main currency, else not null
    private Calculator calculator;
    private EditText editText;

    static DialogTransaction newInstance() {
        DialogTransaction dialog = new DialogTransaction();
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_transaction, container, false);

        view.findViewById(R.id.delta).setOnClickListener(this);
        view.findViewById(R.id.result).setOnClickListener(this);
        view.findViewById(R.id.clear).setOnClickListener(this);
        view.findViewById(R.id.negate).setOnClickListener(this);
        view.findViewById(R.id.next).setOnClickListener(this);
        editText = view.findViewById(R.id.editValue);
        editText.setOnEditorActionListener(this);

        int assetId = ((FragmentAssets) getFragmentManager()
                .findFragmentByTag(FragmentAssets.TAG))
                .getSelectedId();
        Cursor assetCursor = ((Data) getActivity().getApplication()).getAssetCursor(assetId);
        assetCursor.moveToFirst();
        ((TextView) view.findViewById(R.id.title)).setText(Data.Assets.getName(assetCursor));
        int currencyId = Data.Assets.getCurrencyId(assetCursor);
        String start;
        String startMain = null;
        String course = null;
        if (currencyId == Data.Currencies.ID_MAIN) {
            start = Data.Assets.getMainValue(assetCursor);
            view.findViewById(R.id.mainCurrencyLayout).setVisibility(View.GONE);
        } else {
            Cursor currencyCursor = ((Data) getActivity().getApplication()).getCurrencyCursor(currencyId);
            currencyCursor.moveToFirst();
            course = Data.Currencies.getCourse(currencyCursor);
            currencyCursor.close();
            start = Data.Assets.getLocalValue(assetCursor);
            startMain = Data.Assets.getMainValue(assetCursor);
            ((TextView) view.findViewById(R.id.startMain))
                    .setText(startMain.concat(" ").concat(Data.getCurrencyName(Data.Currencies.ID_MAIN)));
        }
        ((TextView) view.findViewById(R.id.start))
                .setText(start.concat(" ").concat(Data.getCurrencyName(currencyId)));
        assetCursor.close();

        calculator = Calculator.get(start, startMain, course);
        if (savedInstanceState != null) {
            editResult = savedInstanceState.getBoolean(KEY_EDIT_RESULT);
            delta = savedInstanceState.getString(KEY_DELTA);
            result = savedInstanceState.getString(KEY_RESULT);
            deltaMain = savedInstanceState.getString(KEY_DELTA_MAIN);
            resultMain = savedInstanceState.getString(KEY_RESULT_MAIN);
        } else {
            delta = "0";
            result = start;
            if (currencyId != Data.Currencies.ID_MAIN) {
                deltaMain = "0";
                resultMain = startMain;
            }
        }
        view.findViewById(R.id.delta).setBackgroundColor(editResult ? Color.TRANSPARENT : Color.GRAY);
        view.findViewById(R.id.result).setBackgroundColor(editResult ? Color.GRAY : Color.TRANSPARENT);
        display(view);
        editText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_EDIT_RESULT, editResult);
        outState.putString(KEY_DELTA, delta);
        outState.putString(KEY_RESULT, result);
        outState.putString(KEY_DELTA_MAIN, deltaMain);
        outState.putString(KEY_RESULT_MAIN, resultMain);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.delta:
                editResult = false;
                view.setBackgroundColor(Color.GRAY);
                getView().findViewById(R.id.result).setBackgroundColor(Color.TRANSPARENT);
                editText.setText(delta.startsWith("-") ? delta.substring(1) : delta);
                break;
            case R.id.result:
                editResult = true;
                getView().findViewById(R.id.delta).setBackgroundColor(Color.TRANSPARENT);
                view.setBackgroundColor(Color.GRAY);
                editText.setText(result.startsWith("-") ? result.substring(1) : result);
                break;
            case R.id.clear:
                editText.setText("");
                break;
            case R.id.negate:
                negate();
                break;
            case R.id.next:
                goNext();
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_NEXT) {
            goNext();
            return true;
        }
        return false;
    }

    String getMainValueDelta() {
        return deltaMain == null ? delta : deltaMain;
    }

    String getMainValueResult() {
        return resultMain == null ? result : resultMain;
    }

    String getLocalValueDelta() {
        return deltaMain == null ? null : delta;
    }

    String getLocalValueResult() {
        return resultMain == null ? null : result;
    }

    private void display(View view) {
        StringBuilder builder = new StringBuilder();
        if (delta.equals("0")) {
            builder.append(delta);
        } else if (delta.startsWith("-")) {
            builder.append("- ").append(delta).deleteCharAt(2);
        } else {
            builder.append("+ ").append(delta);
        }
        ((TextView) view.findViewById(R.id.delta)).setText(builder.toString());
        ((TextView) view.findViewById(R.id.result)).setText("= ".concat(result));

        if (deltaMain != null) {
            builder = new StringBuilder();
            if (!deltaMain.equals("0")) {
                if (deltaMain.startsWith("-")) {
                    builder.append("- ").append(deltaMain).deleteCharAt(2);
                } else {
                    builder.append("+ ").append(deltaMain);
                }
                builder.append(" = ");
                builder.append(resultMain);
            }
            ((TextView) view.findViewById(R.id.main)).setText(builder.toString());
        }
    }

    private void goNext() {
        String currentValue;
        if (editResult) {
            currentValue = result.startsWith("-") ? result.substring(1) : result;
        } else {
            currentValue = delta.startsWith("-") ? delta.substring(1) : delta;
        }
        String input = editText.getText().toString();

        if (input.isEmpty()) {
            editText.setText(currentValue);
        } else if (!input.equals(currentValue)) {
            input = Calculator.inputTransaction(input);
            editText.setText(input);

            if (editResult) {
                if (result.startsWith("-") && !input.equals("0")) {
                    result = "-".concat(input);
                } else {
                    result = input;
                }
                delta = calculator.getDeltaByResult(result);
            } else {
                if (delta.startsWith("-") && !input.equals("0")) {
                    delta = "-".concat(input);
                } else {
                    delta = input;
                }
                result = calculator.getResultByDelta(delta, true);
            }
            if (resultMain != null) {
                resultMain = calculator.getResultMain(null);
                deltaMain = calculator.getDeltaMain();
            }

            display(getView());
        } else if (!delta.equals("0")) {
            FragmentTransaction transaction = getFragmentManager()
                    .beginTransaction()
                    .addToBackStack(null);
            DialogString.newInstance(DialogString.TYPE_ADD_DESCRIPTION).show(transaction, null);
        }
    }

    private void negate() {
        String currentValue;
        if (editResult) {
            currentValue = result.startsWith("-") ? result.substring(1) : result;
        } else {
            currentValue = delta.startsWith("-") ? delta.substring(1) : delta;
        }
        String input = editText.getText().toString();
        if (input.isEmpty()) {
            input = currentValue;
            editText.setText(input);
        } else if (!input.equals(currentValue)) {
            input = Calculator.inputTransaction(input);
            editText.setText(input);
        }

        if (editResult) {
            if (result.startsWith("-") || input.equals("0")) {
                result = input;
            } else {
                result = "-".concat(input);
            }
            delta = calculator.getDeltaByResult(result);
        } else {
            if (delta.startsWith("-") || input.equals("0")) {
                delta = input;
            } else {
                delta = "-".concat(input);
            }
            result = calculator.getResultByDelta(delta, true);
        }
        if (resultMain != null) {
            resultMain = calculator.getResultMain(null);
            deltaMain = calculator.getDeltaMain();
        }

        display(getView());
    }
}