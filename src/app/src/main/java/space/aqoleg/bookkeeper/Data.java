// SQLite database
// can use asynchronous thread
package space.aqoleg.bookkeeper;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.provider.BaseColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Data extends Application {
    static final String BACKUP_FOLDER = "backups";
    static final SimpleDateFormat BACKUP_SDF = new SimpleDateFormat("'Bk'-yyyyMMdd-HHmmss.'bk'", Locale.getDefault());
    static final String BACKUP_PREFIX = "Bk"; // all file names started with prefix and ended with suffix acceptable
    static final String BACKUP_SUFFIX = ".bk";
    private static final String DATABASE_NAME = "bk.db";
    private static final long KEEP_HISTORY_TIME = 8640000000L; // 100 days
    private static DataHelper dataHelper; // single instance
    private static String[] currenciesNames; // cached

    static String getCurrencyName(int currencyId) {
        return currenciesNames[currencyId - 1];
    }

    // Open database when open app
    @Override
    public void onCreate() {
        super.onCreate();
        if (dataHelper == null) {
            dataHelper = new DataHelper(this);
            load();
        }
    }

    // Close all database objects
    void close() {
        dataHelper.close();
    }

    // Read

    String getTotalValue() {
        Cursor cursor = getCurrencyCursor(Currencies.ID_MAIN);
        cursor.moveToFirst();
        String totalValue = Currencies.getCourse(cursor);
        cursor.close();
        return totalValue;
    }

    boolean hasCurrencies() {
        return dataHelper
                .getWritableDatabase()
                .compileStatement("SELECT count(*) FROM " + Currencies.TABLE)
                .simpleQueryForLong() > 1;
    }

    boolean currencyHasAssets(int currencyId) {
        return dataHelper
                .getWritableDatabase()
                .compileStatement("SELECT count(*) FROM " + Assets.TABLE
                        + " WHERE " + Assets.CURRENCY + " = " + currencyId)
                .simpleQueryForLong() != 0;
    }

    boolean assetIsZero(int assetId) {
        Cursor cursor = getAssetCursor(assetId);
        cursor.moveToFirst();
        String value;
        if (Assets.getCurrencyId(cursor) == Currencies.ID_MAIN) {
            value = Assets.getMainValue(cursor);
        } else {
            value = Assets.getLocalValue(cursor);
        }
        cursor.close();
        return value.equals("0");
    }

    boolean assetHasHistory(int assetId) {
        return dataHelper
                .getWritableDatabase()
                .compileStatement("SELECT count(*) FROM " + History.TABLE
                        + " WHERE " + History.ASSET + " = " + assetId)
                .simpleQueryForLong() != 0;
    }

    boolean hasLaterHistory(int historyId) {
        Cursor cursor = getHistoryCursor(historyId);
        cursor.moveToFirst();
        int assetId = History.getAssetId(cursor);
        cursor.close();
        return dataHelper
                .getWritableDatabase()
                .compileStatement("SELECT count(*) FROM " + History.TABLE
                        + " WHERE " + History.ID + " > " + historyId
                        + " AND " + History.ASSET + " = " + assetId)
                .simpleQueryForLong() != 0;
    }

    // Return cursors
    // close after using

    Cursor getCurrencyCursor(int currencyId) {
        return dataHelper.getWritableDatabase().query(
                Currencies.TABLE,
                null,
                Currencies.ID + " = " + currencyId,
                null,
                null,
                null,
                null
        );
    }

    Cursor getCurrenciesCursor() {
        return dataHelper.getWritableDatabase().query(
                Currencies.TABLE,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    Cursor getAssetCursor(int assetId) {
        return dataHelper.getWritableDatabase().query(
                Assets.TABLE,
                null,
                Assets.ID + " = " + assetId,
                null,
                null,
                null,
                null
        );
    }

    Cursor getAssetsCursor() {
        return dataHelper.getWritableDatabase().query(
                Assets.TABLE,
                null,
                null,
                null,
                null,
                null,
                Assets.ORDER
        );
    }

    Cursor getHistoryCursor(int historyId) {
        return dataHelper.getWritableDatabase().query(
                History.TABLE,
                null,
                History.ID + " = " + historyId,
                null,
                null,
                null,
                null);
    }

    Cursor getHistoryCursorForAsset(int assetId) {
        return dataHelper.getWritableDatabase().query(
                History.TABLE,
                null,
                History.ASSET + " = " + assetId,
                null,
                null,
                null,
                History.ID + " DESC");
    }

    Cursor getHistoryCursor() {
        return dataHelper.getWritableDatabase().query(
                History.TABLE,
                null,
                null,
                null,
                null,
                null,
                History.ID + " DESC");
    }

    // Write, return true if OK

    boolean addCurrency(String currencyName) {
        ContentValues cv = new ContentValues();
        cv.put(Currencies.NAME, currencyName);
        cv.put(Currencies.COURSE, "1"); // set initial course to 1
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (database.insert(Currencies.TABLE, null, cv) == -1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        load(); // reload cache
        return true;
    }

    boolean addAsset(String assetName, int currencyId) {
        // Find last asset
        int lastOrder = 0;
        Cursor cursor = dataHelper.getWritableDatabase().query(
                Assets.TABLE,
                null,
                null,
                null,
                null,
                null,
                Assets.ORDER + " DESC",
                "1"
        );
        if (cursor.moveToFirst()) {
            lastOrder = Assets.getOrder(cursor);
        }
        cursor.close();
        // Add asset in the end of the ordered list
        ContentValues cv = new ContentValues();
        cv.put(Assets.NAME, assetName);
        cv.put(Assets.CURRENCY, currencyId);
        cv.put(Assets.MAIN_VALUE, "0"); // set initial value to 0
        if (currencyId != Currencies.ID_MAIN) {
            cv.put(Assets.LOCAL_VALUE, "0");
        }
        cv.put(Assets.ORDER, lastOrder + 1);
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (database.insert(Assets.TABLE, null, cv) == -1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return true;
    }

    boolean renameCurrency(int currencyId, String currencyName) {
        ContentValues cv = new ContentValues();
        cv.put(Currencies.NAME, currencyName);
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (database.update(Currencies.TABLE, cv, Currencies.ID + " = " + currencyId, null) != 1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        load(); // reload cache
        return true;
    }

    boolean renameAsset(int assetId, String assetName) {
        ContentValues cv = new ContentValues();
        cv.put(Assets.NAME, assetName);
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (database.update(Assets.TABLE, cv, Assets.ID + " = " + assetId, null) != 1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return true;
    }

    boolean changeHistoryDescription(int historyId, String description) {
        ContentValues cv = new ContentValues();
        cv.put(History.DESCRIPTION, description);
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (database.update(History.TABLE, cv, History.ID + " = " + historyId, null) != 1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return true;
    }

    // Set asset with movedId before or after item with targetId
    boolean move(int movedId, int targetId) {
        // Find orders of items
        Cursor cursor = getAssetCursor(movedId);
        cursor.moveToFirst();
        int movedOrder = Assets.getOrder(cursor);
        cursor.close();
        cursor = getAssetCursor(targetId);
        cursor.moveToFirst();
        int targetOrder = Assets.getOrder(cursor);
        cursor.close();
        // targetOrder, n, ... , movedOrder; targetOrder+=1, n+=1, ...
        // movedOrder, n, ... , targetOrder; n-=1, ... , targetOrder-=1
        // Move each item between moved and target, include target, up or down
        boolean orderPlus = movedOrder > targetOrder;
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        cursor = database.query(
                Assets.TABLE,
                null,
                Assets.ORDER + " > " + (orderPlus ? targetOrder - 1 : movedOrder) + " AND "
                        + Assets.ORDER + " < " + (orderPlus ? movedOrder : targetOrder + 1),
                null,
                null,
                null,
                Assets.ORDER
        );
        cursor.moveToFirst();
        database.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            do {
                cv.put(Assets.ORDER, Assets.getOrder(cursor) + (orderPlus ? 1 : -1));
                if (database.update(Assets.TABLE, cv, Assets.ID + " = " + Assets.getId(cursor), null) != 1) {
                    return false;
                }
                cv.clear();
            } while (cursor.moveToNext());
            // Set moved item on the place of target item
            cv.put(Assets.ORDER, targetOrder);
            if (database.update(Assets.TABLE, cv, Assets.ID + " = " + movedId, null) != 1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            cursor.close();
            database.endTransaction();
        }
        return true;
    }

    // Scale of main value is the same as scale of course
    boolean updateCourse(int currencyId, String course) {
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        Cursor cursor = database.query(
                Assets.TABLE,
                null,
                Assets.CURRENCY + " = " + currencyId,
                null,
                null,
                null,
                null);
        boolean hasAssets = cursor.moveToFirst();
        database.beginTransaction();
        try {
            Calculator calculator = Calculator.get(null, null, course); // throw exception if non-positive
            ContentValues cv = new ContentValues();
            // Calculate and update all asset with this currency
            if (hasAssets) {
                do {
                    String result = calculator.getResultMain(Assets.getLocalValue(cursor));
                    cv.put(Assets.MAIN_VALUE, result);
                    if (database.update(Assets.TABLE, cv, Assets.ID + " = " + Assets.getId(cursor), null) != 1) {
                        return false;
                    }
                    cv.clear();
                } while (cursor.moveToNext());
            }
            // Update course
            cv.put(Currencies.COURSE, course);
            if (database.update(Currencies.TABLE, cv, Currencies.ID + " = " + currencyId, null) != 1) {
                return false;
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            return false;
        } finally {
            cursor.close();
            database.endTransaction();
        }
        return calculateTotalValue(); // calculate total value
    }

    // localValueDelta = localValueResult = null for main currency
    boolean makeHistory(
            int assetId,
            String mainValueDelta,
            String mainValueResult,
            String localValueDelta,
            String localValueResult,
            String description
    ) {
        ContentValues historyCv = new ContentValues();
        ContentValues assetCv = new ContentValues();
        historyCv.put(History.ASSET, assetId);
        historyCv.put(History.MAIN_VALUE_DELTA, mainValueDelta);
        historyCv.put(History.MAIN_VALUE_RESULT, mainValueResult);
        assetCv.put(Assets.MAIN_VALUE, mainValueResult);
        if (localValueDelta != null) {
            historyCv.put(History.LOCAL_VALUE_DELTA, localValueDelta);
            historyCv.put(History.LOCAL_VALUE_RESULT, localValueResult);
            assetCv.put(Assets.LOCAL_VALUE, localValueResult);
        }
        historyCv.put(History.TIME, new Date().getTime());
        historyCv.put(History.DESCRIPTION, description);
        // Create history item and update asset
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (database.insert(History.TABLE, null, historyCv) == -1) {
                return false;
            }
            if (database.update(Assets.TABLE, assetCv, Assets.ID + " = " + assetId, null) != 1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return calculateTotalValue(); // calculate total value
    }

    // Check !currencyHasAssets(currencyId) before
    boolean deleteCurrency(int currencyId) {
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (database.delete(Currencies.TABLE, Currencies.ID + " = " + currencyId, null) != 1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return true;
    }

    // Check assetIsZero(assetId) and !assetHasHistory(assetId) before
    boolean deleteAsset(int assetId) {
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (database.delete(Assets.TABLE, Assets.ID + " = " + assetId, null) != 1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return true;
    }

    // Check !hasLaterHistory(historyId) before
    boolean removeHistoryItem(int historyId) {
        Cursor historyCursor = getHistoryCursor(historyId);
        historyCursor.moveToFirst();
        int assetId = History.getAssetId(historyCursor);

        Cursor assetCursor = getAssetCursor(assetId);
        assetCursor.moveToFirst();
        int currencyId = Assets.getCurrencyId(assetCursor);
        assetCursor.close();
        // Calculate previous values
        ContentValues cv = new ContentValues();
        if (currencyId == Currencies.ID_MAIN) {
            Calculator calculator = Calculator.get(History.getMainValueResult(historyCursor), null, null);
            String mainValue = calculator.getResultByDelta(History.getMainValueDelta(historyCursor), false);
            cv.put(Assets.MAIN_VALUE, mainValue);
        } else {
            Cursor currencyCursor = getCurrencyCursor(currencyId);
            currencyCursor.moveToFirst();
            String course = Currencies.getCourse(currencyCursor);
            currencyCursor.close();

            Calculator calculator = Calculator.get(History.getLocalValueResult(historyCursor), null, course);
            String localValue = calculator.getResultByDelta(History.getLocalValueDelta(historyCursor), false);
            cv.put(Assets.MAIN_VALUE, calculator.getResultMain(null));
            cv.put(Assets.LOCAL_VALUE, localValue);
        }
        historyCursor.close();
        // Update asset, remove history item
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (database.update(Assets.TABLE, cv, Assets.ID + " = " + assetId, null) != 1) {
                return false;
            }
            if (database.delete(History.TABLE, History.ID + " = " + historyId, null) != 1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return calculateTotalValue(); // calculate total value
    }

    void clearOldHistory() {
        long lastTime = new Date().getTime() - KEEP_HISTORY_TIME;
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        // Delete all item before lastTime
        database.beginTransaction();
        try {
            database.delete(History.TABLE, History.TIME + " < " + lastTime, null);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    // Backup and restore, throws exception if can not do it

    // Return file path
    String backup() throws IOException {
        close(); // close all objects
        File backupFolder = new File(Environment.getExternalStorageDirectory(), BACKUP_FOLDER);
        if (!backupFolder.exists()) {
            if (!backupFolder.mkdirs()) {
                throw new IOException("Can not create folder");
            }
        }
        File backupFile = new File(backupFolder, BACKUP_SDF.format(new Date()));
        FileOutputStream outputStream = new FileOutputStream(backupFile);
        FileInputStream inputStream = new FileInputStream(getDatabasePath(DATABASE_NAME));
        FileChannel fromChannel = inputStream.getChannel();
        FileChannel toChannel = outputStream.getChannel();
        fromChannel.transferTo(0, fromChannel.size(), toChannel);
        outputStream.close();
        inputStream.close();
        return backupFile.getAbsolutePath();
    }

    void restore(String fileName) throws IOException {
        close(); // close all objects
        File backupFolder = new File(Environment.getExternalStorageDirectory(), BACKUP_FOLDER);
        File backupFile = new File(backupFolder, fileName);
        FileOutputStream outputStream = new FileOutputStream(getDatabasePath(DATABASE_NAME));
        FileInputStream inputStream = new FileInputStream(backupFile);
        FileChannel fromChannel = inputStream.getChannel();
        FileChannel toChannel = outputStream.getChannel();
        fromChannel.transferTo(0, fromChannel.size(), toChannel);
        outputStream.close();
        inputStream.close();
        load(); // load cache
    }

    // Load currencies names
    private void load() {
        Cursor cursor = getCurrenciesCursor();
        cursor.moveToLast();
        currenciesNames = new String[Currencies.getId(cursor)];
        do {
            currenciesNames[Currencies.getId(cursor) - 1] = Currencies.getName(cursor);
        } while (cursor.moveToPrevious());
        cursor.close();
    }

    private boolean calculateTotalValue() {
        // Calculate sum
        Calculator calculator = Calculator.get(null, null, null);
        Cursor cursor = getAssetsCursor();
        if (cursor.moveToFirst()) {
            do {
                calculator.add(Assets.getMainValue(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        // Update total value
        ContentValues cv = new ContentValues();
        cv.put(Currencies.COURSE, calculator.getTotal());
        SQLiteDatabase database = dataHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (database.update(Currencies.TABLE, cv, Currencies.ID + " = " + Currencies.ID_MAIN, null) != 1) {
                return false;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return true;
    }

    // Main currency stored in first row with id = 1
    // total value stored in first row in column COURSE
    static final class Currencies {
        static final String TABLE = "C";
        // Columns
        static final String ID = BaseColumns._ID; // integer started from 1
        static final String NAME = "N"; // string
        static final String COURSE = "C"; // string, scale of course = scale of main value
        // Id of main currency
        static final int ID_MAIN = 1;

        // Returns value for cursor with all columns in current position

        static int getId(Cursor cursor) {
            return cursor.getInt(0);
        }

        static String getName(Cursor cursor) {
            return cursor.getString(1);
        }

        static String getCourse(Cursor cursor) {
            return cursor.getString(2);
        }
    }

    static final class Assets {
        static final String TABLE = "A";
        // Columns
        static final String ID = BaseColumns._ID; // integer started from 1
        static final String NAME = "N"; // string
        static final String CURRENCY = "C"; // integer, currency id
        static final String MAIN_VALUE = "MV"; // string
        static final String LOCAL_VALUE = "LV"; // string or null (for main currency)
        static final String ORDER = "O"; // integer started from 1

        // Returns value for cursor with all columns in current position

        static int getId(Cursor cursor) {
            return cursor.getInt(0);
        }

        static String getName(Cursor cursor) {
            return cursor.getString(1);
        }

        static int getCurrencyId(Cursor cursor) {
            return cursor.getInt(2);
        }

        static String getMainValue(Cursor cursor) {
            return cursor.getString(3);
        }

        static String getLocalValue(Cursor cursor) {
            return cursor.getString(4);
        }

        static int getOrder(Cursor cursor) {
            return cursor.getInt(5);
        }
    }

    static final class History {
        static final String TABLE = "H";
        // Columns
        static final String ID = BaseColumns._ID; // integer started from 1
        static final String ASSET = "A"; // integer, id of asset
        static final String MAIN_VALUE_DELTA = "MVD"; // string
        static final String MAIN_VALUE_RESULT = "MVR"; // string
        static final String LOCAL_VALUE_DELTA = "LVD"; // string or null (for main currency)
        static final String LOCAL_VALUE_RESULT = "LVR"; // string or null (same type as LOCAL_VALUE_DELTA)
        static final String TIME = "T"; // integer (long), unix time
        static final String DESCRIPTION = "D"; // string

        // Returns value for cursor with all columns in current position

        static int getAssetId(Cursor cursor) {
            return cursor.getInt(1);
        }

        static String getMainValueDelta(Cursor cursor) {
            return cursor.getString(2);
        }

        static String getMainValueResult(Cursor cursor) {
            return cursor.getString(3);
        }

        static String getLocalValueDelta(Cursor cursor) {
            return cursor.getString(4);
        }

        static String getLocalValueResult(Cursor cursor) {
            return cursor.getString(5);
        }

        static long getTime(Cursor cursor) {
            return cursor.getLong(6);
        }

        static String getDescription(Cursor cursor) {
            return cursor.getString(7);
        }
    }

    private class DataHelper extends SQLiteOpenHelper implements BaseColumns {
        DataHelper(Context context) {
            super(context, DATABASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            ContentValues cv = new ContentValues();
            cv.put(Currencies.NAME, "BTC");
            cv.put(Currencies.COURSE, "0"); // set total value to 0
            database.beginTransaction();
            try {
                database.execSQL("CREATE TABLE " + Currencies.TABLE + " ("
                        + Currencies.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + Currencies.NAME + " TEXT NOT NULL, "
                        + Currencies.COURSE + " TEXT NOT NULL);");
                database.execSQL("CREATE TABLE " + Assets.TABLE + " ("
                        + Assets.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + Assets.NAME + " TEXT NOT NULL, "
                        + Assets.CURRENCY + " INTEGER NOT NULL, "
                        + Assets.MAIN_VALUE + " TEXT NOT NULL, "
                        + Assets.LOCAL_VALUE + " TEXT, "
                        + Assets.ORDER + " INTEGER NOT NULL);");
                database.execSQL("CREATE TABLE " + History.TABLE + " ("
                        + History.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + History.ASSET + " INTEGER NOT NULL, "
                        + History.MAIN_VALUE_DELTA + " TEXT NOT NULL, "
                        + History.MAIN_VALUE_RESULT + " TEXT NOT NULL, "
                        + History.LOCAL_VALUE_DELTA + " TEXT, "
                        + History.LOCAL_VALUE_RESULT + " TEXT, "
                        + History.TIME + " INTEGER NOT NULL, "
                        + History.DESCRIPTION + " TEXT NOT NULL);");
                if (database.insert(Currencies.TABLE, null, cv) != Currencies.ID_MAIN) {
                    return;
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        }
    }
}