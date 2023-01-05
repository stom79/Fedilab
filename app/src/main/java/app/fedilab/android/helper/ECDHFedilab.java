package app.fedilab.android.helper;
/* Copyright 2022 Thomas Schneider
 *
 * This file is a part of Fedilab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Fedilab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Fedilab; if not,
 * see <http://www.gnu.org/licenses>. */

import static app.fedilab.android.client.entities.app.StatusCache.restoreNotificationFromString;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import app.fedilab.android.client.entities.api.Notification;


public class ECDHFedilab {


    public static final String kp_public = "kp_public";
    public static final String peer_public = "peer_public";

    public static final String name = "prime256v1";
    private static final byte[] P256_HEAD = new byte[]{(byte) 0x30, (byte) 0x59, (byte) 0x30, (byte) 0x13, (byte) 0x06, (byte) 0x07, (byte) 0x2a,
            (byte) 0x86, (byte) 0x48, (byte) 0xce, (byte) 0x3d, (byte) 0x02, (byte) 0x01, (byte) 0x06, (byte) 0x08, (byte) 0x2a, (byte) 0x86,
            (byte) 0x48, (byte) 0xce, (byte) 0x3d, (byte) 0x03, (byte) 0x01, (byte) 0x07, (byte) 0x03, (byte) 0x42, (byte) 0x00};

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }

    private final KeyPairGenerator kpg;
    private final PublicKey publicKey;
    private final String encodedPublicKey;
    private final byte[] authKey;
    private final String slug;
    private final String pushPublicKey;
    private final String encodedAuthKey;
    private final String pushAccountID;
    private final String pushPrivateKey;
    PrivateKey privateKey;
    private String pushPrivateKe;

    public ECDHFedilab(Context context, String slug) throws Exception {
        if (slug == null) {
            throw new Exception("slug cannot be null");
        }
        try {
            kpg = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec spec = new ECGenParameterSpec("prime256v1");
            kpg.initialize(spec);
            KeyPair keyPair = kpg.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
            encodedPublicKey = Base64.encodeToString(serializeRawPublicKey(publicKey), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            authKey = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(authKey);
            byte[] randomAccountID = new byte[16];
            secureRandom.nextBytes(randomAccountID);
            pushPrivateKey = Base64.encodeToString(privateKey.getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            pushPublicKey = Base64.encodeToString(publicKey.getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            encodedAuthKey = Base64.encodeToString(authKey, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            pushAccountID = Base64.encodeToString(randomAccountID, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            SharedPreferences.Editor prefsEditor = PreferenceManager
                    .getDefaultSharedPreferences(context).edit();
            prefsEditor.putString("pushPrivateKey" + slug, pushPrivateKey);
            prefsEditor.putString("pushPublicKey" + slug, pushPublicKey);
            prefsEditor.putString("encodedAuthKey" + slug, encodedAuthKey);
            prefsEditor.putString("pushAccountID" + slug, pushAccountID);
            prefsEditor.apply();
            this.slug = slug;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String getServerKey(Context context, String slug) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        return sharedPreferences.getString("server_key" + slug, null);
    }

    private static byte[] serializeRawPublicKey(PublicKey key) {
        ECPoint point = ((ECPublicKey) key).getW();
        byte[] x = point.getAffineX().toByteArray();
        byte[] y = point.getAffineY().toByteArray();
        if (x.length > 32)
            x = Arrays.copyOfRange(x, x.length - 32, x.length);
        if (y.length > 32)
            y = Arrays.copyOfRange(y, y.length - 32, y.length);
        byte[] result = new byte[65];
        result[0] = 4;
        System.arraycopy(x, 0, result, 1 + (32 - x.length), x.length);
        System.arraycopy(y, 0, result, result.length - y.length, y.length);
        return result;
    }

    public static Notification decryptNotification(Context context, String slug, byte[] messageEncrypted) {


        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        Log.v(Helper.TAG, ">>slug: " + slug);
        String pushPrivateKey = sharedPreferences.getString("pushPrivateKey" + slug, null);
        String pushPublicKey = sharedPreferences.getString("pushPublicKey" + slug, null);
        String encodedAuthKey = sharedPreferences.getString("encodedAuthKey" + slug, null);
        sharedPreferences.getString("pushAccountID" + slug, null);


        Log.v(Helper.TAG, "getServerKey(context, slug): " + getServerKey(context, slug));


        Log.v(Helper.TAG, "pushPrivateKey: " + pushPrivateKey);
        Log.v(Helper.TAG, "pushPublicKey: " + pushPublicKey);
        Log.v(Helper.TAG, "encodedAuthKey: " + encodedAuthKey);

        PublicKey serverKey = null;
        serverKey = deserializeRawPublicKey(Base64.decode(getServerKey(context, slug), Base64.URL_SAFE));
        Log.v(Helper.TAG, "serverKey: " + serverKey);
        PrivateKey privateKey;
        PublicKey publicKey;
        byte[] authKey;
        try {
            KeyFactory kf = KeyFactory.getInstance("EC");
            privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(pushPrivateKey, Base64.URL_SAFE)));
            publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.decode(pushPublicKey, Base64.URL_SAFE)));
            authKey = Base64.decode(encodedAuthKey, Base64.URL_SAFE);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            Log.v(Helper.TAG, "err1: " + e.getMessage());
            return null;
        }
        byte[] sharedSecret;
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(serverKey, true);
            sharedSecret = keyAgreement.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            Log.v(Helper.TAG, "err2: " + e.getMessage());
            return null;
        }
        byte[] secondSaltInfo = "Content-Encoding: auth\0".getBytes(StandardCharsets.UTF_8);
        byte[] deriveKey;
        try {
            deriveKey = deriveKey(authKey, sharedSecret, secondSaltInfo, 32);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            Log.v(Helper.TAG, "err3: " + e.getMessage());
            return null;
        }
        String decryptedStr;
        try {

            SecretKeySpec aesKey = new SecretKeySpec(deriveKey, "AES");
            byte[] iv = Arrays.copyOfRange(messageEncrypted, 0, 12);
            byte[] ciphertext = Arrays.copyOfRange(messageEncrypted, 12, messageEncrypted.length); // Separate ciphertext (the MAC is implicitly separated from the ciphertext)
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gCMParameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gCMParameterSpec);
            byte[] decrypted = cipher.doFinal(ciphertext);
            decryptedStr = new String(decrypted, 2, decrypted.length - 2, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            Log.v(Helper.TAG, "err4: " + e.getMessage());
            return null;
        }
        return restoreNotificationFromString(decryptedStr);
    }

    protected static PublicKey deserializeRawPublicKey(byte[] rawBytes) {
        if (rawBytes.length != 65 && rawBytes.length != 64)
            return null;
        try {
            KeyFactory kf = KeyFactory.getInstance("EC");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(P256_HEAD);
            if (rawBytes.length == 64)
                os.write(4);
            os.write(rawBytes);
            return kf.generatePublic(new X509EncodedKeySpec(os.toByteArray()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] deriveKey(byte[] firstSalt, byte[] secondSalt, byte[] info, int length) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmacContext = Mac.getInstance("HmacSHA256");
        hmacContext.init(new SecretKeySpec(firstSalt, "HmacSHA256"));
        byte[] hmac = hmacContext.doFinal(secondSalt);
        hmacContext.init(new SecretKeySpec(hmac, "HmacSHA256"));
        hmacContext.update(info);
        byte[] result = hmacContext.doFinal(new byte[]{1});
        return result.length <= length ? result : Arrays.copyOfRange(result, 0, length);
    }

    public String getPublicKey() {
        return this.encodedPublicKey;
    }

    public String getAuthKey() {
        return this.encodedAuthKey;
    }

}
