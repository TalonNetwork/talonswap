package network.nerve.swap.tx.v1.vals;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Coin;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.swap.cache.FarmCacher;
import network.nerve.swap.cache.SwapPairCacher;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.txdata.CreateFarmData;
import network.nerve.swap.rpc.call.LedgerCall;

import java.math.BigInteger;

/**
 * @author Niels
 */
@Component
public class CreateFarmTxValidater {

    public ValidaterResult validate(int chainId, Transaction tx) {

        CreateFarmData txData = new CreateFarmData();
        try {
            txData.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            Log.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }
        if (null == txData || txData.getAdminAddress() == null || txData.getStakeToken() == null || txData.getStartBlockHeight() < 0 || txData.getSyrupPerBlock() == null || txData.getSyrupToken() == null) {
            return ValidaterResult.getFailed(SwapErrorCode.PARAMETER_ERROR);
        }
        // 验证2种资产存在
        try {
            if (!LedgerCall.existNerveAsset(chainId, txData.getStakeToken().getChainId(), txData.getStakeToken().getAssetId())) {
                return ValidaterResult.getFailed(SwapErrorCode.FARM_TOKEN_ERROR);
            }
            if (!LedgerCall.existNerveAsset(chainId, txData.getSyrupToken().getChainId(), txData.getSyrupToken().getAssetId())) {
                return ValidaterResult.getFailed(SwapErrorCode.FARM_TOKEN_ERROR);
            }
        } catch (NulsException e) {
            Log.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }

        // 验证每个区块奖励数额区间正确
        if (txData.getSyrupPerBlock().compareTo(BigInteger.ZERO) <= 0) {
            return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_PER_BLOCK_ERROR);
        }
        // 验证管理员地址正确
        if (!AddressTool.validNormalAddress(txData.getAdminAddress(), chainId)) {
            return ValidaterResult.getFailed(SwapErrorCode.FARM_ADMIN_ADDRESS_ERROR);
        }

        if (txData.getStartBlockHeight() >= txData.getMultipleStopBlockHeight()) {
            return ValidaterResult.getFailed(SwapErrorCode.FARM_MULTIPLE_HEIGHT_ERROR);
        }
        if (txData.getMultiple() <= 1) {
            return ValidaterResult.getFailed(SwapErrorCode.FARM_MULTIPLE_ERROR);
        }
        return ValidaterResult.getSuccess();
    }


}
