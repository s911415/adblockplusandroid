/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.adblockplus.android.core.callback;

import android.content.Context;
import org.adblockplus.android.Utils;
import org.adblockplus.libadblockplus.FilterChangeCallback;
import org.adblockplus.libadblockplus.JsValue;

public class AndroidFilterChangeCallback extends FilterChangeCallback {
    private final Context context;

    public AndroidFilterChangeCallback(final Context context) {
        this.context = context;
    }

    @Override
    public void filterChangeCallback(final String action, final JsValue jsValue) {
        if (action.equals("subscription.lastDownload") || action.equals("subscription.downloadStatus")) {
            JsValue url = jsValue.getProperty("url");
            try {
                Utils.updateSubscriptionStatus(this.context, url.asString());
            } finally {
                url.dispose();
            }
        }
    }
}