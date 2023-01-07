package app.fedilab.android.client.endpoints;
/* Copyright 2021 Thomas Schneider
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

import java.util.List;

import app.fedilab.android.client.entities.api.Notification;
import app.fedilab.android.client.entities.api.PushSubscription;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MastodonNotificationsService {

    @GET("notifications")
    Call<List<Notification>> getNotifications(
            @Header("Authorization") String token,
            @Query("exclude_types[]") List<String> exclude_types,
            @Query("account_id") String account_id,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id,
            @Query("limit") int limit
    );

    @GET("notifications/{id}")
    Call<Notification> getNotification(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    @POST("notifications/clear")
    Call<Void> clearAllNotifications(
            @Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("notifications/{id}/dismiss")
    Call<Void> dismissNotification(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    @FormUrlEncoded
    @POST("push/subscription")
    Call<PushSubscription> pushSubscription(
            @Header("Authorization") String token,
            @Field("subscription[endpoint]") String endpoint,
            @Field("subscription[keys][p256dh]") String keys_p256dh,
            @Field("subscription[keys][auth]") String keys_auth,
            @Field("data[alerts][follow]") boolean follow,
            @Field("data[alerts][favourite]") boolean favourite,
            @Field("data[alerts][reblog]") boolean reblog,
            @Field("data[alerts][mention]") boolean mention,
            @Field("data[alerts][poll]") boolean poll,
            @Field("data[alerts][status]") boolean status,
            @Field("data[alerts][update]") boolean update,
            @Field("data[alerts][admin.sign_up]") boolean admin_sign_up,
            @Field("data[alerts][admin.report]") boolean admin_report

    );

    @GET("push/subscription")
    Call<PushSubscription> getPushSubscription(
            @Header("Authorization") String token
    );


    @FormUrlEncoded
    @PUT("push/subscription")
    Call<PushSubscription> updatePushSubscription(
            @Header("Authorization") String token,
            @Field("data[alerts][follow]") boolean follow,
            @Field("data[alerts][favourite]") boolean favourite,
            @Field("data[alerts][reblog]") boolean reblog,
            @Field("data[alerts][mention]") boolean mention,
            @Field("data[alerts][poll]") boolean poll
    );

    @DELETE("push/subscription")
    Call<Void> deletePushsubscription(
            @Header("Authorization") String token
    );
}
