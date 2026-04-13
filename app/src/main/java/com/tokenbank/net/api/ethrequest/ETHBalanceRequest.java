package com.tokenbank.net.api.ethrequest;

import com.android.volley.VolleyError;
import com.tokenbank.config.Constant;
import com.tokenbank.net.apirequest.BaseGetApiRequest;


public class ETHBalanceRequest extends BaseGetApiRequest {

    private final String mAddress;

    public ETHBalanceRequest(String address) {
        mAddress = address;
    }

    @Override
    public String initUrl() {
        String url = Constant.etherscan_base_url
                + "?module=account&action=balance&address=" + mAddress + "&tag=latest";
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
