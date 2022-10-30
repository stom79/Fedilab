package app.fedilab.android.client.entities.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.sqlite.Sqlite;


public class DomainsBlock {

    public static final String LAST_DATE_OF_UPDATE = "LAST_DATE_OF_UPDATE";
    public static List<String> trackingDomains = null;

    private static void getDomains(Context context) {
        if (trackingDomains == null) {
            try {
                SQLiteDatabase db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
                Cursor c = db.query(Sqlite.TABLE_DOMAINS_TRACKING, null, null, null, null, null, null, null);
                trackingDomains = cursorToDomain(c);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateDomains(Context _mContext) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(_mContext);
        String last_date = sharedpreferences.getString(LAST_DATE_OF_UPDATE, null);
        Date dateUpdate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10));
        Date dateLastUpdate = Helper.stringToDate(_mContext, last_date);
        SQLiteDatabase db = Sqlite.getInstance(_mContext.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
        if (last_date == null || dateUpdate.after(dateLastUpdate)) {
            new Thread(() -> {
                try {
                    HttpsURLConnection connection = (HttpsURLConnection) new URL("https://hosts.fedilab.app/hosts").openConnection();
                    if (connection.getResponseCode() > HttpsURLConnection.HTTP_MOVED_TEMP) {
                        return;
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    List<String> domains = new ArrayList<>();
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("0.0.0.0 ")) {
                            try {
                                domains.add(line.replace("0.0.0.0 ", "").trim());
                            } catch (Exception e) {
                                return;
                            }

                        }
                    }
                    br.close();
                    connection.disconnect();
                    insertDomains(db, domains);
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(LAST_DATE_OF_UPDATE, Helper.dateToString(new Date()));
                    editor.apply();
                } catch (IOException | DBException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            getDomains(_mContext);
        }
    }

    /**
     * Insert a domains in db
     *
     * @param domains {@link List<String>}
     * @throws DBException exception with database
     */
    private static void insertDomains(SQLiteDatabase db, List<String> domains) throws DBException {

        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        db.delete(Sqlite.TABLE_DOMAINS_TRACKING, null, null);
        DomainsBlock.trackingDomains = new ArrayList<>();
        for (String domain : domains) {
            ContentValues values = new ContentValues();
            values.put(Sqlite.COL_DOMAIN, domain);
            //Inserts token
            try {
                db.insertOrThrow(Sqlite.TABLE_DOMAINS_TRACKING, null, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
            DomainsBlock.trackingDomains.add(domain);
        }
    }


    /***
     * Method to hydrate domain from database
     * @param c Cursor
     * @return List<String>
     */
    private static List<String> cursorToDomain(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<String> domains = new ArrayList<>();
        while (c.moveToNext()) {
            domains.add(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_DOMAIN)));
        }
        //Close the cursor
        c.close();
        //domains list is returned
        return domains;
    }
}
