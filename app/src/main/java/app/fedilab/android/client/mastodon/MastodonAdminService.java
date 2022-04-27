package app.fedilab.android.client.mastodon;
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

import app.fedilab.android.client.mastodon.entities.AdminAccount;
import app.fedilab.android.client.mastodon.entities.AdminReport;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MastodonAdminService {

    @GET("/admin/accounts")
    Call<List<AdminAccount>> getAccounts(
            @Header("Authorization") String token,
            @Query("local") boolean local,
            @Query("remote") boolean remote,
            @Query("by_domain") String by_domain,
            @Query("active") boolean active,
            @Query("pending") boolean pending,
            @Query("disabled") boolean disabled,
            @Query("silenced") boolean silenced,
            @Query("suspended") boolean suspended,
            @Query("username") String username,
            @Query("display_name") String display_name,
            @Query("email") String email,
            @Query("ip") String ip,
            @Query("staff") boolean staff,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("limit") int limit
    );

    @GET("/admin/accounts/{id}")
    Call<AdminAccount> getAccount(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    @POST("/admin/accounts/{account_id}/action")
    Call<Void> performAction(
            @Header("Authorization") String app_token,
            @Path("account_id") String account_id,
            @Field("type") String type,
            @Field("report_id") String report_id,
            @Field("warning_preset_id") String warning_preset_id,
            @Field("text") String text,
            @Field("send_email_notification") boolean send_email_notification
    );

    @FormUrlEncoded
    @POST("/admin/accounts/{account_id}/approve")
    Call<AdminAccount> approve(
            @Header("Authorization") String app_token,
            @Path("account_id") String account_id
    );

    @FormUrlEncoded
    @POST("/admin/accounts/{account_id}/reject")
    Call<AdminAccount> reject(
            @Header("Authorization") String app_token,
            @Path("account_id") String account_id
    );

    @FormUrlEncoded
    @POST("/admin/accounts/{account_id}/enable")
    Call<AdminAccount> enable(
            @Header("Authorization") String app_token,
            @Path("account_id") String account_id
    );

    @FormUrlEncoded
    @POST("/admin/accounts/{account_id}/unsilence")
    Call<AdminAccount> unsilence(
            @Header("Authorization") String app_token,
            @Path("account_id") String account_id
    );

    @FormUrlEncoded
    @POST("/admin/accounts/{account_id}/unsuspend")
    Call<AdminAccount> unsuspend(
            @Header("Authorization") String app_token,
            @Path("account_id") String account_id
    );

    @FormUrlEncoded
    @GET("/admin/reports")
    Call<List<AdminReport>> getReports(
            @Header("Authorization") String token,
            @Field("resolved") boolean resolved,
            @Field("account_id") String account_id,
            @Field("target_account_id") String target_account_id
    );

    @FormUrlEncoded
    @GET("/admin/reports/{id}")
    Call<AdminReport> getReport(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    @FormUrlEncoded
    @POST("/admin/reports/{id}/assign_to_self")
    Call<AdminReport> assignToSelf(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );

    @FormUrlEncoded
    @POST("/admin/reports/{id}/unassign")
    Call<AdminReport> unassign(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );

    @FormUrlEncoded
    @POST("/admin/reports/{id}/resolve")
    Call<AdminReport> resolved(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );

    @FormUrlEncoded
    @POST("/admin/reports/{id}/reopen")
    Call<AdminReport> reopen(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );
}
