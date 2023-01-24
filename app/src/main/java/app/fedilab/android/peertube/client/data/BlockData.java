package app.fedilab.android.peertube.client.data;
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
        private AccountData.PeertubeAccount blockedAccount;
        @SerializedName("byAccount")
        private AccountData.PeertubeAccount byAccount;
        @SerializedName("createdAt")
        private Date createdAt;

        public AccountData.PeertubeAccount getBlockedAccount() {
            return blockedAccount;
        }

        public void setBlockedAccount(AccountData.PeertubeAccount blockedAccount) {
            this.blockedAccount = blockedAccount;
        }

        public AccountData.PeertubeAccount getByAccount() {
            return byAccount;
        }

        public void setByAccount(AccountData.PeertubeAccount byAccount) {
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
