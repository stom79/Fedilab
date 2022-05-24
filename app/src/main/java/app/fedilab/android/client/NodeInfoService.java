package app.fedilab.android.client;
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


import app.fedilab.android.client.entities.app.WellKnownNodeinfo;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface NodeInfoService {

    @GET(".well-known/nodeinfo")
    Call<WellKnownNodeinfo> getWellKnownNodeinfoLinks();

    @GET("{nodeInfoPath}")
    Call<WellKnownNodeinfo.NodeInfo> getNodeinfo(
            @Path(value = "nodeInfoPath", encoded = true) String nodeInfoPath
    );

}
