// Dialog for change course
package space.aqoleg.bookkeeper;

import android.app.DialogFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class DialogCourse extends DialogFragment implements View.OnClickListener, TextView.OnEditorActionListener {
    private Data data;
    private EditText editText;
    private String currentCourse;

    static DialogCourse newInstance() {
        DialogCourse dialog = new DialogCourse();
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        data = (Data) getActivity().getApplication();
        View view = inflater.inflate(R.layout.dialog_course, container, false);
        view.findViewById(R.id.clear).setOnClickListener(this);
        view.findViewById(R.id.invert).setOnClickListener(this);
        view.findViewById(R.id.save).setOnClickListener(this);
        editText = view.findViewById(R.id.editText);
        editText.setOnEditorActionListener(this);
        int currencyId = ((FragmentCurrencies) getFragmentManager()
                .findFragmentByTag(FragmentCurrencies.TAG))
                .getSelectedId();
        Cursor cursor = data.getCurrencyCursor(currencyId);
        cursor.moveToFirst();
        currentCourse = Data.Currencies.getCourse(cursor);
        cursor.close();
        String hint = Data.getCurrencyName(Data.Currencies.ID_MAIN)
                .concat("/")
                .concat(Data.getCurrencyName(currencyId));
        editText.setHint(hint);
        editText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (editText.getText().toString().isEmpty()) {
            editText.setText(currentCourse);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.clear:
                editText.setText("");
                break;
            case R.id.invert:
                String input = editText.getText().toString();
                if (input.isEmpty()) {
                    editText.setText("0");
                    return;
                }
                editText.setText(Calculator.invert(input));
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
        if (input.isEmpty()) {
            editText.setText("1");
        } else {
            String correctInput = Calculator.inputCourse(input);
            if (!input.equals(correctInput)) {
                editText.setText(correctInput);
            } else if (!input.equals(currentCourse)) {
                // Update course, display delta
                FragmentCurrencies fragment = (FragmentCurrencies) getFragmentManager()
                        .findFragmentByTag(FragmentCurrencies.TAG);
                Calculator calculator = Calculator.get(data.getTotalValue(), null, null);
                if (data.updateCourse(fragment.getSelectedId(), input)) {
                    String delta = calculator.getDeltaByResult(data.getTotalValue());
                    if (!delta.equals("0")) {
                        StringBuilder builder = new StringBuilder(getString(R.string.totalChange));
                        if (delta.startsWith("-")) {
                            int pos = builder.length() + 3;
                            builder.append(" - ").append(delta).deleteCharAt(pos);
                        } else {
                            builder.append(" + ").append(delta);
                        }
                        Toast.makeText(getActivity(), builder.toString(), Toast.LENGTH_LONG).show();
                    }
                    fragment.load();
                    dismiss();
                }
            }
        }
    }
}