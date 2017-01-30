package com.example.yuao.a571stocksearch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by yuao on 4/21/16.
 */
public class httpGetDataAndProcess extends AsyncTask<String, Void, String> {
    String mode;
    String symbol;
    int num;
    Activity callingActivity;
    httpGetDataAndProcess(Activity callingActivity) {
        this.callingActivity = callingActivity;
    }
    @Override
    protected String doInBackground(String[] para) {
        String s = para[0];
        symbol = s;
        mode = para[1];
        if(mode == "symbols")
            num = Integer.parseInt(para[2]);
        return callPHP(s, mode);
    }

    protected void onPostExecute (String result) {
        if(mode == "details") {
            try {
                JSONObject jsonObj = new JSONObject(result);
                String message = "";
                if(jsonObj.has("Message") || !jsonObj.getString("Status").equals("SUCCESS")) {
                    if(jsonObj.has("Message"))  //http://my-project571.appspot.com/?symbol=Grngrn
                        message = jsonObj.getString("Message");
                    else  // http://my-project571.appspot.com/?symbol=GRNREG
                        message = "No information for this stock";
                    DialogFragment newFragment = new MyAlertDialog();
                    Bundle args = new Bundle();
                    args.putString(callingActivity.getResources().getString(R.string.errormsg_key), message);
                    newFragment.setArguments(args);
                    newFragment.show(((MainActivity)callingActivity).getSupportFragmentManager(), "error_alert");
                } else {
                    Intent intent = new Intent(((MainActivity)callingActivity), ResultActivity.class);
                    intent.putExtra(((MainActivity)callingActivity).EXTRA_MESSAGE, result);
                    ((MainActivity)callingActivity).startActivity(intent);
                }
            } catch (Exception e) {
                Log.e("log_tag", "Error in parse details " + e.toString());
            }
        } else if(mode == "symbols") {
            try {
                JSONArray jsonArray = new JSONArray(result);
                FavBean[] favBean = new FavBean[num];
                if(num != jsonArray.length()) {
                    System.out.println("Fatal Error!");
                    System.exit(1);
                }
                for(int i=0; i<jsonArray.length(); i++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);
                    favBean[i] = new FavBean();
                    favBean[i].Name = jsonObj.getString("Name");
                    favBean[i].Symbol = jsonObj.getString("Symbol");
                    favBean[i].LastPrice = "$ " + String.format("%.2f", jsonObj.getDouble("LastPrice"));
                    if(jsonObj.getDouble("ChangePercent") > 0)
                        favBean[i].ChangePercent = " +" + String.format("%.2f", jsonObj.getDouble("ChangePercent")) + "% ";
                    else
                        favBean[i].ChangePercent = " " + String.format("%.2f", jsonObj.getDouble("ChangePercent")) + "% ";
                    double temp;
                    temp = jsonObj.getDouble("MarketCap") / 1000000000;
                    if (temp < 0.005) {
                        temp *= 1000;
                        favBean[i].mcUnite = "Million";
                    } else
                        favBean[i].mcUnite = "Billion";
                    favBean[i].MarketCap = String.format("%.2f", temp);
                }
                ((MainActivity)callingActivity).addFavListView(favBean);
            } catch (Exception e) {
                Log.e("log_tag", "Error in parse symbols " + e.toString());
            }
        } else if(mode == "feeds") {
            ResultActivity activity = (ResultActivity) callingActivity;
            activity.feed = (FeedBean[]) activity.parseJson(result, "feeds");
            new httpGetImage(activity).execute(symbol);
        }
    }

    private String callPHP(String para, String mode) {
        String result = "";
        String urlstr = "";
        if (mode == "details")
            urlstr = "http://my-project571.appspot.com/?symbol=" + para;
        else if (mode == "symbols")
            urlstr = "http://my-project571.appspot.com/?num=" + num + "&symbols=" + para;
        else if (mode == "feeds")
            urlstr = "http://my-project571.appspot.com/?symbol=" + para + "&feed=yes";
        System.out.println(urlstr);
        try {
            URL url = new URL(urlstr);
            //BufferedReader webPage = new BufferedReader(new InputStreamReader(url.openStream()));
            URLConnection myURLConnection = url.openConnection();
            BufferedReader webPage = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
            result = webPage.readLine();
            System.out.println("result is :" + result);
            webPage.close();
        } catch (Exception e) {
            Log.e("log_tag", "Error in http connection " + e.toString());
        }
        return result;
    }
}

class httpGetImage extends AsyncTask<String, Void, Drawable> {
    String symbol;
    Activity callingActivity;
    httpGetImage(Activity callingActivity) {
        this.callingActivity = callingActivity;
    }
    @Override
    protected Drawable doInBackground(String[] para) {
        String s = para[0];
        symbol = s;
        Drawable d = null;
        try {
            URLConnection myURLConnection = (new URL("http://chart.finance.yahoo.com/t?s=" + symbol + "&lang=en-US&width=200&height=200")).openConnection();
            d = Drawable.createFromStream(myURLConnection.getInputStream(), "yahoo chart");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(d==null)
            System.out.println("We are getting null image");
        return d;
    }

    protected void onPostExecute (Drawable result) {
        ResultActivity activity = (ResultActivity) callingActivity;
        activity.imageDrawable = result;

        activity.initView();
        activity.mTabLayout.addTab(activity.mTabLayout.newTab().setText("Current".toUpperCase()));
        activity.mTabLayout.addTab(activity.mTabLayout.newTab().setText("Historical".toUpperCase()));
        activity.mTabLayout.addTab(activity.mTabLayout.newTab().setText("News".toUpperCase()));

    }
}