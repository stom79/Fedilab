package app.fedilab.android.peertube.fragment;
/* Copyright 2023 Thomas Schneider
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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.BlockData;
import app.fedilab.android.peertube.drawer.AccountsListAdapter;
import app.fedilab.android.peertube.viewmodel.AccountsVM;
import es.dmoral.toasty.Toasty;


public class DisplayAccountsFragment extends Fragment implements AccountsListAdapter.AllAccountsRemoved {

    private boolean flag_loading;
    private Context context;
    private AccountsListAdapter accountsListAdapter;
    private String max_id;
    private List<AccountData.PeertubeAccount> accounts;
    private RelativeLayout mainLoader, nextElementLoader, textviewNoAction;
    private boolean firstLoad;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView lv_accounts;
    private View rootView;
    private RetrofitPeertubeAPI.DataType accountFetch;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_recyclerview_peertube, container, false);

        context = getContext();
        Bundle bundle = this.getArguments();
        accounts = new ArrayList<>();
        if (bundle != null) {
            if (bundle.containsKey("accountFetch")) {
                accountFetch = (RetrofitPeertubeAPI.DataType) bundle.getSerializable("accountFetch");
            }
        }
        max_id = null;
        firstLoad = true;
        flag_loading = true;

        swipeRefreshLayout = rootView.findViewById(R.id.swipeContainer);


        lv_accounts = rootView.findViewById(R.id.lv_elements);
        lv_accounts.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        mainLoader = rootView.findViewById(R.id.loader);
        nextElementLoader = rootView.findViewById(R.id.loading_next);
        textviewNoAction = rootView.findViewById(R.id.no_action);
        mainLoader.setVisibility(View.VISIBLE);
        nextElementLoader.setVisibility(View.GONE);
        accountsListAdapter = new AccountsListAdapter(accountFetch, this.accounts);
        accountsListAdapter.allAccountsRemoved = this;
        lv_accounts.setAdapter(accountsListAdapter);
        TextView no_action_text = rootView.findViewById(R.id.no_action_text);
        if (accountFetch == RetrofitPeertubeAPI.DataType.MUTED) {
            no_action_text.setText(context.getString(R.string.no_muted));
        }
        final LinearLayoutManager mLayoutManager;
        mLayoutManager = new LinearLayoutManager(context);
        lv_accounts.setLayoutManager(mLayoutManager);
        lv_accounts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();
                    int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flag_loading) {
                            flag_loading = true;
                            AccountsVM viewModel = new ViewModelProvider(DisplayAccountsFragment.this).get(AccountsVM.class);
                            viewModel.getAccounts(accountFetch, max_id).observe(DisplayAccountsFragment.this.requireActivity(), apiResponse -> manageViewAccounts(apiResponse));
                            nextElementLoader.setVisibility(View.VISIBLE);
                        }
                    } else {
                        nextElementLoader.setVisibility(View.GONE);
                    }
                }
            }
        });
        swipeRefreshLayout.setOnRefreshListener(this::pullToRefresh);
        AccountsVM viewModel = new ViewModelProvider(this).get(AccountsVM.class);
        viewModel.getAccounts(RetrofitPeertubeAPI.DataType.MUTED, max_id).observe(DisplayAccountsFragment.this.requireActivity(), this::manageViewAccounts);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity() != null) {
            View action_button = getActivity().findViewById(R.id.action_button);
            if (action_button != null) {
                action_button.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
    }

    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void scrollToTop() {
        if (lv_accounts != null)
            lv_accounts.setAdapter(accountsListAdapter);
    }

    private void manageViewAccounts(APIResponse apiResponse) {
        mainLoader.setVisibility(View.GONE);
        nextElementLoader.setVisibility(View.GONE);
        if (apiResponse.getError() != null) {
            Toasty.error(context, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            swipeRefreshLayout.setRefreshing(false);
            flag_loading = false;
            return;
        }
        flag_loading = (apiResponse.getMax_id() == null);
        List<AccountData.PeertubeAccount> accounts = apiResponse.getAccounts();
        if (accountFetch == RetrofitPeertubeAPI.DataType.MUTED) {
            accounts = new ArrayList<>();
            List<BlockData.Block> blockList = apiResponse.getMuted();
            for (BlockData.Block block : blockList) {
                accounts.add(block.getBlockedAccount());
            }
        }
        if (max_id == null) {
            max_id = "0";
        }
        if (firstLoad && (accounts == null || accounts.size() == 0))
            textviewNoAction.setVisibility(View.VISIBLE);
        else
            textviewNoAction.setVisibility(View.GONE);
        max_id = String.valueOf(Integer.parseInt(max_id) + 20);
        if (accounts != null && accounts.size() > 0) {
            int previousPosition = this.accounts.size();
            int currentPosition = this.accounts.size();
            this.accounts.addAll(accounts);
            if (previousPosition == 0) {
                accountsListAdapter = new AccountsListAdapter(accountFetch, this.accounts);
                accountsListAdapter.allAccountsRemoved = this;
                lv_accounts.setAdapter(accountsListAdapter);
            } else
                accountsListAdapter.notifyItemRangeChanged(currentPosition, accounts.size());
        }
        swipeRefreshLayout.setRefreshing(false);
        firstLoad = false;
    }

    public void pullToRefresh() {
        max_id = null;
        accounts = new ArrayList<>();
        firstLoad = true;
        flag_loading = true;
        swipeRefreshLayout.setRefreshing(true);
        AccountsVM viewModel = new ViewModelProvider(this).get(AccountsVM.class);
        viewModel.getAccounts(RetrofitPeertubeAPI.DataType.MUTED, null).observe(DisplayAccountsFragment.this.requireActivity(), this::manageViewAccounts);
    }

    @Override
    public void onAllAccountsRemoved() {
        textviewNoAction.setVisibility(View.VISIBLE);
    }


}
