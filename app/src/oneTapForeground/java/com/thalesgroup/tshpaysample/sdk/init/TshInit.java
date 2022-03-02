/*
 * MIT License
 *
 * Copyright (c) 2021 Thales DIS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.thalesgroup.tshpaysample.sdk.init;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperience;
import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperienceSettings;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class TshInit extends TshInitBase {

    //region Defines

    private static final String TAG = TshInit.class.getSimpleName();

    //endregion

    //region AbstractTshInit

    @Override
    public void init(@NonNull final Context context) {
        super.init(context);

        // One Tap Foreground needs to initialize SDK from foreground service.
        AppLoggerHelper.debug(TAG, "SDKLoaderReceiver start service");
        final Intent service = new Intent(context, TshInitService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            AppLoggerHelper.debug(TAG, "SDKLoaderReceiver start foreground Service for OS Android 8 and above");
            //For Android O and above.
            context.startForegroundService(service);
        } else {
            AppLoggerHelper.debug(TAG, "SDKLoaderReceiver start background Service for OS below Android 8");
            context.startService(service);
        }
    }

    //endregion

    //region Public API

    /* Triggered from TshInitService as entry point for SDK init. */
    public void onServiceStartCommand() {
        // Notify UI, that we started with SDK init.
        updateState(TshInitState.INIT_IN_PROGRESS);

        // CPS and MG might be initialised independently, however in order to keep
        // code simpler and not keep track for multiple states, we would do it in series.
        initCpsSdk(new InitSdkCallback() {
            @Override
            public void onSuccess() {
                // Unlike ONE_TAP_ENABLED we do not have to run benchmark for this scenario and can
                // set the value directly.
                PaymentExperienceSettings.setPaymentExperience(mContext, PaymentExperience.ONE_TAP_REQUIRES_SDK_INITIALIZED);

                // Required for one tap variant.
                registerPreFpEntry();

                // Init MG SDK
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    initMgSdk(new InitSdkCallback() {
                        @Override
                        public void onSuccess() {
                            updateState(TshInitState.INIT_SUCCESSFUL);
                        }

                        @Override
                        public void onError(final String error) {
                            updateState(TshInitState.INIT_FAILED, error);
                        }

                    });

                }, INIT_DELAY_MS);
            }

            @Override
            public void onError(final String error) {
                // Informational only. Actual error is handled by updateInitState.
                AppLoggerHelper.error(TAG, "InitSdkCallback#onError(): " + error);
            }
        });
    }

    //endregion

}
