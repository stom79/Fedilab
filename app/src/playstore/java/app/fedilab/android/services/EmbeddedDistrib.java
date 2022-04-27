package app.fedilab.android.services;


import org.unifiedpush.android.embedded_fcm_distributor.EmbeddedDistributorReceiver;

public class EmbeddedDistrib extends EmbeddedDistributorReceiver {
    public EmbeddedDistrib() {
        super(new HandlerFCM());
    }

}