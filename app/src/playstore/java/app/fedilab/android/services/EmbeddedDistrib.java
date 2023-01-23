package app.fedilab.android.services;


import android.content.Context;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.unifiedpush.android.embedded_fcm_distributor.EmbeddedDistributorReceiver;

public class EmbeddedDistrib extends EmbeddedDistributorReceiver {
    @Override
    public @NotNull
    String getEndpoint(@Nullable Context context, @NotNull String token, @NotNull String instance) {
        return "https://gotify.fedilab.app/FCM?v2&token=" + token + "&instance=" + instance;
    }
}