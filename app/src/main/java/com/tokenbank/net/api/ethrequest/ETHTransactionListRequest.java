package com.tokenbank.net.api.ethrequest;

import com.android.volley.VolleyError;
import com.tokenbank.config.Constant;
import com.tokenbank.net.apirequest.BaseGetApiRequest;


public class ETHTransactionListRequest extends BaseGetApiRequest {

    private final String mAddress;
    private final int mPage;
    private final int mPageSize;

    public ETHTransactionListRequest(String address, int page, int pageSize) {
        mAddress = address;
        mPage = page;
        mPageSize = pageSize;
    }

    @Override
    public String initUrl() {
        String url = Constant.etherscan_base_url
                + "?module=account&action=txlist&address=" + mAddress
                + "&page=" + mPage
                + "&offset=" + mPageSize
                + "&sort=desc";
        if (!Constant.ETHERSCAN_API_KEY.isEmpty()) {
            url += "&apikey=" + Constant.ETHERSCAN_API_KEY;
        }
        return url;
    }

    @Override
    public void handleMessage(String response) {
    }

    @Override
    public void handleError(int code, VolleyError error) {
    }
}
