/*
 * Copyright 2016-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.sample.lex;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;
import com.amazonaws.util.StringUtils;

import java.util.Locale;
import java.util.Map;

public class InteractiveVoiceActivity extends Activity
        implements InteractiveVoiceView.InteractiveVoiceListener {
    private static final String TAG = "VoiceActivity";
    private Context appContext;
    private InteractiveVoiceView voiceView;
    private TextView transcriptTextView;
    private TextView responseTextView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_voice);
        transcriptTextView = findViewById(R.id.transcriptTextView);
        responseTextView = findViewById(R.id.responseTextView);
        init();
        StringUtils.isBlank("notempty");
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    private void init() {
        appContext = getApplicationContext();
        voiceView = findViewById(R.id.voiceInterface);
        voiceView.setInteractiveVoiceListener(this);
        AWSMobileClient.getInstance().initialize(this, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                Log.d(TAG, "onResult: ");
                voiceView.getViewAdapter().setCredentialProvider(AWSMobileClient.getInstance());

                String identityId = AWSMobileClient.getInstance().getIdentityId();
                InteractionConfig lexInteractionConfig = new InteractionConfig(
                        appContext.getString(R.string.bot_name),
                        appContext.getString(R.string.bot_alias),
                        identityId);
                voiceView.getViewAdapter().setInteractionConfig(lexInteractionConfig);

                voiceView.getViewAdapter().setAwsRegion(appContext.getString(R.string.lex_region));
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "onError: ", e);
            }
        });
    }

    private void exit() {
        finish();
    }

    @Override
    public void dialogReadyForFulfillment(final Map<String, String> slots, final String intent) {
        Log.d(TAG, String.format(
                Locale.US,
                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                intent,
                slots.toString()));
    }

    @Override
    public void onResponse(Response response) {
        Log.d(TAG, "Bot response: " + response.getTextResponse());
        Log.d(TAG, "Transcript: " + response.getInputTranscript());

        responseTextView.setText(response.getTextResponse());
        transcriptTextView.setText(response.getInputTranscript());
    }

    @Override
    public void onError(final String responseText, final Exception e) {
        Log.e(TAG, "Error: " + responseText, e);
    }
}
