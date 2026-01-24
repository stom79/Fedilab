package com.github.stom79.mytransl.client;
/* Copyright 2017 Thomas Schneider
 *
 * This file is a part of MyTransL
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * MyTransL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MyTransL; if not,
 * see <http://www.gnu.org/licenses>. */

import android.os.Build;

import com.github.stom79.mytransl.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by @stom79 on 27/11/2017.
 * Manages GET and POST calls
 * Changed 10/01/2021
 */

public class Client {


    private static final String USER_AGENT = "MyTransL/" + BuildConfig.VERSION_NAME + " Android/" + Build.VERSION.RELEASE;

    public Client() {
    }


    /***
     * Get call to the translator API
     * @param urlConnection - String url to query
     * @param timeout - int a timeout
     * @return response - String
     * @throws IOException - Exception
     * @throws NoSuchAlgorithmException - Exception
     * @throws KeyManagementException - Exception
     * @throws HttpsConnectionException - Exception
     */
    @SuppressWarnings({"SameParameterValue"})
    public String get(String urlConnection, int timeout) throws IOException, NoSuchAlgorithmException, KeyManagementException, HttpsConnectionException {
        return get(urlConnection, timeout, null);
    }

    /***
     * Get call to the translator API with Authorization header
     * @param urlConnection - String url to query
     * @param timeout - int a timeout
     * @param authorizationHeader - String authorization header value (can be null)
     * @return response - String
     * @throws IOException - Exception
     * @throws NoSuchAlgorithmException - Exception
     * @throws KeyManagementException - Exception
     * @throws HttpsConnectionException - Exception
     */
    @SuppressWarnings({"SameParameterValue"})
    public String get(String urlConnection, int timeout, String authorizationHeader) throws IOException, NoSuchAlgorithmException, KeyManagementException, HttpsConnectionException {
        URL url = new URL(urlConnection);
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
        httpsURLConnection.setConnectTimeout(timeout * 1000);
        httpsURLConnection.setRequestProperty("http.keepAlive", "false");
        httpsURLConnection.setRequestProperty("User-Agent", USER_AGENT);
        if (authorizationHeader != null) {
            httpsURLConnection.setRequestProperty("Authorization", authorizationHeader);
        }
        httpsURLConnection.setRequestMethod("GET");
        //Read the reply
        if (httpsURLConnection.getResponseCode() >= 200 && httpsURLConnection.getResponseCode() < 400) {
            Reader in;
            in = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int c; (c = in.read()) >= 0; )
                sb.append((char) c);
            httpsURLConnection.disconnect();
            in.close();
            return sb.toString();
        } else {
            Reader in;
            in = new BufferedReader(new InputStreamReader(httpsURLConnection.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int c; (c = in.read()) >= 0; )
                sb.append((char) c);
            httpsURLConnection.disconnect();
            throw new HttpsConnectionException(httpsURLConnection.getResponseCode(), sb.toString());
        }
    }


    /***
     * POST call to the translator API
     * @param urlConnection - String url to query
     * @param timeout - int a timeout
     * @param jsonObject - parameters to send (JSON)
     * @return response - String
     * @throws IOException - Exception
     * @throws NoSuchAlgorithmException - Exception
     * @throws KeyManagementException - Exception
     * @throws HttpsConnectionException - Exception
     */
    @SuppressWarnings({"SameParameterValue", "unused", "RedundantSuppression"})
    public String post(String urlConnection, int timeout, JSONObject jsonObject) throws IOException, NoSuchAlgorithmException, KeyManagementException, HttpsConnectionException {
        URL url = new URL(urlConnection);
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
        byte[] postDataBytes;
        postDataBytes = jsonObject.toString().getBytes(StandardCharsets.UTF_8);
        httpsURLConnection.setRequestProperty("User-Agent", USER_AGENT);
        httpsURLConnection.setConnectTimeout(timeout * 1000);
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setDoOutput(true);
        httpsURLConnection.setUseCaches(false);
        httpsURLConnection.setRequestMethod("POST");
        httpsURLConnection.setRequestProperty("Content-Type", "application/json");
        httpsURLConnection.setRequestProperty("Accept", "application/json");
        httpsURLConnection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        // Send POST output
        DataOutputStream printout = new DataOutputStream(httpsURLConnection.getOutputStream());
        httpsURLConnection.getOutputStream().write(postDataBytes);
        printout.flush();
        printout.close();
        //Read the reply
        if (httpsURLConnection.getResponseCode() >= 200 && httpsURLConnection.getResponseCode() < 400) {
            Reader in;
            in = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int c; (c = in.read()) >= 0; )
                sb.append((char) c);
            httpsURLConnection.disconnect();
            in.close();
            return sb.toString();
        } else {
            Reader in;
            in = new BufferedReader(new InputStreamReader(httpsURLConnection.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int c; (c = in.read()) >= 0; )
                sb.append((char) c);
            httpsURLConnection.disconnect();
            throw new HttpsConnectionException(httpsURLConnection.getResponseCode(), sb.toString());
        }
    }

}
