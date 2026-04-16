package com.tokenbank.net.api.ethrequest;

import com.android.volley.VolleyError;
import com.tokenbank.config.Constant;
import com.tokenbank.net.apirequest.BaseGetApiRequest;


public class ETHBroadcastRequest extends BaseGetApiRequest {

    private final String mRawTransaction;

    public ETHBroadcastRequest(String rawTransaction) {
        mRawTransaction = rawTransaction;
    }

    @Override
    public String initUrl() {
        String url = Constant.etherscan_base_url
                + "?module=proxy&action=eth_sendRawTransaction&hex=" + mRawTransaction;
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
