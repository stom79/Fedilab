package app.fedilab.android.mastodon.services;

import static app.fedilab.android.mastodon.helper.Helper.TAG;


import android.content.SharedPreferences;


import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;


import org.unifiedpush.android.connector.FailedReason;
import org.unifiedpush.android.connector.PushService;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PushMessage;

import app.fedilab.android.R;

import app.fedilab.android.mastodon.helper.NotificationsHelper;
import app.fedilab.android.mastodon.helper.PushNotifications;

public class PushServiceImpl extends PushService {


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onMessage(@NonNull PushMessage pushMessage, @NonNull String slug) {
        new Thread(() -> {
            try {
                /*if( pushMessage.getDecrypted()) {
                    String decryptedMessage = new String(pushMessage.getContent(), StandardCharsets.UTF_8);
                    JSONObject decryptedMessageJSON = new JSONObject(decryptedMessage);
                } else {

                }*/
                NotificationsHelper.task(getApplicationContext(), slug);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }).start();
    }

    @Override
    public void onNewEndpoint(@NonNull PushEndpoint pushEndpoint, @NonNull String slug) {
        if (getApplicationContext() != null) {
            synchronized (this) {
                SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String storedEnpoint = sharedpreferences.getString(getApplicationContext().getString(R.string.SET_STORED_ENDPOINT) + slug, null);
                if (storedEnpoint == null || !storedEnpoint.equals(pushEndpoint.getUrl())) {
                    PushNotifications
                            .registerPushNotifications(getApplicationContext(), pushEndpoint, slug);
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(getApplicationContext().getString(R.string.SET_STORED_ENDPOINT) + slug, pushEndpoint.getUrl());
                    editor.commit();
                }
            }
        }
    }

    @Override
    public void onRegistrationFailed(@NonNull FailedReason failedReason, @NonNull String s) {

    }

    @Override
    public void onUnregistered(@NonNull String s) {
    }
}
