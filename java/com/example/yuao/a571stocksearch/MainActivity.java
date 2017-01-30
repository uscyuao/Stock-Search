package com.example.yuao.a571stocksearch;

import java.io.*;
import java.net.*;
import java.nio.CharBuffer;
import java.util.*;
import java.util.concurrent.ThreadFactory;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.*;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.ActionBar;
import android.support.v4.app.*;
import android.os.Bundle;
import android.support.v7.view.menu.ActionMenuItemView;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;
import android.text.*;
import android.util.*;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.app.Dialog;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;

import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;

public class MainActivity extends AppCompatActivity {
    String companySymbol = null;
    public final static String EXTRA_MESSAGE = "com.example.yuao.a571stocksearch.MESSAGE";
    SharedPreferences settings;
    AutoCompleteTextView textView;
    String symbolList = "";
    private DynamicListView feedList;
    private ArrayAdapter<FavBean> mAdapter;
    int mProgressStatus = 0;
    android.os.Handler mHandler = new android.os.Handler();
    ProgressBar mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.home_icon);
        //actionBar.setLogo(R.drawable.home_icon);
        actionBar.setTitle(R.string.bar_title);
        actionBar.setDisplayUseLogoEnabled(true);

//        mProgress = (ProgressBar) findViewById(R.id.progress_bar);
//        mProgress.setVisibility(View.GONE);
        settings = getSharedPreferences(getString(R.string.app_name), MODE_WORLD_READABLE);
        int compNum = settings.getInt("companyNum", 0);
        for(int i=1; i<compNum; i++) {
            symbolList += settings.getString("symbol"+i, "") + ' ';
        }
        symbolList += settings.getString("symbol"+compNum, "");
        System.out.println("symbol list is: " + symbolList);
        //if(symbolList != null)
            new httpGetDataAndProcess(this).execute(symbolList, "symbols", "" + compNum);

        MyAutocompleteAdapter adapter = new MyAutocompleteAdapter (this,
                android.R.layout.simple_dropdown_item_1line);
        textView = (AutoCompleteTextView) findViewById(R.id.ac_text_view);
        textView.setAdapter(adapter);
        textView.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) {
                        Object item = parent.getItemAtPosition(position);
                        char[] compInfo = ((String) item).toCharArray();
                        int i = 0;
                        for (; i<compInfo.length; i++) {
                            if(compInfo[i] == '-')
                                break;
                        }
                        companySymbol = new String(compInfo, 0, i);
                        textView.setText(companySymbol, false);
                    }
                });
/*        final TextWatcher textChecker = new TextWatcher() {
            public void afterTextChanged(Editable s) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if(s.toString().length() >= 3 && start != 0)  // this can resolve the problem of selection invocation
                    new httpGetCompanyList().execute(s.toString());
            }
        };
        textView.addTextChangedListener(textChecker);*/

        Button quoteButton = (Button)
                findViewById(R.id.get_quote_button);
        Switch refresh = (Switch)
                findViewById(R.id.refresh_toggle);

    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("main starts");
        int compNum = settings.getInt("companyNum", 0);
        System.out.println("in onresume the company number is:" + compNum);
        symbolList = "";
        for(int i=1; i<compNum; i++) {
            symbolList += settings.getString("symbol"+i, "") + "%20";
        }
        symbolList += settings.getString("symbol"+compNum, "");
        System.out.println("symbol list is: " + symbolList);
        new httpGetDataAndProcess(this).execute(symbolList, "symbols", "" + compNum);
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("main pauses");
    }
    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("main Stops");
    }
    private static class ItemFavCache {
        public TextView symbol;
        public TextView name;
        public TextView price;
        public TextView change;
        public TextView marketcap;
    }

    public void removeFromFavList(int position) {
        System.out.println("remove the "+position+"item");
        mAdapter.remove(mAdapter.getItem(position));
        int compNum = settings.getInt("companyNum", 0);
        if(compNum==0) {
            System.out.println("very big error");
            System.exit(0);
        }
        SharedPreferences.Editor editor = settings.edit();
        int i=1;
        for(; i<=compNum; i++) {
            if(i == position+1) {
                break;
            }
        }
        for(; i<compNum; i++) {
            editor.putString("symbol"+i, settings.getString("symbol"+(i+1), null));
        }
        compNum--;
        editor.putInt("companyNum", compNum);
        // Commit the edits!
        editor.commit();
    }
    public void addFavListView(FavBean[] favlist) {
        feedList = (DynamicListView) findViewById(R.id.fav_div);
        mAdapter = new ArrayAdapter<FavBean>(this, R.layout.listitem_layout, new ArrayList<FavBean>(Arrays.asList(favlist))) {

            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView==null){
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.favitem_layout, null);
                    ItemFavCache viewCache = new ItemFavCache();
                    viewCache.symbol = (TextView) convertView.findViewById(R.id.fav_symbol);
                    viewCache.name = (TextView) convertView.findViewById(R.id.fav_name);
                    viewCache.price = (TextView) convertView.findViewById(R.id.fav_price);
                    viewCache.change = (TextView) convertView.findViewById(R.id.fav_change_percent);
                    viewCache.marketcap = (TextView) convertView.findViewById(R.id.fav_market_cap);
                    convertView.setTag(viewCache);
                }
                ItemFavCache cache = (ItemFavCache) convertView.getTag();
                cache.symbol.setText(getItem(position).Symbol);
                cache.name.setText(getItem(position).Name);
                cache.price.setText(getItem(position).LastPrice);
                if(getItem(position).ChangePercent.charAt(0) == '-')
                    cache.change.setBackgroundColor(Color.RED);
                else
                    cache.change.setBackgroundColor(Color.GREEN);
                cache.change.setText(getItem(position).ChangePercent);
                String temp = "Market Cap : " + getItem(position).MarketCap + " " + getItem(position).mcUnite;
                cache.marketcap.setText(temp);
                return convertView;
            }
        };
        mAdapter.setNotifyOnChange(true);
        AlphaInAnimationAdapter animationAdapter = new AlphaInAnimationAdapter(mAdapter);
        animationAdapter.setAbsListView(feedList);
        feedList.setAdapter(animationAdapter);

        feedList.enableSwipeToDismiss(
                new OnDismissCallback() {
                    @Override
                    public void onDismiss(@NonNull final ViewGroup listView, @NonNull final int[] reverseSortedPositions) {
                        for (final int position : reverseSortedPositions) {
                            DialogFragment newFragment = new MyAlertDialog();
                            Bundle args = new Bundle();
                            args.putString("name", ((FavBean)feedList.getAdapter().getItem(position)).Name);
                            args.putString("position", ""+position);
                            newFragment.setArguments(args);
                            newFragment.show(getSupportFragmentManager(), "delete_alert");
                        }

                    }
                }
        );
    }

    public void quoteButtonClicked(View view) {
        if(textView.getText().length() == 0) {
            System.out.println(textView.getText()+"aaa");
            DialogFragment newFragment = new MyAlertDialog();
            Bundle args = new Bundle();
            args.putString(getResources().getString(R.string.errormsg_key), getResources().getString(R.string.error_no));
            newFragment.setArguments(args);
            newFragment.show(getSupportFragmentManager(), "error_alert");
        } else {
            String symbol = textView.getText().toString();
            System.out.println(symbol);
            new httpGetDataAndProcess(this).execute(symbol, "details");
        }

    }
    public void clearButtonClicked(View view) {
        textView.setText("");
    }
    public void refreshButtonClicked(View view) {
        //LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.progress_layout, null);
//        mProgress.setVisibility(View.VISIBLE);
//        runOnUiThread(new Runnable() {
//            public void run() {
//                mProgress.setVisibility(View.VISIBLE);
//                while (mProgressStatus < 100) {
//                    //mProgressStatus = doWork();
//                    mProgressStatus += 1;
//                    // Update the progress bar
//                    try {
//                        Thread.sleep(200);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
////                    mHandler.post(new Runnable() {
////                        public void run() {
////                            mProgress.setProgress(mProgressStatus);
////                        }
////                    });
//                }
//                //dismissBar();
//                mProgress.setVisibility(View.GONE);
//            }
//        });
    }
    private void dismissBar() {
        mProgress.setVisibility(View.GONE);
    }
}



class MyAutocompleteAdapter extends ArrayAdapter<String> {
    private final String MY_DEBUG_TAG = "CustomerAdapter";
    private ArrayList<String> suggestions;
    private int viewResourceId;

    public MyAutocompleteAdapter(Context context, int viewResourceId) {
        super(context, viewResourceId);
        this.suggestions = new ArrayList<String>();
        this.viewResourceId = viewResourceId;
    }

    public Filter getFilter() {
        return nameFilter;
    }

    Filter nameFilter = new Filter() {
        @Override
        public String convertResultToString(Object resultValue) {
            String str = ((String) (resultValue));
            return str;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                suggestions.clear();
                new httpGetCompanyList().execute(constraint.toString());
                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<String> filteredList = (ArrayList<String>) results.values;
            if (results != null && results.count > 0) {
                clear();
                for (String c : filteredList) {
                    add(c);
                }
                notifyDataSetChanged();
            }
        }
    };

    private class httpGetCompanyList extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String[] para) {
            String s = para[0];
            return parseJson(callPHP(s));
        }

        protected void onPostExecute (String[] result) {
            clear();
            if(result != null)
                for (String c : result) {
                    add(c);
                }
        }

        private String callPHP(String para) {
            String result = "";
            System.out.println(para);
            try {
                URL url = new URL("http://my-project571.appspot.com/?term=" + para);
                //BufferedReader webPage = new BufferedReader(new InputStreamReader(url.openStream()));
                URLConnection myURLConnection = url.openConnection();
                BufferedReader webPage = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
                result = webPage.readLine();
                System.out.println("result is :" + result);
                webPage.close();
            } catch (Exception e) {
                Log.e("log_tag", "Error in http connection "+e.toString());
            }

            return result;
        }

        private String[] parseJson(String js) {
            String[] lists = null;
            try {
                JSONArray jsonArray = new JSONArray(js);
                lists = new String[jsonArray.length()];
                for(int i=0; i < jsonArray.length(); i++) {
                    lists[i] = jsonArray.getString(i);
                }
            } catch (Exception e) {
                Log.e("log_tag", "Error in parse " + e.toString());
            }
            return lists;
        }
    }
}

class FavBean {
    public String Name;
    public String Symbol;
    public String LastPrice;
    public String Change;
    public String ChangePercent;
    public String MarketCap;
    public String mcUnite;
}