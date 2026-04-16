package com.tokenbank.base;

import android.content.Context;
import android.text.TextUtils;

import com.tokenbank.config.Constant;
import com.tokenbank.dialog.EthGasSettignDialog;
import com.tokenbank.net.api.ethrequest.ETHBalanceRequest;
import com.tokenbank.net.api.ethrequest.ETHBroadcastRequest;
import com.tokenbank.net.api.ethrequest.ETHNonceRequest;
import com.tokenbank.net.api.ethrequest.ETHTransactionListRequest;
import com.tokenbank.net.load.RequestPresenter;
import com.tokenbank.utils.GsonUtil;
import com.tokenbank.utils.Util;


/**
 * Ethereum blockchain implementation of {@link BaseWalletUtil}.
 *
 * Wallet creation and transaction signing are handled by the JS bridge
 * (web3.js via {@link JSUtil}).  Balance queries, transaction history and
 * broadcasting are handled through the Etherscan public API.
 */
public class ETHWalletBlockchain implements BaseWalletUtil {

    private static final String TAG = "ETHWalletBlockchain";

    /** ETH address: "0x" + 40 hex characters (case-insensitive). */
    private static final String ETH_ADDRESS_REGEX = "^0[xX][0-9a-fA-F]{40}$";
    /** ETH private key: optional "0x" + 64 hex characters. */
    private static final String ETH_PK_REGEX = "^(0[xX])?[0-9a-fA-F]{64}$";

    @Override
    public void init() {
    }

    @Override
    public void createWallet(final String walletName, final String walletPassword, int blockType,
                             final WCallback callback) {
        if (!checkInit(callback)) {
            return;
        }
        GsonUtil json = new GsonUtil("{}");
        json.putInt("blockType", blockType);
        JSUtil.getInstance().callJS("createEthWallet", json, callback);
    }

    @Override
    public void importWallet(String privateKey, int blockType, int type, WCallback callback) {
        if (!checkInit(callback)) {
            return;
        }
        GsonUtil json = new GsonUtil("{}");
        json.putInt("blockType", blockType);
        if (type == 1) {
            json.putString("words", privateKey);
            JSUtil.getInstance().callJS("importEthWalletWithWords", json, callback);
        } else {
            json.putString("privateKey", privateKey);
            JSUtil.getInstance().callJS("importEthWalletWithPK", json, callback);
        }
    }

    @Override
    public void toIban(String address, WCallback callback) {
        if (!checkInit(callback)) {
            return;
        }
        GsonUtil json = new GsonUtil("{}");
        json.putString("ethAddress", address);
        JSUtil.getInstance().callJS("toIbanAddress", json, callback);
    }

    @Override
    public void fromIban(String ibanAddress, WCallback callback) {
        if (!checkInit(callback)) {
            return;
        }
        GsonUtil json = new GsonUtil("{}");
        json.putString("ibanAddress", ibanAddress);
        JSUtil.getInstance().callJS("toEthAddress", json, callback);
    }

    @Override
    public void gasPrice(final WCallback callback) {
        // Return a sensible default (8 Gwei); a real implementation would
        // call the Etherscan gas-oracle or eth_gasPrice.
        GsonUtil gasPriceJson = new GsonUtil("{}");
        gasPriceJson.putDouble("gasPrice", 8.0);
        callback.onGetWResult(0, gasPriceJson);
    }

    @Override
    public void signedTransaction(GsonUtil data, WCallback callback) {
        if (!checkInit(callback)) {
            return;
        }
        JSUtil.getInstance().callJS("signEthTransaction", data, callback);
    }

    @Override
    public void sendSignedTransaction(final String rawTransaction, final WCallback callback) {
        new RequestPresenter().loadEtherscanData(new ETHBroadcastRequest(rawTransaction),
                new RequestPresenter.RequestCallback() {
                    @Override
                    public void onRequesResult(int ret, GsonUtil json) {
                        callback.onGetWResult(ret, json);
                    }
                });
    }

    @Override
    public boolean isWalletLegal(String pk, String address) {
        return checkWalletPk(pk) && checkWalletAddress(address);
    }

    @Override
    public void generateReceiveAddress(String walletAddress, double amount, String token,
                                       WCallback callback) {
        if (TextUtils.isEmpty(walletAddress)) {
            callback.onGetWResult(-1, new GsonUtil("{}"));
            return;
        }
        final double safeAmount = amount < 0 ? 0.0 : amount;
        GsonUtil address = new GsonUtil("{}");
        String receiveStr = String.format("ethereum:%s?amount=%f&token=%s",
                walletAddress, safeAmount, token);
        address.putString("receiveAddress", receiveStr);
        callback.onGetWResult(0, address);
    }

    @Override
    public void calculateGasInToken(double gas, double gasPrice, boolean defaultToken,
                                    WCallback callback) {
        double totalGasInWei = Util.fromGweToWei(TBController.ETH_INDEX, gasPrice) * gas;
        double gasInEth = Util.fromWei(TBController.ETH_INDEX, totalGasInWei);
        GsonUtil gasJson = new GsonUtil("{}");
        gasJson.putString("gas", Util.formatDoubleToStr(5, gasInEth) + " ETH");
        gasJson.putDouble("gasPrice", gasPrice);
        callback.onGetWResult(0, gasJson);
    }

    @Override
    public void gasSetting(final Context context, final double gasPrice, final boolean defaultToken,
                           final WCallback callback) {
        EthGasSettignDialog dialog = new EthGasSettignDialog(context,
                new EthGasSettignDialog.OnSettingGasListener() {
                    @Override
                    public void onSettingGas(double newGasPrice, double gasInToken) {
                        GsonUtil gas = new GsonUtil("{}");
                        gas.putString("gas", Util.formatDoubleToStr(5, gasInToken) + " ETH");
                        gas.putDouble("gasPrice", newGasPrice);
                        callback.onGetWResult(0, gas);
                    }
                }, gasPrice, defaultToken, TBController.ETH_INDEX);
        dialog.show();
    }

    @Override
    public double getRecommendGas(double gas, boolean defaultToken) {
        return Util.getRecommendGweiGas(TBController.ETH_INDEX, defaultToken);
    }

    @Override
    public String getDefaultTokenSymbol() {
        return "ETH";
    }

    @Override
    public int getDefaultDecimal() {
        return 18;
    }

    @Override
    public void getTokenInfo(String token, long blockChainId, WCallback callback) {
    }

    @Override
    public void translateAddress(String sourceAddress, WCallback callback) {
        GsonUtil addressJson = new GsonUtil("{}");
        addressJson.putString("receive_address",
                TextUtils.isEmpty(sourceAddress) ? "" : sourceAddress);
        callback.onGetWResult(0, addressJson);
    }

    @Override
    public boolean checkWalletAddress(String receiveAddress) {
        if (TextUtils.isEmpty(receiveAddress)) {
            return false;
        }
        return receiveAddress.matches(ETH_ADDRESS_REGEX);
    }

    @Override
    public boolean checkWalletPk(String privateKey) {
        if (TextUtils.isEmpty(privateKey)) {
            return false;
        }
        return privateKey.matches(ETH_PK_REGEX);
    }

    @Override
    public void queryTransactionDetails(final String hash, final WCallback callback) {
        if (TextUtils.isEmpty(hash)) {
            callback.onGetWResult(-1, new GsonUtil("{}"));
            return;
        }
        // ETH transaction details are surfaced from the list; a dedicated
        // tx-detail endpoint is not currently wired to the UI for ETH.
        callback.onGetWResult(-1, new GsonUtil("{}"));
    }

    @Override
    public void queryTransactionList(final GsonUtil params, final WCallback callback) {
        int page = params.getInt("start", 0) + 1;
        int pageSize = params.getInt("pagesize", 10);
        final String walletAddress = WalletInfoManager.getInstance().getWAddress();

        new RequestPresenter().loadEtherscanData(
                new ETHTransactionListRequest(walletAddress, page, pageSize),
                new RequestPresenter.RequestCallback() {
                    @Override
                    public void onRequesResult(int ret, GsonUtil json) {
                        if (ret == 0) {
                            GsonUtil translatedData = new GsonUtil("{}");
                            GsonUtil dataList = new GsonUtil("[]");
                            GsonUtil txList = json.getArray("result", "[]");
                            int len = txList.getLength();
                            for (int i = 0; i < len; i++) {
                                GsonUtil tx = txList.getObject(i, "{}");
                                GsonUtil item = new GsonUtil("{}");
                                item.putString("hash", tx.getString("hash", ""));
                                item.putString("from", tx.getString("from", "").toLowerCase());
                                item.putString("to", tx.getString("to", "").toLowerCase());
                                item.putLong("timeStamp",
                                        tx.getLong("timeStamp", 0L));
                                item.putDouble("fee",
                                        Util.fromWei(TBController.ETH_INDEX,
                                                tx.getDouble("gasUsed", 0.0)
                                                        * tx.getDouble("gasPrice", 0.0)));
                                // value is in wei; convert to ETH
                                double valueInEth = Util.fromWei(TBController.ETH_INDEX,
                                        Util.parseDouble(tx.getString("value", "0")));
                                item.putString("real_value",
                                        Util.formatDoubleToStr(8, valueInEth));
                                item.putString("tokenSymbol", "ETH");
                                dataList.add(item);
                            }
                            translatedData.put("data", dataList);
                            translatedData.putString("marker", "");
                            callback.onGetWResult(0, translatedData);
                        } else {
                            callback.onGetWResult(-1, new GsonUtil("{}"));
                        }
                    }
                });
    }

    @Override
    public double getValue(int decimal, double originValue) {
        if (decimal <= 0) {
            decimal = getDefaultDecimal();
        }
        return Util.formatDouble(8, Util.translateValue(decimal, originValue));
    }

    @Override
    public void queryBalance(final String address, int type, final WCallback callback) {
        new RequestPresenter().loadEtherscanData(new ETHBalanceRequest(address),
                new RequestPresenter.RequestCallback() {
                    @Override
                    public void onRequesResult(int ret, GsonUtil json) {
                        GsonUtil formatData = new GsonUtil("{}");
                        GsonUtil arrays = new GsonUtil("[]");
                        GsonUtil data = new GsonUtil("{}");
                        data.putLong("blockchain_id", TBController.ETH_INDEX);
                        data.putString("icon_url",
                                "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/ethereum/info/logo.png");
                        data.putString("bl_symbol", "ETH");
                        data.putInt("decimal", getDefaultDecimal());
                        if (ret == 0) {
                            // result is the balance in wei (decimal string)
                            String weiStr = json.getString("result", "0");
                            double balanceEth = Util.fromWei(TBController.ETH_INDEX,
                                    Util.parseDouble(weiStr));
                            data.putString("balance", Util.formatDoubleToStr(8, balanceEth));
                        } else {
                            data.putString("balance", "0");
                        }
                        data.putString("asset", "0");
                        arrays.put(data);
                        formatData.put("data", arrays);
                        callback.onGetWResult(0, formatData);
                    }
                });
    }

    @Override
    public String getTransactionSearchUrl(String hash) {
        return "https://etherscan.io/tx/" + hash;
    }

    @Override
    public GsonUtil loadTransferTokens(Context context) {
        return new GsonUtil("[]");
    }

    // -------------------------------------------------------------------------

    public GsonUtil buildTransactionParams(String from, String to, double valueEth,
                                           double gasPrice, double gas, long nonce) {
        GsonUtil tx = new GsonUtil("{}");
        tx.putString("from", from);
        tx.putString("to", to);
        // value in wei (hex string expected by web3.js signTransaction)
        long weiValue = (long) Util.toWei(TBController.ETH_INDEX, valueEth);
        tx.putString("value", "0x" + Long.toHexString(weiValue));
        tx.putString("gas", "0x" + Long.toHexString((long) gas));
        long gasPriceWei = (long) Util.fromGweToWei(TBController.ETH_INDEX, gasPrice);
        tx.putString("gasPrice", "0x" + Long.toHexString(gasPriceWei));
        tx.putString("nonce", "0x" + Long.toHexString(nonce));
        tx.putString("chainId", "1"); // Ethereum mainnet
        return tx;
    }

    public void getNonce(String address, final WCallback callback) {
        new RequestPresenter().loadEtherscanData(new ETHNonceRequest(address),
                new RequestPresenter.RequestCallback() {
                    @Override
                    public void onRequesResult(int ret, GsonUtil json) {
                        if (ret == 0) {
                            // result is hex nonce, e.g. "0x1a"
                            String hexNonce = json.getString("result", "0x0");
                            long nonce;
                            try {
                                nonce = Long.parseLong(
                                        hexNonce.startsWith("0x") || hexNonce.startsWith("0X")
                                                ? hexNonce.substring(2) : hexNonce, 16);
                            } catch (NumberFormatException e) {
                                nonce = 0;
                            }
                            GsonUtil result = new GsonUtil("{}");
                            result.putLong("nonce", nonce);
                            callback.onGetWResult(0, result);
                        } else {
                            callback.onGetWResult(-1, new GsonUtil("{}"));
                        }
                    }
                });
    }

    private boolean checkInit(WCallback callback) {
        return JSUtil.getInstance().checkInit(callback);
    }
}
