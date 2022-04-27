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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.preference.PreferenceManager;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x9.ECNamedCurveTable;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.params.ECNamedDomainParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.jce.spec.ECNamedCurveSpec;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPrivateKeySpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;


public class ECDH {


    public static final String kp_public = "kp_public";
    public static final String peer_public = "peer_public";
    public static final String PROVIDER = org.spongycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

    public static final String kp_private = "kp_private";
    public static final String KEGEN_ALG = "ECDH";

    public static final String name = "prime256v1";

    private static final String kp_public_affine_x = "kp_public_affine_x";
    private static final String kp_public_affine_y = "kp_public_affine_y";

    private static ECDH instance;

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }

    public final KeyFactory kf;
    private final KeyPairGenerator kpg;
    private final String slug;

    public ECDH(String slug) throws Exception {
        if (slug == null) {
            throw new Exception("slug cannot be null");
        }
        try {
            kf = KeyFactory.getInstance(KEGEN_ALG, PROVIDER);
            kpg = KeyPairGenerator.getInstance(KEGEN_ALG, PROVIDER);
            this.slug = slug;
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static synchronized ECDH getInstance(String slug) throws Exception {
        if (instance == null) {
            instance = new ECDH(slug);
        }
        return instance;
    }

    public static String base64Encode(byte[] b) {
        return Base64.encodeToString(
                b, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    static byte[] base64Decode(String str) {
        return Base64.decode(str, Base64.URL_SAFE);
    }

    synchronized KeyPair generateKeyPair()
            throws Exception {
        ECGenParameterSpec ecParamSpec = new ECGenParameterSpec(name);
        kpg.initialize(ecParamSpec);

        return kpg.generateKeyPair();
    }

    private byte[] generateSecret(PrivateKey myPrivKey, PublicKey otherPubKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance(KEGEN_ALG);
        keyAgreement.init(myPrivKey);
        keyAgreement.doPhase(otherPubKey, true);

        return keyAgreement.generateSecret();
    }


    synchronized KeyPair readKeyPair(Context context)
            throws Exception {
        return new KeyPair(readMyPublicKey(context), readMyPrivateKey(context));
    }

    @SuppressLint("ApplySharedPref")
    public KeyPair newPair(Context context) {
        SharedPreferences.Editor prefsEditor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        KeyPair kp;
        try {
            kp = generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        ECPublicKey key = (ECPublicKey) kp.getPublic();
        byte[] x = key.getW().getAffineX().toByteArray();
        byte[] y = key.getW().getAffineY().toByteArray();
        BigInteger xbi = new BigInteger(1, x);
        BigInteger ybi = new BigInteger(1, y);
        X9ECParameters x9 = ECNamedCurveTable.getByName(name);
        ASN1ObjectIdentifier oid = ECNamedCurveTable.getOID(name);

        ECCurve curve = x9.getCurve();
        ECPoint point = curve.createPoint(xbi, ybi);
        ECNamedDomainParameters dParams = new ECNamedDomainParameters(oid,
                x9.getCurve(), x9.getG(), x9.getN(), x9.getH(), x9.getSeed());

        ECPublicKeyParameters pubKey = new ECPublicKeyParameters(point, dParams);


        ECPrivateKeyParameters privateKey = new ECPrivateKeyParameters(new BigInteger(kp.getPrivate().getEncoded()), pubKey.getParameters());
        byte[] privateKeyBytes = privateKey.getD().toByteArray();

        String keyString = base64Encode(pubKey.getQ().getEncoded(false));
        String keypString = base64Encode(privateKeyBytes);
        prefsEditor.putString(kp_public + slug, keyString);
        prefsEditor.putString(kp_public_affine_x + slug, key.getW().getAffineX().toString());
        prefsEditor.putString(kp_public_affine_y + slug, key.getW().getAffineY().toString());
        prefsEditor.putString(kp_private + slug, keypString);
        prefsEditor.commit();
        return kp;
    }


    synchronized PublicKey readMyPublicKey(Context context) throws Exception {

        X9ECParameters x9 = ECNamedCurveTable.getByName(name);
        ASN1ObjectIdentifier oid = ECNamedCurveTable.getOID(name);

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        BigInteger xbi = new BigInteger(prefs.getString(kp_public_affine_x + slug, "0"));
        BigInteger ybi = new BigInteger(prefs.getString(kp_public_affine_y + slug, "0"));

        ECNamedDomainParameters dParams = new ECNamedDomainParameters(oid,
                x9.getCurve(), x9.getG(), x9.getN(), x9.getH(), x9.getSeed());


        ECNamedCurveSpec ecNamedCurveSpec = new ECNamedCurveSpec(name, dParams.getCurve(), dParams.getG(), dParams.getN());
        java.security.spec.ECPoint w = new java.security.spec.ECPoint(xbi, ybi);
        return kf.generatePublic(new java.security.spec.ECPublicKeySpec(w, ecNamedCurveSpec));
    }


    public String uncryptMessage(Context context, String cyphered) {
        byte[] privateKey = getSharedSecret(context);
        try {
            Cipher outCipher = Cipher.getInstance("ECIES", PROVIDER);
            PrivateKey ddd = readPrivateKey(privateKey);
            outCipher.init(Cipher.DECRYPT_MODE, readPrivateKey(privateKey));
            byte[] plaintext = outCipher.doFinal(base64Decode(cyphered));
            return new String(plaintext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";

    }


    public PublicKey readPublicKey(String keyStr) throws Exception {
        ECParameterSpec parameterSpec = org.spongycastle.jce.ECNamedCurveTable.getParameterSpec(name);
        ECCurve curve = parameterSpec.getCurve();
        ECPoint point = curve.decodePoint(base64Decode(keyStr));
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, parameterSpec);
        return kf.generatePublic(pubSpec);
    }


    public PrivateKey readPrivateKey(byte[] key) throws Exception {
        ECParameterSpec parameterSpec = org.spongycastle.jce.ECNamedCurveTable.getParameterSpec(name);
        ECPrivateKeySpec pubSpec = new ECPrivateKeySpec(new BigInteger(1, key), parameterSpec);
        return kf.generatePrivate(pubSpec);
    }

    synchronized PrivateKey readMyPrivateKey(Context context) throws Exception {
        X9ECParameters x9 = ECNamedCurveTable.getByName(name);
        ASN1ObjectIdentifier oid = ECNamedCurveTable.getOID(name);

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        BigInteger ybi = new BigInteger(prefs.getString(kp_public_affine_y + slug, "0"));
        ECNamedDomainParameters dParams = new ECNamedDomainParameters(oid,
                x9.getCurve(), x9.getG(), x9.getN(), x9.getH(), x9.getSeed());
        ECNamedCurveSpec ecNamedCurveSpec = new ECNamedCurveSpec(name, dParams.getCurve(), dParams.getG(), dParams.getN());
        return kf.generatePrivate(new java.security.spec.ECPrivateKeySpec(ybi, ecNamedCurveSpec));
    }


    private synchronized KeyPair getPair(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String strPub = prefs.getString(kp_public + slug, "");
        String strPriv = prefs.getString(kp_private + slug, "");
        if (strPub.trim().isEmpty() || strPriv.trim().isEmpty()) {
            return newPair(context);
        }
        try {
            return readKeyPair(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    PublicKey getServerKey(Context context) throws Exception {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String serverKey = prefs.getString(peer_public + slug, "");
        return readPublicKey(serverKey);
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public byte[] getSharedSecret(Context context) {
        try {
            KeyPair keyPair = getPair(context);
            if (keyPair != null) {
                return generateSecret(keyPair.getPrivate(), getServerKey(context));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public String getPublicKey(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        return prefs.getString(kp_public + slug, "");
    }

    @SuppressLint("ApplySharedPref")
    public void saveServerKey(Context context, String strPeerPublic) {
        SharedPreferences.Editor prefsEditor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();

        prefsEditor.putString(peer_public + slug, strPeerPublic);
        prefsEditor.commit();
    }
}
