package network.nerve.swap.tx.v1.vals;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.swap.cache.FarmCacher;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.txdata.CreateFarmData;
import network.nerve.swap.rpc.call.LedgerCall;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;

/**
 * @author Niels
 */
@Component
public class CreateFarmTxCommiter {

    @Autowired
    private FarmCacher farmCacher;

    @Autowired
    private FarmStorageService storageService;

    public ValidaterResult commit(int chainId, Transaction tx) throws NulsException {
        CreateFarmData txData = new CreateFarmData();
        txData.parse(tx.getTxData(), 0);


        FarmPoolPO po = new FarmPoolPO();


        po.setFarmAddress(SwapUtils.getFarmAddress(chainId, tx.getHash()));
        po.setAdminAddress(txData.getAdminAddress());
        po.setFarmHash(tx.getHash());
        po.setLastRewardBlock(0L);
        po.setMultiple(txData.getMultiple());
        po.setMultipleStopBlockHeight(txData.getMultipleStopBlockHeight());
        po.setStakeToken(txData.getStakeToken());
        po.setStartBlockHeight(txData.getStartBlockHeight());
        po.setSyrupPerBlock(txData.getSyrupPerBlock());
        po.setSyrupToken(txData.getSyrupToken());


        farmCacher.put(tx.getHash().toHex(), po);
        storageService.save(po);
        return ValidaterResult.getSuccess();
    }


    public ValidaterResult rollback(int chainId, Transaction tx) {
        farmCacher.remove(tx.getHash().toHex());
        storageService.delete(tx.getHash().getBytes());
        return ValidaterResult.getSuccess();
    }
}
