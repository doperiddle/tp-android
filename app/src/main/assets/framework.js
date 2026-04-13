//jt
function createJtWallet(params) {
    var paramsjson = JSON.parse(params);
    var callid = paramsjson.callid;
    var wallet = jingtum.Wallet.generate();
    var accountInfo = new Object();
    accountInfo.blockType = paramsjson.blockType;
    accountInfo.privatekey = wallet.secret;
    accountInfo.address = wallet.address;
    notifyClient(callid, 0, accountInfo);
}

function retrieveWalletFromPk(params) {
    var paramsjson = JSON.parse(params);
    var callid = paramsjson.callid;
    var privateKey = paramsjson.privateKey;
    var wallet = jingtum.Wallet.fromSecret(privateKey);
    var accountInfo = new Object();
    accountInfo.blockType = paramsjson.blockType;
    accountInfo.privatekey = wallet.secret;
    accountInfo.address = wallet.address;
    notifyClient(callid, 0, accountInfo);
}

function isValidAddress(params) {
    var paramsjson = JSON.parse(params);
    var callid = paramsjson.callid;
    var address = paramsjson.address;
    var isAddressValid = jingtum.Wallet.isValidAddress(data);
    var checkAddress = new Object();
    checkAddress.isAddressValid = isAddressValid;
    checkAddress.address = wallet.address;
    notifyClient(callid, 0, checkAddress);
}

function jtSingtx(params) {
    var paramsjson = JSON.parse(params);
    var callid = paramsjson.callid;
    var tx = {};
    tx.Fee = paramsjson.fee;
    tx.Flags = 0;
    tx.Sequence = paramsjson.sequence;//通过balance接口拿到这个值
    tx.TransactionType = 'Payment';
    tx.Account = paramsjson.account;
    tx.Amount = {"value": paramsjson.value, "currency": paramsjson.currency, "issuer": paramsjson.issuer};
    tx.Destination = paramsjson.destination;
    var sign = jingtumLib.jingtum_sign_tx(tx, {"seed": paramsjson.seed});
    var callbackData = new Object();
    var signedTransaction = new Object();
    signedTransaction.rawTransaction = sign;
    callbackData.signedTransaction = signedTransaction;
    notifyClient(callid, 0, callbackData);
}

function importWalletWithWords(params) {
    var paramsjson = JSON.parse(params);
    var callid = paramsjson.callid;
    var words = paramsjson.words;
    try {
        var wallet = jingtum.Wallet.fromMnemonic(words);
        var accountInfo = new Object();
        accountInfo.blockType = paramsjson.blockType;
        accountInfo.privatekey = wallet.secret;
        accountInfo.address = wallet.address;
        notifyClient(callid, 0, accountInfo);
    } catch (e) {
        notifyClient(callid, -1, {error: e.toString()});
    }
}

//end jt

//eth

function initEth() {
    if (typeof Web3 !== 'undefined') {
        web3 = new Web3();
    }
}

function createEthWallet(params) {
    var paramsjson = JSON.parse(params);
    var callid = paramsjson.callid;
    var mnemonic = bip39.generateMnemonic();
    var seed = bip39.mnemonicToSeed(mnemonic);
    var hdWallet = hdkey.fromMasterSeed(seed);
    var key1 = hdWallet.derivePath("m/44'/60'/0'/0/0");
    var accountInfo = new Object();
    accountInfo.blockType = paramsjson.blockType;
    accountInfo.privatekey = key1.getWallet().getPrivateKeyString();
    accountInfo.address = key1.getWallet().getAddressString();
    accountInfo.words = mnemonic;
    notifyClient(callid, 0, accountInfo);
}

function importEthWalletWithPK(params) {
    var paramsjson = JSON.parse(params);
    var callid = paramsjson.callid;
    var privateKey = paramsjson.privateKey;
    try {
        var account = web3.eth.accounts.privateKeyToAccount(privateKey);
        var accountInfo = new Object();
        accountInfo.blockType = paramsjson.blockType;
        accountInfo.privatekey = account.privateKey;
        accountInfo.address = account.address;
        notifyClient(callid, 0, accountInfo);
    } catch (e) {
        notifyClient(callid, -1, {error: e.toString()});
    }
}

function importEthWalletWithWords(params) {
    var paramsjson = JSON.parse(params);
    var callid = paramsjson.callid;
    var words = paramsjson.words;
    try {
        var seed = bip39.mnemonicToSeed(words);
        var hdWallet = hdkey.fromMasterSeed(seed);
        var key1 = hdWallet.derivePath("m/44'/60'/0'/0/0");
        var accountInfo = new Object();
        accountInfo.blockType = paramsjson.blockType;
        accountInfo.privatekey = key1.getWallet().getPrivateKeyString();
        accountInfo.address = key1.getWallet().getAddressString();
        notifyClient(callid, 0, accountInfo);
    } catch (e) {
        notifyClient(callid, -1, {error: e.toString()});
    }
}

function signEthTransaction(params) {
    var paramsjson = JSON.parse(params);
    var callid = paramsjson.callid;
    var privateKey = paramsjson.privateKey;
    var tx = paramsjson.transactionToSign;
    web3.eth.accounts.signTransaction(tx, privateKey).then(function(result) {
        var callbackData = new Object();
        callbackData.signedTransaction = result;
        notifyClient(callid, 0, callbackData);
    }).catch(function(err) {
        notifyClient(callid, -1, {error: err.toString()});
    });
}

//end eth


function notifyClient(callid, ret, extra) {
    var result = new Object();
    result.ret = ret;
    result.callid = callid;
    result.extra = extra;
    var resultStr = toJsonString(result);
    window.client.notifyWeb3Result(resultStr);
}


function toJsonString(obj) {
    if (obj == undefined) {
        return "{}";
    } else {
        return JSON.stringify(obj);
    }
}

function outputObj(obj) {
    var description = "";
    for (var i in obj) {
        description += i + " = " + (obj[i]) + "\n";
    }
    console.log(description);
}
function printAccount(account) {
    var description = "";
    for (var i in account) {
        description += i + " = " + account[i] + "\n";
    }
    console.log(description);
}

function testJson() {
    var result = new Object();
    result.ret = -1;
    alert(toJsonString(result));
}


