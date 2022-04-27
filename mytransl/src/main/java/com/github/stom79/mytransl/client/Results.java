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


import com.github.stom79.mytransl.translate.Translate;

/**
 * Created by @stom79 on 27/11/2017.
 * Handler for the results of the translation
 */

public interface Results {
    void onSuccess(Translate translate);

    void onFail(HttpsConnectionException httpsConnectionException);
}
