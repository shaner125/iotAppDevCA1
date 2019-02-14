/**
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.demo.androidpubsubwebsocket;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class PubSubActivity extends Activity implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_IOT_ENDPOINT = "a2a4apg8zaw7mm-ats.iot.eu-west-1.amazonaws.com";
    String topic;
    Switch lightSwitch;
    Switch soundSwitch;
    Switch ultraSwitch;
    Switch rotarySwitch;
    Spinner lightSpinner;
    Spinner soundSpinner;
    Spinner ultraSpinner;
    Spinner rotarySpinner;
    TextView lightData;
    TextView soundData;
    TextView ultraData;
    TextView rotaryData;

    TextView tvLastMessage;
    TextView tvClientId;
    TextView tvStatus;

    Button btnConnect;

    AWSIotMqttManager mqttManager;
    String clientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lightSwitch = (Switch) findViewById(R.id.lightSwitch);
        soundSwitch = (Switch) findViewById(R.id.soundSwitch);
        ultraSwitch = (Switch) findViewById(R.id.ultraSwitch);
        rotarySwitch = (Switch) findViewById(R.id.rotarySwitch);

        // Spinner element
        lightSpinner = (Spinner) findViewById(R.id.lightSpinner);
        soundSpinner = (Spinner) findViewById(R.id.soundSpinner);
        ultraSpinner = (Spinner) findViewById(R.id.ultraSpinner);
        rotarySpinner = (Spinner) findViewById(R.id.rotarySpinner);

        // Spinner click listener
        lightSpinner.setOnItemSelectedListener(this);
        soundSpinner.setOnItemSelectedListener(this);
        ultraSpinner.setOnItemSelectedListener(this);
        rotarySpinner.setOnItemSelectedListener(this);

        // Spinner Drop down elements
        List<String> categories = new ArrayList<String>();
        categories.add("5");
        categories.add("10");
        categories.add("15");
        categories.add("20");
        categories.add("25");
        categories.add("30");

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        lightSpinner.setAdapter(dataAdapter);
        soundSpinner.setAdapter(dataAdapter);
        ultraSpinner.setAdapter(dataAdapter);
        rotarySpinner.setAdapter(dataAdapter);

        topic = "$aws/things/ShanePi/shadow/update";

        lightData = findViewById(R.id.light_value);
        soundData = findViewById(R.id.sound_value);
        ultraData = findViewById(R.id.ultra_value);
        rotaryData = findViewById(R.id.rotary_value);

        tvLastMessage = findViewById(R.id.tvLastMessage);
        tvClientId = findViewById(R.id.tvClientId);
        tvStatus = findViewById(R.id.tvStatus);

        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setEnabled(true);

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        tvClientId.setText(clientId);

        // Initialize the credentials provider
        final CountDownLatch latch = new CountDownLatch(1);
        AWSMobileClient.getInstance().initialize(
                getApplicationContext(),
                new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        latch.countDown();
                        Log.e(LOG_TAG, "onError: ", e);
                    }
                }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_IOT_ENDPOINT);

        // Enable button once all clients are ready
        try {
            mqttManager.connect(AWSMobileClient.getInstance(), new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));
                    if(String.valueOf(status).equals("Connected")){
                        subscribe();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText(status.toString());
                            if (throwable != null) {
                                Log.e(LOG_TAG, "Connection error.", throwable);
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
            tvStatus.setText("Error! " + e.getMessage());
        }

        lightSwitch.setOnClickListener(this);
        soundSwitch.setOnClickListener(this);
        ultraSwitch.setOnClickListener(this);
        rotarySwitch.setOnClickListener(this);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onClick(View v) {
        // define the button that invoked the listener by id
        String statusSwitch = new String();
        switch (v.getId()) {
            case R.id.lightSwitch:
                if (lightSwitch.isChecked()) {
                    statusSwitch = "{\"state\":{\"desired\":{\"lightStatus\":1}}}";
                    lightData.setText("Waiting...");
                    lightData.setTextColor(Color.parseColor("#669900"));
                }
                else {
                    statusSwitch = "{\"state\":{\"desired\":{\"lightStatus\":0}}}";
                    lightData.setText("OFF");
                    lightData.setTextColor(Color.RED);
                }
                break;
            case R.id.soundSwitch:
                if (soundSwitch.isChecked()) {
                    statusSwitch = "{\"state\":{\"desired\":{\"soundStatus\":1}}}";
                    soundData.setText("Waiting...");
                    soundData.setTextColor(Color.parseColor("#669900"));
                }
                else {
                    statusSwitch = "{\"state\":{\"desired\":{\"soundStatus\":0}}}";
                    soundData.setText("OFF");
                    soundData.setTextColor(Color.RED);
                }
                break;
            case R.id.ultraSwitch:
                if (ultraSwitch.isChecked()) {
                    statusSwitch = "{\"state\":{\"desired\":{\"ultraStatus\":1}}}";
                    ultraData.setText("Waiting...");
                    ultraData.setTextColor(Color.parseColor("#669900"));
                }
                else {
                    statusSwitch = "{\"state\":{\"desired\":{\"ultraStatus\":0}}}";
                    ultraData.setText("OFF");
                    ultraData.setTextColor(Color.RED);
                }
                break;
            case R.id.rotarySwitch:
                if (rotarySwitch.isChecked()) {
                    statusSwitch = "{\"state\":{\"desired\":{\"rotaryStatus\":1}}}";
                    rotaryData.setText("Waiting...");
                    rotaryData.setTextColor(Color.parseColor("#669900"));
                }
                else {
                    statusSwitch = "{\"state\":{\"desired\":{\"rotaryStatus\":0}}}";
                    rotaryData.setText("OFF");
                    rotaryData.setTextColor(Color.RED);
                }
                break;
        }
        try {
            mqttManager.publishString(statusSwitch, topic, AWSIotMqttQos.QOS0);
            }
            catch (Exception e) {
                    Log.e(LOG_TAG, "Publish error.", e);
            }
    }

    public void connect(final View view) {
        Log.d(LOG_TAG, "clientId = " + clientId);
        try {
            mqttManager.connect(AWSMobileClient.getInstance(), new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));
                    if(String.valueOf(status).equals("Connected")){
                        subscribe();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText(status.toString());
                            if (throwable != null) {
                                Log.e(LOG_TAG, "Connection error.", throwable);
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
            tvStatus.setText("Error! " + e.getMessage());
        }
    }

    public void subscribe() {

        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String message = new String(data, "UTF-8");
                                        Log.d(LOG_TAG, "Message arrived:");
                                        Log.d(LOG_TAG, "   Topic: " + topic);
                                        Log.d(LOG_TAG, " Message: " + message);
                                        try {
                                            JSONObject obj = new JSONObject(message);
                                            if (message.contains("lightValue") && lightSwitch.isChecked()){
                                                lightData.setText(obj.getJSONObject("state").getJSONObject("reported").getString("lightValue"));
                                            }
                                            else if (message.contains("soundValue") && soundSwitch.isChecked()){
                                                soundData.setText(obj.getJSONObject("state").getJSONObject("reported").getString("soundValue"));
                                            }
                                            else if (message.contains("ultraValue") && ultraSwitch.isChecked()){
                                                ultraData.setText(obj.getJSONObject("state").getJSONObject("reported").getString("ultraValue"));
                                            }
                                            else if (message.contains("rotaryValue") && rotarySwitch.isChecked()){
                                                rotaryData.setText(obj.getJSONObject("state").getJSONObject("reported").getString("rotaryValue"));
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        tvLastMessage.setText(message);

                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(LOG_TAG, "Message encoding error.", e);
                                    }
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    }

    public void disconnect(final View view) {
        try {
            mqttManager.disconnect();
            btnConnect.setEnabled(true);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();
        int uid = parent.getId();
        String msg = new String();
        switch(uid){
            case R.id.lightSpinner:
                msg = "{\"state\":{\"desired\":{\"lightDelta\": "+item+"}}}";
                break;
            case R.id.soundSpinner:
                msg = "{\"state\":{\"desired\":{\"soundDelta\": "+item+"}}}";
                break;
            case R.id.ultraSpinner:
                msg = "{\"state\":{\"desired\":{\"ultraDelta\": "+item+"}}}";
                break;
            case R.id.rotarySpinner:
                msg = "{\"state\":{\"desired\":{\"rotaryDelta\": "+item+"}}}";
                break;
        }
    //
        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
