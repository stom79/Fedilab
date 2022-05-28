package app.fedilab.android.viewmodel.mastodon;
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

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.client.endpoints.MastodonAdminService;
import app.fedilab.android.client.entities.api.AdminAccount;
import app.fedilab.android.client.entities.api.AdminAccounts;
import app.fedilab.android.client.entities.api.AdminReport;
import app.fedilab.android.client.entities.api.AdminReports;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AdminVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();
    private MutableLiveData<AdminAccount> adminAccountMutableLiveData;
    private MutableLiveData<AdminAccounts> adminAccountsListMutableLiveData;
    private MutableLiveData<AdminReport> adminReportMutableLiveData;
    private MutableLiveData<AdminReports> adminReporstListMutableLiveData;

    public AdminVM(@NonNull Application application) {
        super(application);
    }

    private MastodonAdminService init(@NonNull String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonAdminService.class);
    }

    /**
     * View accounts matching certain criteria for filtering, up to 100 at a time.
     *
     * @param instance    Instance domain of the active account
     * @param token       Access token of the active account
     * @param local       Filter for local accounts?
     * @param remote      Filter for remote accounts?
     * @param byDomain    Filter by the given domain
     * @param active      Filter for currently active accounts?
     * @param pending     Filter for currently pending accounts?
     * @param disabled    Filter for currently disabled accounts?
     * @param silenced    Filter for currently silenced accounts?
     * @param suspended   Filter for currently suspended accounts?
     * @param username    Username to search for
     * @param displayName Display name to search for
     * @param email       Lookup a user with this email
     * @param ip          Lookup users by this IP address
     * @param staff       Filter for staff accounts?
     * @return {@link LiveData} containing a {@link List} of {@link AdminAccount}s
     */
    public LiveData<AdminAccounts> getAccounts(@NonNull String instance,
                                               String token,
                                               Boolean local,
                                               Boolean remote,
                                               String byDomain,
                                               Boolean active,
                                               Boolean pending,
                                               Boolean disabled,
                                               Boolean silenced,
                                               Boolean suspended,
                                               String username,
                                               String displayName,
                                               String email,
                                               String ip,
                                               Boolean staff,
                                               String maxId,
                                               String sinceId,
                                               Integer limit) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminAccountsListMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<List<AdminAccount>> getAccountsCall = mastodonAdminService.getAccounts(
                    token, local, remote, byDomain, active, pending, disabled, silenced, suspended,
                    username, displayName, email, ip, staff, maxId, sinceId, limit);
            AdminAccounts adminAccounts = new AdminAccounts();
            if (getAccountsCall != null) {
                try {
                    Response<List<AdminAccount>> getAccountsResponse = getAccountsCall.execute();

                    if (getAccountsResponse.isSuccessful()) {
                        adminAccounts.adminAccounts = getAccountsResponse.body();
                        adminAccounts.pagination = MastodonHelper.getPagination(getAccountsResponse.headers());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> adminAccountsListMutableLiveData.setValue(adminAccounts);
            mainHandler.post(myRunnable);
        }).start();
        return adminAccountsListMutableLiveData;
    }

    /**
     * View admin-level information about the given account.
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       ID of the account
     * @return {@link LiveData} containing a {@link AdminAccount}
     */
    public LiveData<AdminAccount> getAccount(@NonNull String instance, String token, @NonNull String id) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminAccountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminAccount adminAccount = null;
            Call<AdminAccount> getAccountCall = mastodonAdminService.getAccount(token, id);
            if (getAccountCall != null) {
                try {
                    Response<AdminAccount> getAccountResponse = getAccountCall.execute();
                    if (getAccountResponse.isSuccessful()) {
                        adminAccount = getAccountResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminAccount finalAdminAccount = adminAccount;
            Runnable myRunnable = () -> adminAccountMutableLiveData.setValue(finalAdminAccount);
            mainHandler.post(myRunnable);
        }).start();
        return adminAccountMutableLiveData;
    }

    /**
     * Perform an action against an account and log this action in the moderation history.
     *
     * @param instance              Instance domain of the active account
     * @param token                 Access token of the active account
     * @param accountId             ID of the account
     * @param type                  Type of action to be taken. Enumerable oneOf: "none" "disable" "silence" "suspend"
     * @param reportId              ID of an associated report that caused this action to be taken
     * @param warningPresetId       ID of a preset warning
     * @param text                  Additional text for clarification of why this action was taken
     * @param sendEmailNotification Whether an email should be sent to the user with the above information.
     */
    public void performAction(@NonNull String instance,
                              String token,
                              @NonNull String accountId,
                              String type,
                              String reportId,
                              String warningPresetId,
                              String text,
                              Boolean sendEmailNotification) {
        MastodonAdminService mastodonAdminService = init(instance);
        new Thread(() -> {
            Call<Void> performActionCall = mastodonAdminService.performAction(token, accountId, type, reportId, warningPresetId, text, sendEmailNotification);
            if (performActionCall != null) {
                try {
                    performActionCall.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Approve the given local account if it is currently pending approval.
     *
     * @param instance  Instance domain of the active account
     * @param token     Access token of the active account
     * @param accountId ID of the account
     * @return {@link LiveData} containing a {@link AdminAccount}
     */
    public LiveData<AdminAccount> approve(@NonNull String instance, String token, @NonNull String accountId) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminAccountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminAccount adminAccount = null;
            Call<AdminAccount> approveCall = mastodonAdminService.approve(token, accountId);
            if (approveCall != null) {
                try {
                    Response<AdminAccount> approveResponse = approveCall.execute();
                    if (approveResponse.isSuccessful()) {
                        adminAccount = approveResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminAccount finalAdminAccount = adminAccount;
            Runnable myRunnable = () -> adminAccountMutableLiveData.setValue(finalAdminAccount);
            mainHandler.post(myRunnable);
        }).start();
        return adminAccountMutableLiveData;
    }

    /**
     * Reject the given local account if it is currently pending approval.
     *
     * @param instance  Instance domain of the active account
     * @param token     Access token of the active account
     * @param accountId ID of the account
     * @return {@link LiveData} containing a {@link AdminAccount}
     */
    public LiveData<AdminAccount> reject(@NonNull String instance, String token, @NonNull String accountId) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminAccountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminAccount adminAccount = null;
            Call<AdminAccount> rejectCall = mastodonAdminService.reject(token, accountId);
            if (rejectCall != null) {
                try {
                    Response<AdminAccount> rejectResponse = rejectCall.execute();
                    if (rejectResponse.isSuccessful()) {
                        adminAccount = rejectResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminAccount finalAdminAccount = adminAccount;
            Runnable myRunnable = () -> adminAccountMutableLiveData.setValue(finalAdminAccount);
            mainHandler.post(myRunnable);
        }).start();
        return adminAccountMutableLiveData;
    }

    /**
     * Re-enable a local account whose login is currently disabled.
     *
     * @param instance  Instance domain of the active account
     * @param token     Access token of the active account
     * @param accountId ID of the account
     * @return {@link LiveData} containing a {@link AdminAccount}
     */
    public LiveData<AdminAccount> enable(@NonNull String instance, String token, @NonNull String accountId) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminAccountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminAccount adminAccount = null;
            Call<AdminAccount> enableCall = mastodonAdminService.enable(token, accountId);
            if (enableCall != null) {
                try {
                    Response<AdminAccount> enableResponse = enableCall.execute();
                    if (enableResponse.isSuccessful()) {
                        adminAccount = enableResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminAccount finalAdminAccount = adminAccount;
            Runnable myRunnable = () -> adminAccountMutableLiveData.setValue(finalAdminAccount);
            mainHandler.post(myRunnable);
        }).start();
        return adminAccountMutableLiveData;
    }

    /**
     * Unsilence a currently silenced account.
     *
     * @param instance  Instance domain of the active account
     * @param token     Access token of the active account
     * @param accountId ID of the account
     * @return {@link LiveData} containing a {@link AdminAccount}
     */
    public LiveData<AdminAccount> unsilence(@NonNull String instance, String token, @NonNull String accountId) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminAccountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminAccount adminAccount = null;
            Call<AdminAccount> unsilenceCall = mastodonAdminService.unsilence(token, accountId);
            if (unsilenceCall != null) {
                try {
                    Response<AdminAccount> unsilenceResponse = unsilenceCall.execute();
                    if (unsilenceResponse.isSuccessful()) {
                        adminAccount = unsilenceResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminAccount finalAdminAccount = adminAccount;
            Runnable myRunnable = () -> adminAccountMutableLiveData.setValue(finalAdminAccount);
            mainHandler.post(myRunnable);
        }).start();
        return adminAccountMutableLiveData;
    }

    /**
     * Unsuspend a currently suspended account.
     *
     * @param instance  Instance domain of the active account
     * @param token     Access token of the active account
     * @param accountId ID of the account
     * @return {@link LiveData} containing a {@link AdminAccount}
     */
    public LiveData<AdminAccount> unsuspend(@NonNull String instance, String token, @NonNull String accountId) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminAccountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminAccount adminAccount = null;
            Call<AdminAccount> unsuspendCall = mastodonAdminService.unsuspend(token, accountId);
            if (unsuspendCall != null) {
                try {
                    Response<AdminAccount> unsuspendResponse = unsuspendCall.execute();
                    if (unsuspendResponse.isSuccessful()) {
                        adminAccount = unsuspendResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminAccount finalAdminAccount = adminAccount;
            Runnable myRunnable = () -> adminAccountMutableLiveData.setValue(finalAdminAccount);
            mainHandler.post(myRunnable);
        }).start();
        return adminAccountMutableLiveData;
    }

    /**
     * View all reports.
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @return {@link LiveData} containing a {@link List} of {@link AdminReport}s
     */
    public LiveData<AdminReports> getReports(@NonNull String instance,
                                             String token,
                                             Boolean resolved,
                                             String accountId,
                                             String targetAccountId,
                                             String max_id) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminReporstListMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            List<AdminReport> adminReportList;
            Call<List<AdminReport>> getReportsCall = mastodonAdminService.getReports(token, resolved, accountId, targetAccountId, max_id, MastodonHelper.statusesPerCall(getApplication()));
            AdminReports adminReports = new AdminReports();
            if (getReportsCall != null) {
                try {
                    Response<List<AdminReport>> getReportsResponse = getReportsCall.execute();
                    if (getReportsResponse.isSuccessful()) {
                        adminReportList = getReportsResponse.body();
                        adminReports.adminReports = adminReportList;
                        adminReports.pagination = MastodonHelper.getPagination(getReportsResponse.headers());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> adminReporstListMutableLiveData.setValue(adminReports);
            mainHandler.post(myRunnable);
        }).start();
        return adminReporstListMutableLiveData;
    }

    /**
     * View information about the report with the given ID.
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @return {@link LiveData} containing a {@link AdminReport}
     */
    public LiveData<AdminReport> getReport(@NonNull String instance, String token, @NonNull String id) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminReportMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminReport adminReport = null;
            Call<AdminReport> getReportCall = mastodonAdminService.getReport(token, id);
            if (getReportCall != null) {
                try {
                    Response<AdminReport> getReportResponse = getReportCall.execute();
                    if (getReportResponse.isSuccessful()) {
                        adminReport = getReportResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminReport finalAdminReportList = adminReport;
            Runnable myRunnable = () -> adminReportMutableLiveData.setValue(finalAdminReportList);
            mainHandler.post(myRunnable);
        }).start();
        return adminReportMutableLiveData;
    }

    /**
     * Claim the handling of this report to yourself.
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @return {@link LiveData} containing a {@link AdminReport}
     */
    public LiveData<AdminReport> assignToSelf(@NonNull String instance, String token, @NonNull String id) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminReportMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminReport adminReport = null;
            Call<AdminReport> assignToSelfCall = mastodonAdminService.assignToSelf(token, id);
            if (assignToSelfCall != null) {
                try {
                    Response<AdminReport> assignToSelfResponse = assignToSelfCall.execute();
                    if (assignToSelfResponse.isSuccessful()) {
                        adminReport = assignToSelfResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminReport finalAdminReportList = adminReport;
            Runnable myRunnable = () -> adminReportMutableLiveData.setValue(finalAdminReportList);
            mainHandler.post(myRunnable);
        }).start();
        return adminReportMutableLiveData;
    }

    /**
     * Unassign a report so that someone else can claim it.
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @return {@link LiveData} containing a {@link AdminReport}
     */
    public LiveData<AdminReport> unassign(@NonNull String instance, String token, @NonNull String id) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminReportMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminReport adminReport = null;
            Call<AdminReport> unassignCall = mastodonAdminService.unassign(token, id);
            if (unassignCall != null) {
                try {
                    Response<AdminReport> unassignResponse = unassignCall.execute();
                    if (unassignResponse.isSuccessful()) {
                        adminReport = unassignResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminReport finalAdminReportList = adminReport;
            Runnable myRunnable = () -> adminReportMutableLiveData.setValue(finalAdminReportList);
            mainHandler.post(myRunnable);
        }).start();
        return adminReportMutableLiveData;
    }

    /**
     * Mark a report as resolved with no further action taken.
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @return {@link LiveData} containing a {@link AdminReport}
     */
    public LiveData<AdminReport> resolved(@NonNull String instance, String token, @NonNull String id) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminReportMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminReport adminReport = null;
            Call<AdminReport> resolvedCall = mastodonAdminService.resolved(token, id);
            if (resolvedCall != null) {
                try {
                    Response<AdminReport> resolvedResponse = resolvedCall.execute();
                    if (resolvedResponse.isSuccessful()) {
                        adminReport = resolvedResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminReport finalAdminReportList = adminReport;
            Runnable myRunnable = () -> adminReportMutableLiveData.setValue(finalAdminReportList);
            mainHandler.post(myRunnable);
        }).start();
        return adminReportMutableLiveData;
    }

    /**
     * Reopen a currently closed report.
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @return {@link LiveData} containing a {@link AdminReport}
     */
    public LiveData<AdminReport> reopen(@NonNull String instance, String token, @NonNull String id) {
        MastodonAdminService mastodonAdminService = init(instance);
        adminReportMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            AdminReport adminReport = null;
            Call<AdminReport> reopenCall = mastodonAdminService.reopen(token, id);
            if (reopenCall != null) {
                try {
                    Response<AdminReport> reopenResponse = reopenCall.execute();
                    if (reopenResponse.isSuccessful()) {
                        adminReport = reopenResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            AdminReport finalAdminReportList = adminReport;
            Runnable myRunnable = () -> adminReportMutableLiveData.setValue(finalAdminReportList);
            mainHandler.post(myRunnable);
        }).start();
        return adminReportMutableLiveData;
    }
}
