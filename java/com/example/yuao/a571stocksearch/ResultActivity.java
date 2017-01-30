package com.example.yuao.a571stocksearch;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.facebook.CallbackManager;
import com.facebook.share.Sharer;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;

import java.util.*;
import java.text.*;

import android.content.*;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.support.design.widget.TabLayout;
import android.view.*;
import android.widget.*;
import android.webkit.*;
import android.text.method.LinkMovementMethod;
import android.text.Html;
import android.net.Uri;
import android.graphics.PorterDuff;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

public class ResultActivity extends AppCompatActivity {
    StockBean stock;
    FeedBean[] feed;
    ViewPager mViewPager;
    TabLayout mTabLayout;
    ArrayList<View> viewContainer = new ArrayList<>();
    CallbackManager callbackManager;
    ShareDialog shareDialog;
    SharedPreferences settings;
    ImageView mImageView;
    Drawable imageDrawable;
    PhotoViewAttacher mAttacher;
    private Toast mCurrentToast;

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ResultActivity activity = this;
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                Toast.makeText(activity, "You shared this post!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancel() {
                Toast.makeText(activity, "No post is shared!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(FacebookException error) {
                Toast.makeText(activity, "No post is shared because of internal error!", Toast.LENGTH_SHORT).show();
            }
        });

        setContentView(R.layout.activity_result);

        Intent intent = getIntent();
        stock = (StockBean) parseJson(intent.getStringExtra(MainActivity.EXTRA_MESSAGE), "details");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(stock.Name);

        new httpGetDataAndProcess(this).execute(stock.Symbol, "feeds");

        settings = getSharedPreferences(getString(R.string.app_name), 0);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        //to check whether this stock is starred
        MenuItem item = menu.getItem(0);
        item.setTitle("empty");
        int compNum = settings.getInt("companyNum", 0);
        System.out.println("result activety on star button creation companyNum is:" + compNum);
        for(int i=1; i<=compNum; i++) {
            if(settings.getString("symbol"+i, null).equals(stock.Symbol)) {
                item.setIcon(R.drawable.ic_star_black_24dp);
                item.getIcon().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);
                item.setTitle("star");
            }
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        int compNum = settings.getInt("companyNum", 0);
        System.out.println("After adding a star num is:" + compNum);
        System.out.println("After adding a star num is:" + 1);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favorite:
                if(item.getTitle().toString().equals("empty")){
                    item.setIcon(R.drawable.ic_star_black_24dp);
                    item.getIcon().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);
                    item.setTitle("star");
                    int compNum = settings.getInt("companyNum", 0);
                    System.out.println("Before adding a star num is:" + compNum);
                    compNum++;
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("symbol"+compNum, stock.Symbol);
                    editor.putInt("companyNum", compNum);
                    // Commit the edits!
                    editor.commit();
                } else if(item.getTitle().toString().equals("star")) {
                    //System.out.print("before selected is full");
                    item.setIcon(R.drawable.ic_star_border_black_24dp);
                    item.setTitle("empty");
                    int compNum = settings.getInt("companyNum", 0);
                    if(compNum==0) {
                        System.out.println("very big error");
                        System.exit(0);
                    }
                    SharedPreferences.Editor editor = settings.edit();
                    int i=1;
                    for(; i<=compNum; i++) {
                        if(settings.getString("symbol"+i, null) == stock.Symbol) {
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
                } else {
                    System.out.println("very big error");
                    System.exit(0);
                }

                return true;

            case R.id.action_facebook:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                if (ShareDialog.canShow(ShareLinkContent.class)) {
                    ShareLinkContent linkContent = new ShareLinkContent.Builder()
                            .setContentTitle("Current Stock Price of " + stock.Name + " is $" + stock.LastPrice)
                            .setContentDescription(
                                    "Stock Information of " + stock.Name + " (" + stock.Symbol + ')')
                            .setContentUrl(Uri.parse("http://dev.markitondemand.com/"))
                            .setImageUrl(Uri.parse("http://chart.finance.yahoo.com/t?s=" + stock.Symbol + "&lang=en-US&width=200&height=200"))
                            .setQuote("Last Traded Price: $ " + stock.LastPrice + ", Change: " + stock.Change + " (" + stock.ChangePercent + "%)")
                            .build();
                    shareDialog.show(linkContent);
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private static class ItemViewCache{
        public TextView title;
        public TextView data;
        public ImageView icon;
    }
    private static class ItemFeedCache{
        public TextView title;
        public TextView data;
        public TextView provider;
        public TextView date;
    }

    public void initView() {
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);

        LinearLayout detailsContainer = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.details_layout, null);
        NonScrollListView view1 =  (NonScrollListView) detailsContainer.findViewById(R.id.nonscroll_view);
        mImageView = (ImageView) detailsContainer.findViewById(R.id.detail_chart);
        mImageView.setImageDrawable(imageDrawable);
        mAttacher = new PhotoViewAttacher(mImageView);
        mAttacher.setOnPhotoTapListener(new PhotoTapListener());
        mAttacher.setZoomable(true);

        view1.setAdapter(new ArrayAdapter<String>(this, R.layout.listitem_layout, stock.toStringArray()) {

            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView==null){
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_layout, null);
                    ItemViewCache viewCache = new ItemViewCache();
                    viewCache.title = (TextView) convertView.findViewById(R.id.item_head);
                    viewCache.data = (TextView) convertView.findViewById(R.id.item_data);
                    //if(position+1==8 || position+1==4)
                    viewCache.icon = (ImageView) convertView.findViewById(R.id.item_icon);
                    convertView.setTag(viewCache);
                }
                ItemViewCache cache = (ItemViewCache) convertView.getTag();
                //设置文本和图片，然后返回这个View，用于ListView的Item的展示
                switch (position+1) {
                    case 1:
                        cache.title.setText(getText(R.string.detail_title1));
                        cache.icon.setVisibility(View.GONE);
                        break;
                    case 2:
                        cache.title.setText(getText(R.string.detail_title2)); break;
                    case 3:
                        cache.title.setText(getText(R.string.detail_title3)); break;
                    case 4:  // change
                        cache.title.setText(getText(R.string.detail_title4));
                        if(getItem(position).charAt(0) == '-')
                            cache.icon.setImageResource(R.drawable.down);
                        else
                            cache.icon.setImageResource(R.drawable.up);
                        break;
                    case 5:
                        cache.title.setText(getText(R.string.detail_title5)); break;
                    case 6:
                        cache.title.setText(getText(R.string.detail_title6)); break;
                    case 7:
                        cache.title.setText(getText(R.string.detail_title7)); break;
                    case 8:
                        if(getItem(position).charAt(0) == '-')
                            cache.icon.setImageResource(R.drawable.down);
                        else
                            cache.icon.setImageResource(R.drawable.up);
                        cache.title.setText(getText(R.string.detail_title8)); break;
                    case 9:
                        cache.title.setText(getText(R.string.detail_title9)); break;
                    case 10:
                        cache.title.setText(getText(R.string.detail_title10)); break;
                    case 11:
                        cache.title.setText(getText(R.string.detail_title11)); break;
                }
                cache.data.setText(getItem(position));
                return convertView;
            }
        });
        //view1.setBackgroundColor(0x0);
//        ImageView

        WebView myWebView = (WebView) LayoutInflater.from(this).inflate(R.layout.webview_layout, null);
        myWebView.loadUrl("http://www-scf.usc.edu/~yuaoliu/html/hisChart.html");
        myWebView.addJavascriptInterface(new WebAppInterface(this, stock), "Android");
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        ScrollView feedPart = (ScrollView) LayoutInflater.from(this).inflate(R.layout.feeds_layout, null);
        NonScrollListView feedList =  (NonScrollListView) feedPart.findViewById(R.id.nonscroll_view_feeds);
        feedList.setAdapter(new ArrayAdapter<FeedBean>(this, R.layout.feedsitem_layout, feed) {

            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView==null){
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.feedsitem_layout, null);
                    ItemFeedCache viewCache = new ItemFeedCache();
                    viewCache.title = (TextView) convertView.findViewById(R.id.feed_title);
                    viewCache.data = (TextView) convertView.findViewById(R.id.feed_content);
                    viewCache.provider = (TextView) convertView.findViewById(R.id.feed_provider);
                    viewCache.date = (TextView) convertView.findViewById(R.id.feed_date);
                    convertView.setTag(viewCache);
                }
                ItemFeedCache cache = (ItemFeedCache) convertView.getTag();
                cache.title.setText(Html.fromHtml(getItem(position).title));
                cache.title.setMovementMethod(LinkMovementMethod.getInstance());
                cache.title.setLinkTextColor(Color.BLACK);
                cache.data.setText(getItem(position).description);
                cache.provider.setText(getItem(position).source);
                cache.date.setText(getItem(position).date);
                return convertView;
            }
        });

//        view1.setOnTouchListener(new OnTouchListener() {
//
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_MOVE) {
//                    return true;
//                    // Indicates that this has been handled by you and will not be forwarded further.
//                }
//                return false;
//            }
//        });

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(detailsContainer);
        scrollView.setFillViewport(true);
        //scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        viewContainer.add(scrollView);
        viewContainer.add(myWebView);
        viewContainer.add(feedPart);

        mViewPager.setAdapter( new PagerAdapter() {

            @Override
            public int getCount() {
                return  viewContainer.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return  view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View view;
                container.addView(view = viewContainer.get(position));
                return view;
            }

            @Override
            public void destroyItem(ViewGroup container, int position,  Object
                    object) {
                container.removeView(viewContainer.get(position));
            }

            //可以不重写
//            @Override
//            public CharSequence getPageTitle(int position) {
//                return titleContainer.get(position);
//            }
        });
        mViewPager.addOnPageChangeListener(new android.support.design.widget.TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        //mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                mViewPager.setCurrentItem(position, true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
        });
    }

    private class PhotoTapListener implements OnPhotoTapListener {

        @Override
        public void onPhotoTap(View view, float x, float y) {
            float xPercentage = x * 100f;
            float yPercentage = y * 100f;

            showToast(String.format("TAP", xPercentage, yPercentage, view == null ? 0 : view.getId()));
        }

        @Override
        public void onOutsidePhotoTap() {
            showToast("You have a tap event on the place where out of the photo.");
        }
    }

    private void showToast(CharSequence text) {
        if (null != mCurrentToast) {
            mCurrentToast.cancel();
        }

        mCurrentToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        mCurrentToast.show();
    }

    public Object parseJson(String js, String mode) {
        if(mode == "details") {
            try { //http://my-project571.appspot.com/?symbol=Grngrn
                StockBean stock = new StockBean();
                JSONObject jsonObj = new JSONObject(js);
                stock.status = true;
                stock.Name = jsonObj.getString("Name");
                stock.Symbol = jsonObj.getString("Symbol");
                stock.LastPrice = String.format("%.2f", jsonObj.getDouble("LastPrice"));
                stock.Change = String.format("%.2f", jsonObj.getDouble("Change"));
                stock.ChangePercent = String.format("%.2f", jsonObj.getDouble("ChangePercent"));
                double temp;
                temp = jsonObj.getDouble("MarketCap") / 1000000000;
                if (temp < 0.005) {
                    temp *= 1000;
                    stock.mcUnite = "Million";
                } else
                    stock.mcUnite = "Billion";
                stock.MarketCap = String.format("%.2f", temp);
                stock.Timestamp = jsonObj.getString("Timestamp");
                SimpleDateFormat dftParser = new SimpleDateFormat("E MMM dd HH:mm:ss 'UTC'ZZZZZ yyyy");
                dftParser.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    Date date = new Date();
                    date = dftParser.parse(stock.Timestamp);
                    SimpleDateFormat ftCreater = new SimpleDateFormat ("dd MMMM yyyy, HH:mm:ss");
                    stock.Timestamp = ftCreater.format(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                temp = jsonObj.getLong("Volume") / 1000000.0;
                stock.Volume = String.format("%.2f", temp);
                stock.ChangeYTD = String.format("%.2f", jsonObj.getDouble("ChangeYTD"));
                if(jsonObj.getDouble("ChangePercentYTD") > 0)
                    stock.ChangePercentYTD = "+" + String.format("%.2f", jsonObj.getDouble("ChangePercentYTD"));
                else
                    stock.ChangePercentYTD = String.format("%.2f", jsonObj.getDouble("ChangePercentYTD"));
                stock.High = jsonObj.getDouble("High");
                stock.Low = jsonObj.getDouble("Low");
                stock.Open = jsonObj.getDouble("Open");
                return stock;
            } catch (Exception e) {
                Log.e("log_tag", "Error in parse " + e.toString());
            }
        } else if(mode == "symbols") {
            try {
                JSONObject jsonObj = new JSONObject(js);
                JSONArray jsonArray = new JSONArray(js);
                String[] lists = new String[jsonArray.length()];
                for(int i=0; i < jsonArray.length(); i++) {
                    lists[i] = jsonArray.getString(i);
                }
            } catch (Exception e) {
                Log.e("log_tag", "Error in parse " + e.toString());
            }
        } else if(mode.equals("feeds")) {
            try {
                JSONArray data = (new JSONObject(js)).getJSONObject("d").getJSONArray("results");
                FeedBean[] feed = new FeedBean[data.length()];
                for(int i=0; i<data.length(); i++) {
                    feed[i] = new FeedBean();
                    JSONObject item = data.getJSONObject(i);
                    feed[i].url = item.getString("Url");
                    feed[i].title = "<a href=\"" + feed[i].url + "\">" + item.getString("Title") + "</a>";
                    feed[i].description = item.getString("Description");
                    feed[i].source = "Publisher: " + item.getString("Source");
                    feed[i].date = "Date: " + item.getString("Date");
                }
                return feed;
            } catch (Exception e) {
                Log.e("log_tag", "Error in parse " + e.toString());
            }
        }
        System.out.println("Wrong parameters");
        return null;
    }
}


class StockBean {
    public boolean status = false;
    public String message;
    public String Name;
    public String Symbol;
    public String LastPrice;
    public String Change;
    public String ChangePercent;
    public String Timestamp;
    public String MarketCap;
    public String mcUnite;
    public String Volume;
    public String ChangeYTD;
    public String ChangePercentYTD;
    public double High;
    public double Low;
    public double Open;
    public Drawable chart;

    public String[] toStringArray() {
        String[] res = new String[11];
        res[0] = Name;
        res[1] = Symbol;
        res[2] = LastPrice;
        res[3] = Change + "(" + ChangePercent + "%)";
        res[4] = Timestamp;
        res[5] = MarketCap + " " + mcUnite;
        res[6] = Volume + " Million";
        res[7] = ChangeYTD + "(" + ChangePercentYTD + "%)";
        res[8] = ((Double) High).toString();
        res[9] = ((Double) Low).toString();
        res[10] = ((Double) Open).toString();
        return res;
    }
}

class FeedBean {
    String url;
    String title;
    String description;
    String source;
    String date;
}
