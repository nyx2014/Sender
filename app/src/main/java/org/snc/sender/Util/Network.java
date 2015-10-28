package org.snc.sender.Util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by nyx2015 on 2015/10/28.
 */
public class Network {

    private static URL url;
    private static int timeout;

    public Network(String url){
        new Network(url,3000);
    }

    Network(String url, int timeout){
        this.timeout = timeout;
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public interface NetworkActCalbk{
//        void onSuccess(String result);
        void onNoNetwork();
        void onTimeout();
        void onError();
    }

    public static String executeHttpPost(String params,NetworkActCalbk cbk) {
        String result = null;
        HttpURLConnection connection = null;
        InputStreamReader in = null;
        try {
            connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Charset", "utf-8");

            DataOutputStream dop = new DataOutputStream(
                    connection.getOutputStream());

            dop.writeBytes(URLEncoder.encode( params, "utf-8"));
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
//            cbk.onSuccess(result);
        } catch (ConnectException ce) {
//            result = "Network is down.";
            cbk.onNoNetwork();
            ce.printStackTrace();
        } catch (FileNotFoundException fe) {
            cbk.onError();
//            result = "Server URL is incorrect.";
            fe.printStackTrace();
        } catch (SocketTimeoutException se){
            cbk.onTimeout();
//            result = "Time out. Try again.";
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            cbk.onError();
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
