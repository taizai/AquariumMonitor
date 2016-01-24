package com.aquarium.monitor;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import android.util.Log;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by taizai on 2016/01/23.
 */
public class MonitorActivity extends ActionBarActivity{

    private final String[] PERIOD_TYPE = {
            "30 mins",
            "1 hour",
            "3 hours",
            "6 hours",
            "12 hours",
            "1 day"};
    private final String[] PERIOD_MINUTES = {
            "30",
            "60",
            "180",
            "360",
            "720",
            "1440"
    };
    private RelativeLayout mPHChart;
    private RelativeLayout mTurbidityChart;
    private RelativeLayout mTempChart;
    private RelativeLayout mDepthChart;
    private ProgressBar mPHProgress;
    private ProgressBar mTurbidityProgress;
    private ProgressBar mTempProgress;
    private ProgressBar mDepthProgress;
    private LineChartView mPHChartView;
    private LineChartView mTurbidityChartView;
    private LineChartView mTempChartView;
    private LineChartView mDepthChartView;

    private class DataValue {
        float pHValue;
        float turbidityValue;
        float tempValue;
        float depthValue;
        int date;
    }

    private List<DataValue> mResultDataSet = null;
    private SharedPreferences mSharedPref;
    private final String PERIOD = "PERIOD";

    private final int UPDATE_PERIOD = 30000;
    private Handler mHandler;
    private Runnable mRefreshData = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPHProgress.setVisibility(View.VISIBLE);
                    mTurbidityProgress.setVisibility(View.VISIBLE);
                    mTempProgress.setVisibility(View.VISIBLE);
                    mDepthProgress.setVisibility(View.VISIBLE);
                    GetData tmp = new GetData();
                    tmp.execute();
                }
            });
//            mHandler.postDelayed(mRefreshData, UPDATE_PERIOD);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor_activity_layout);

        mSharedPref = getSharedPreferences(getApplicationContext().getPackageName(), MODE_PRIVATE);
        getSupportActionBar().setTitle(getPeriodTitle());

        mPHChart = (RelativeLayout) findViewById(R.id.ph_chart_layout);
        mPHProgress = (ProgressBar) findViewById(R.id.ph_chart_progress);
        mTurbidityChart = (RelativeLayout) findViewById(R.id.turbidity_chart_layout);
        mTurbidityProgress = (ProgressBar) findViewById(R.id.turbidity_chart_progress);
        mTempChart = (RelativeLayout) findViewById(R.id.temp_chart_layout);
        mTempProgress = (ProgressBar) findViewById(R.id.temp_chart_progress);
        mDepthChart = (RelativeLayout) findViewById(R.id.depth_chart_layout);
        mDepthProgress = (ProgressBar) findViewById(R.id.depth_chart_progress);

        GetData tmp = new GetData();
        tmp.execute();
//        mHandler = new Handler();
//        mRefreshData.run();
    }

    @Override
    protected void onResume() {
//        GetData tmp = new GetData();
//        tmp.execute();
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        switch (item.getItemId()) {
            case R.id.minutes_30:
                editor.putInt(PERIOD, 0);
                break;
            case R.id.hour_1:
                editor.putInt(PERIOD, 1);
                break;
            case R.id.hour_3:
                editor.putInt(PERIOD, 2);
                break;
            case R.id.hour_6:
                editor.putInt(PERIOD, 3);
                break;
            case R.id.hour_12:
                editor.putInt(PERIOD, 4);
                break;
            case R.id.day_1:
                editor.putInt(PERIOD, 5);
                break;
            default:
                editor.putInt(PERIOD, 0);
                break;
        }
        editor.commit();
        getSupportActionBar().setTitle(getPeriodTitle());
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        return true;
    }

    private void drawChart(RelativeLayout layout) {
        LineChartView chart = new LineChartView(this);
        setChartView(layout, chart);
        chart.setInteractive(true);
        chart.setZoomType(ZoomType.HORIZONTAL);
        chart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        List<PointValue> values = new ArrayList<PointValue>();
        Axis xAxis = new Axis();
        xAxis.setName("Time");
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < mResultDataSet.size(); i++) {
            values.add(new PointValue(i, getValue(mResultDataSet.get(i), layout)));
            if (i % 2 == 0) {
                AxisValue axisValue = new AxisValue(i);
                Date d = new Date(mResultDataSet.get(i).date * 1000);
                calendar.setTime(d);
                if (i == 0 || calendar.get(Calendar.MINUTE) == 0)
                    axisValue.setLabel(calendar.get(Calendar.HOUR_OF_DAY) + ":" + String.format("%2d", calendar.get(Calendar.MINUTE)));
                else
                    axisValue.setLabel(String.format("%2d", calendar.get(Calendar.MINUTE)));
                axisValues.add(axisValue);
            }
        }
        xAxis.setValues(axisValues);
        xAxis.setHasLines(true);

        Line line = new Line(values).setColor(Color.BLUE).setCubic(true);
        List<Line> lines = new ArrayList<Line>();
        lines.add(line);

        LineChartData data = new LineChartData();
        Axis yAxis = new Axis();
        yAxis.setHasLines(true);
        yAxis.setName(getLabel(layout));
        Axis yAxisRight = new Axis();
        yAxisRight.setName("");

        data.setAxisYLeft(yAxis);
        data.setAxisXBottom(xAxis);
        data.setAxisYRight(yAxisRight);

        data.setLines(lines);
        chart.setLineChartData(data);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                600);
        layout.addView(chart, params);
    }

    private class GetData extends AsyncTask<Void, Void, List<DataValue>>{

        @Override
        protected List<DataValue> doInBackground(Void... params) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(getUrl());
            HttpResponse response;
            String result = null;
            List<DataValue> resultDataSet = null;
            try {
                response = httpClient.execute(httpGet);
                result = EntityUtils.toString(response.getEntity());

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (result != null) {
                try {
                    JSONArray dataArray = new JSONArray(result);
                    resultDataSet = new ArrayList<DataValue>();
                    for (int i = 0; i < dataArray.length(); i++) {
                        DataValue dv = new DataValue();
                        JSONObject data = dataArray.optJSONObject(i);
                        String tmp = data.optString("ph");
                        dv.pHValue = (tmp != null && tmp != "null") ? Float.parseFloat(tmp) : -1;
                        tmp = data.optString("temperature");
                        dv.tempValue = (tmp != null && tmp != "null") ? Float.parseFloat(tmp) : -1;
                        tmp = data.optString("turbidity");
                        dv.turbidityValue = (tmp != null && tmp != "null") ? Float.parseFloat(tmp) : -1;
                        tmp = data.optString("level");
                        dv.depthValue = (tmp != null && tmp != "null") ? Float.parseFloat(tmp) : -1;
                        tmp = data.optString("created_at");
                        dv.date = (tmp != null && tmp != "null") ? Integer.parseInt(tmp) : -1;
                        resultDataSet.add(dv);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return resultDataSet;
        }

        @Override
        protected void onPreExecute() {
            mPHChart.removeView(mPHChartView);
            mTurbidityChart.removeView(mTurbidityChartView);
            mTempChart.removeView(mTempChartView);
            mDepthChart.removeView(mDepthChartView);
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(List<DataValue> result) {
            super.onPostExecute(result);
            mResultDataSet = result;

            mPHProgress.setVisibility(View.INVISIBLE);
            mTurbidityProgress.setVisibility(View.INVISIBLE);
            mTempProgress.setVisibility(View.INVISIBLE);
            mDepthProgress.setVisibility(View.INVISIBLE);

            drawChart(mPHChart);
            drawChart(mTurbidityChart);
            drawChart(mTempChart);
            drawChart(mDepthChart);
        }
    }

    private float getValue(DataValue dv, RelativeLayout rl) {
        if (rl == mPHChart)
            return dv.pHValue;
        else if (rl == mTurbidityChart)
            return dv.turbidityValue;
        else if (rl == mTempChart)
            return dv.tempValue;
        else if (rl == mDepthChart)
            return dv.depthValue;
        return -1;
    }

    private String getLabel(RelativeLayout rl) {
        if (rl == mPHChart)
            return "pH Value";
        else if (rl == mTurbidityChart)
            return "Turbidity Value";
        else if (rl == mTempChart)
            return "Temperature Value";
        else if (rl == mDepthChart)
            return "Depth Value";
        return "";
    }

    private String getUrl() {
        int type = mSharedPref.getInt(PERIOD, 0);
        String url = "http://106.187.44.189:3000/devises/test001/logs?period=" + PERIOD_MINUTES[type];
        return url;
    }

    private String getPeriodTitle() {
        int type = mSharedPref.getInt(PERIOD, 0);
        return PERIOD_TYPE[type];
    }

    private void setChartView(RelativeLayout rl, LineChartView lcv) {
        if (rl == mPHChart)
            mPHChartView = lcv;
        else if (rl == mTurbidityChart)
            mTurbidityChartView = lcv;
        else if (rl == mTempChart)
            mTempChartView = lcv;
        else if (rl == mDepthChart)
            mDepthChartView = lcv;
    }
}
