// Single activity
package space.aqoleg.bookkeeper;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Field;

public class ActivityMain extends Activity {
    private static final int REQUEST_BACKUP = 0;
    private static final int REQUEST_RESTORE = 1;
    private static final int STATE_CURRENCIES = 1;
    private static final int STATE_ASSETS = 2;
    private static final int STATE_HISTORY = 3;
    private static final String KEY_STATE = "KS";
    private int state; // which fragment in use

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        // Set menu dots in action bar always visible
        try {
            String field = "sHasPermanentMenuKey";
            Field menuField = ViewConfiguration.class.getDeclaredField(field);
            if (menuField != null) {
                menuField.setAccessible(true);
                menuField.setBoolean(ViewConfiguration.get(this), false);
            }
        } catch (Exception ignored) {
        }
        // Load current state, or STATE_ASSET
        if (savedInstanceState == null) {
            state = STATE_ASSETS;
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.frame, FragmentAssets.newInstance(), FragmentAssets.TAG)
                    .commit();
        } else {
            state = savedInstanceState.getInt(KEY_STATE);
        }
    }

    @Override
    protected void onDestroy() {
        ((Data) getApplication()).close();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_STATE, state);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (state == STATE_ASSETS) {
            if (!((FragmentAssets) getFragmentManager().findFragmentByTag(FragmentAssets.TAG)).onBackPressed()) {
                super.onBackPressed();
            }
        } else {
            state = STATE_ASSETS;
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame, FragmentAssets.newInstance(), FragmentAssets.TAG)
                    .commit();
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getActionBar().setDisplayHomeAsUpEnabled(state != STATE_ASSETS);
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.currencies).setVisible(state != STATE_CURRENCIES);
        menu.findItem(R.id.history).setVisible(state != STATE_HISTORY);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.currencies:
                state = STATE_CURRENCIES;
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame, FragmentCurrencies.newInstance(), FragmentCurrencies.TAG)
                        .commit();
                invalidateOptionsMenu();
                break;
            case R.id.history:
                state = STATE_HISTORY;
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame, FragmentHistory.newInstance(), FragmentHistory.TAG)
                        .commit();
                invalidateOptionsMenu();
                break;
            case R.id.backup:
                backup(REQUEST_BACKUP);
                break;
            case R.id.restore:
                backup(REQUEST_RESTORE);
                break;
            case R.id.source:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aqoleg/Bookkeeper"));
                if (intent.resolveActivity(getPackageManager()) == null) {
                    Toast.makeText(this, "github.com/aqoleg/Bookkeeper", Toast.LENGTH_LONG).show();
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    backup(requestCode);
                    return;
                }
            }
        }
        Toast.makeText(this, getString(R.string.canNotBackupRestore), Toast.LENGTH_SHORT).show();
    }

    void viewHistory(int assetId) {
        state = STATE_HISTORY;
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.frame, FragmentHistory.newInstance(assetId), FragmentHistory.TAG)
                .commit();
        invalidateOptionsMenu();
    }

    boolean restore(String fileName) {
        try {
            ((Data) getApplication()).restore(fileName);
        } catch (IOException exception) {
            String message = getString(R.string.unsuccessful) + exception.getMessage();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return false;
        }
        // Reload after successful restore
        state = STATE_ASSETS;
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.frame, FragmentAssets.newInstance(), FragmentAssets.TAG)
                .commit();
        invalidateOptionsMenu();
        return true;
    }

    private void backup(int request) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            int read = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            int write = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (read == PackageManager.PERMISSION_DENIED || write == PackageManager.PERMISSION_DENIED) {
                requestPermissions(
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        request
                );
                return;
            }
        }
        switch (request) {
            case REQUEST_BACKUP:
                String message;
                try {
                    message = getString(R.string.successfulBackupTo) + ((Data) getApplication()).backup();
                } catch (IOException exception) {
                    message = getString(R.string.unsuccessful) + exception.getMessage();
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                break;
            case REQUEST_RESTORE:
                DialogRestore.newInstance().show(getFragmentManager(), null);
                break;
        }
    }
}