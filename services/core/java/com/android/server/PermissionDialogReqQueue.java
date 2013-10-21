/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server;

import android.app.ActivityThread;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class PermissionDialogReqQueue {
    public final static class PermissionDialogReq {
        boolean mHasResult = false;
        int mResult;
        final AppOpsService.Op mOp;

        public PermissionDialogReq(final AppOpsService.Op op) {
            mOp = op;
        }

        public void set(int res) {
            synchronized (this) {
                mHasResult = true;
                mResult = res;
                notifyAll();
            }
        }

        public int get() {
            synchronized (this) {
                while (!mHasResult) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return mResult;
        }

    }

    private PermissionDialog mDialog = null;
    private List<PermissionDialogReq> resultList = new ArrayList<>();

    public void register(AppOpsService service, PermissionDialogReq req) {
        synchronized (this) {
            resultList.add(req);
        }
        if (mDialog == null) {
            final Context context = ActivityThread.currentActivityThread().getSystemUiContext();
            mDialog = new PermissionDialog(context, service,
                    req.mOp.op, req.mOp.uid, req.mOp.packageName);
        }
    }

    public void showDialog() {
        if (mDialog != null) {
            mDialog.show();
        }
    }

    public void dismissAndNotify(int mode) {
        if (mDialog == null) {
            return;
        }
        synchronized (this) {
            while (resultList.size() != 0) {
                PermissionDialogReq res = resultList.get(0);
                res.set(mode);
                resultList.remove(0);
            }
        }
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mDialog = null;
    }

    public void ignore() {
        if (mDialog != null) {
            mDialog.ignore();
        }
    }
}
