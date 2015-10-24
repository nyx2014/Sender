package org.snc.sender;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class SenderActivity extends Activity {

    private static String url;

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

        url = mPreferences.getString("lSvrList","http://202.4.136.143/api/test.php");
        url = url.equals("custom") ? mPreferences.getString("sURL","http://202.4.136.143/api/test.php") : url;

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
            int seconds = 3;
            connection.setConnectTimeout(seconds * 1000);
            connection.setReadTimeout(seconds * 1000);

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
