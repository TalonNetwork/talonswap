package network.nerve.swap.rpc.callback;

import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.rpc.invoke.BaseInvoke;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.swap.model.Chain;

import java.util.HashMap;

/**
 * 订阅最新高度的回调
 *
 * @author: Loki
 * @date: 2020-02-28
 */
public class NewBlockHeightInvoke extends BaseInvoke {

    private Chain chain;

    public NewBlockHeightInvoke(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void callBack(Response response) {
        HashMap hashMap = (HashMap) ((HashMap) response.getResponseData()).get("latestHeight");
        if (null == hashMap.get("value")) {
            chain.getLogger().error("[订阅事件]最新区块高度为null");
            return;
        }
        long height = Long.valueOf(hashMap.get("value").toString());
        chain.setBestHeight(height);
    }
}