package app.fedilab.android.peertube.client.data;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class BlockData {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<Block> data;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<Block> getData() {
        return data;
    }

    public void setData(List<Block> data) {
        this.data = data;
    }

    @SuppressWarnings("unused")
    public static class Block {
        @SerializedName("blockedAccount")
        private AccountData.Account blockedAccount;
        @SerializedName("byAccount")
        private AccountData.Account byAccount;
        @SerializedName("createdAt")
        private Date createdAt;

        public AccountData.Account getBlockedAccount() {
            return blockedAccount;
        }

        public void setBlockedAccount(AccountData.Account blockedAccount) {
            this.blockedAccount = blockedAccount;
        }

        public AccountData.Account getByAccount() {
            return byAccount;
        }

        public void setByAccount(AccountData.Account byAccount) {
            this.byAccount = byAccount;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }


}
