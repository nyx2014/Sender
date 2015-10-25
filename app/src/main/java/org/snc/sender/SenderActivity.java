package org.snc.sender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;

public class SenderActivity extends Activity {

    private static String url;
    private static int timeout;

    private SharedPreferences mPreferences;
    private static String sharedText;
    private static Handler handler;

    public SenderActivity() {
        handler = new Handler(){
            public void handleMessage(Message msg)
            {
                Toast.makeText(SenderActivity.this,msg.obj+"",Toast.LENGTH_SHORT).show();
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(700);
                            SenderActivity.this.finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        };
    }

    private interface DAction{
        void run();
    }
    private void DelayedAction(final DAction action, final int delay){
        new Thread(){
            @Override
            public void run() {
//                Looper.prepare();
                try {
//                    Looper.loop();
                    Thread.sleep(delay);
                    action.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(mPreferences==null){
            mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        }

        String lTimeout = mPreferences.getString("lTimeout", "3000");
        timeout = Integer.parseInt(lTimeout);

        String defaultSvrURL = getString(R.string.pref_default_svr_URL);

        url = mPreferences.getString("lSvrList",defaultSvrURL);
        url = url.equals("custom") ? mPreferences.getString("sURL",defaultSvrURL) : url;

        ConnectivityManager connectMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectMgr.getActiveNetworkInfo();

        if (info!=null) {
            if(info.getType() == ConnectivityManager.TYPE_WIFI){
//                Toast.makeText(this,"wifi!",Toast.LENGTH_SHORT).show();
            }else {
                url = getResources().getStringArray(R.array.svr_urls)[2];
//                Toast.makeText(this,"mobile"+url,Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(this,"Network is down.",Toast.LENGTH_LONG).show();
            finish();
        }

        sharedText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        new Thread(){
            @Override
            public void run() {
                Message msg = new Message();
                msg.obj = executeHttpPost(sharedText);
                handler.sendMessage(msg);
            }
        }.start();
//        DelayedAction(new DAction() {
//            @Override
//            public void run() {
//                Toast.makeText(SenderActivity.this, "Server Unreachable.", Toast.LENGTH_SHORT).show();
//                DelayedAction(new DAction() {
//                    @Override
//                    public void run() {
//                        SenderActivity.this.finish();
//                    }
//                }, 700);
//            }
//        }, 3100);
    }

    private String executeHttpPost(String msg) {
        String result = null;
        URL _url;
        HttpURLConnection connection = null;
        InputStreamReader in = null;
        try {
            _url = new URL(url);
            connection = (HttpURLConnection) _url.openConnection();

            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Charset", "utf-8");

            DataOutputStream dop = new DataOutputStream(
                    connection.getOutputStream());
            msg = URLEncoder.encode(msg, "utf-8");
            dop.writeBytes("m=" + msg);
            dop.flush();
            dop.close();

            in = new InputStreamReader(connection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(in);
            StringBuilder strBuffer = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                strBuffer.append(line);
            }
            result = strBuffer.toString();

//            result += connection.getResponseCode();
        } catch (ConnectException ce) {
            result = "Network is down.";
            ce.printStackTrace();
        } catch (FileNotFoundException fe) {
            result = "Server URL is incorrect.";

            Intent mIntent = new Intent(SenderActivity.this,SettingsActivity.class);
            SenderActivity.this.startActivity(mIntent);
//            SenderActivity.this.finish();
            
            fe.printStackTrace();
        } catch (SocketTimeoutException se){
            result = "Time out. Try again.";
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return result;
    }
}
