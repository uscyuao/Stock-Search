package com.example.yuao.a571stocksearch;

import android.content.Context;
import android.webkit.JavascriptInterface;
/**
 * Created by yuao on 4/30/16.
 */
public class WebAppInterface {
    Context mContext;
    StockBean stock;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c, StockBean stock) {
        mContext = c;
        this.stock = stock;
    }

    @JavascriptInterface
    public String getSymbol() {
        return stock.Symbol;
    }

    @JavascriptInterface
    public String getName() {
        return stock.Name;
    }
}
