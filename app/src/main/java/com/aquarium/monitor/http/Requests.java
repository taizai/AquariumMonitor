package com.aquarium.monitor.http;

import android.os.AsyncTask;

/**
 * Created by taizai on 2016/01/23.
 */
public class Requests {

    private static Requests instance;
    public static String[] REQUEST_TYPE = {"GET", "POST"};

    public static Requests getInstance() {
        if(instance == null) {
            instance = new Requests();
        }
        return instance;
    }

    class RequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }
}
