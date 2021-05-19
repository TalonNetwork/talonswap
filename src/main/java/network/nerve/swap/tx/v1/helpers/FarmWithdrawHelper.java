package network.nerve.swap.tx.v1.helpers;

import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.swap.cache.FarmCacher;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.manager.FarmTempManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.po.FarmUserInfoPO;
import network.nerve.swap.model.txdata.FarmStakeChangeData;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.storage.FarmUserInfoStorageService;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;

/**
 * @author Niels
 */
@Component
public class FarmWithdrawHelper {

    @Autowired
    private FarmCacher farmCacher;

    @Autowired
    private FarmStorageService storageService;

    @Autowired
    private FarmUserInfoStorageService userInfoStorageService;

    public ValidaterResult validate(Chain chain, Transaction tx, long blockTime) {
        NulsLogger logger = chain.getLogger();
        FarmStakeChangeData data = new FarmStakeChangeData();
        try {
            data.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(SwapErrorCode.DATA_PARSE_ERROR);
        }
        return validateTxData(chain, tx, data, null, blockTime);
    }

    public ValidaterResult validateTxData(Chain chain, Transaction tx, FarmStakeChangeData data, FarmTempManager farmTempManager, long blockTime) {
        NulsLogger logger = chain.getLogger();
        //验证farm是否存在
        FarmPoolPO farm = farmCacher.get(data.getFarmHash());
        String farmHash = data.getFarmHash().toHex();
        if (farmTempManager != null && farmTempManager.getFarm(farmHash) != null) {
            farm = farmTempManager.getFarm(farmHash);
        }
        if (farm == null) {
            logger.warn("Farm不存在");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }

        byte[] userAddress;
        try {
            userAddress = SwapUtils.getSingleAddressFromTX(tx, chain.getChainId(), true);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(SwapErrorCode.FARM_NERVE_WITHDRAW_ERROR);
        }

        FarmUserInfoPO user = userInfoStorageService.load(chain.getChainId(), data.getFarmHash(), userAddress);
        if (null == user || user.getAmount().compareTo(data.getAmount()) < 0) {
            logger.warn("用户退出金额超出");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_NERVE_WITHDRAW_ERROR);
        }

        if (blockTime > 0 && blockTime < farm.getLockedTime()) {
            logger.warn("Farm尚未解锁");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_IS_LOCKED_ERROR);
        }

        return ValidaterResult.getSuccess();
    }

    public void setFarmCacher(FarmCacher farmCacher) {
        this.farmCacher = farmCacher;
    }

    public void setStorageService(FarmStorageService storageService) {
        this.storageService = storageService;
    }

    public void setUserInfoStorageService(FarmUserInfoStorageService userInfoStorageService) {
        this.userInfoStorageService = userInfoStorageService;
    }
}
