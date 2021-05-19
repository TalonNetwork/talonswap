package network.nerve.swap.tx.v1.helpers;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.ArraysTool;
import network.nerve.swap.cache.FarmCacher;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.txdata.FarmCreateData;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.tx.v1.helpers.converter.LedgerService;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;

/**
 * @author Niels
 */
@Component
public class FarmCreateTxHelper {

    @Autowired
    private FarmCacher farmCacher;

    @Autowired
    private FarmStorageService storageService;

    @Autowired
    private LedgerService ledgerService;


    public ValidaterResult commit(int chainId, Transaction tx) throws NulsException {
        FarmCreateData txData = new FarmCreateData();
        txData.parse(tx.getTxData(), 0);

        FarmPoolPO po = getBean(chainId, tx, txData);

        farmCacher.put(tx.getHash(), po);
        storageService.save(chainId, po);
        return ValidaterResult.getSuccess();
    }

    public FarmPoolPO getBean(int chainId, Transaction tx, FarmCreateData txData) throws NulsException {
        FarmPoolPO po = new FarmPoolPO();
        TransactionSignature signature = new TransactionSignature();
        try {
            signature.parse(tx.getTransactionSignature(), 0);
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsException(e.getErrorCode());
        }
        if (signature.getSignersCount() != 1) {
            Log.warn("管理员地址不对");
            throw new NulsException(SwapErrorCode.FARM_ADMIN_ADDRESS_ERROR);
        }
        byte[] address;
        try {
            address = SwapUtils.getSingleAddressFromTX(tx, chainId,false);
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsException(e.getErrorCode());
        }
        po.setCreatorAddress(address);
        po.setFarmHash(tx.getHash());
        po.setLastRewardBlock(txData.getStartBlockHeight());
        po.setAccSyrupPerShare(BigInteger.ZERO);
        po.setLockedTime(txData.getLockedTime());
        po.setStakeToken(txData.getStakeToken());
        po.setStartBlockHeight(txData.getStartBlockHeight());
        po.setSyrupPerBlock(txData.getSyrupPerBlock());
        po.setSyrupToken(txData.getSyrupToken());
        po.setTotalSyrupAmount(txData.getTotalSyrupAmount());
        po.setSyrupTokenBalance(txData.getTotalSyrupAmount());
        return po;
    }

    public ValidaterResult rollback(int chainId, Transaction tx) {
        farmCacher.remove(tx.getHash());
        storageService.delete(chainId, tx.getHash().getBytes());
        return ValidaterResult.getSuccess();
    }

    public ValidaterResult validate(Chain chain, Transaction tx) {

        NulsLogger logger = chain.getLogger();
        FarmCreateData txData = new FarmCreateData();
        try {
            txData.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }

        return validateTxData(chain, tx, txData);
    }

    public ValidaterResult validateTxData(Chain chain, Transaction tx, FarmCreateData txData) {
        NulsLogger logger = chain.getLogger();
        int chainId = chain.getChainId();
        if (null == txData || txData.getStakeToken() == null || txData.getStartBlockHeight() < 0 || txData.getSyrupPerBlock() == null || txData.getSyrupToken() == null) {
            logger.warn("基础数据不完整");
            return ValidaterResult.getFailed(SwapErrorCode.PARAMETER_ERROR);
        }
        // 验证2种资产存在
        try {
            if (!ledgerService.existNerveAsset(chainId, txData.getStakeToken().getChainId(), txData.getStakeToken().getAssetId())) {
                logger.warn("质押资产类型不正确");
                return ValidaterResult.getFailed(SwapErrorCode.FARM_TOKEN_ERROR);
            }
            if (!ledgerService.existNerveAsset(chainId, txData.getSyrupToken().getChainId(), txData.getSyrupToken().getAssetId())) {
                logger.warn("糖果资产类型不正确");
                return ValidaterResult.getFailed(SwapErrorCode.FARM_TOKEN_ERROR);
            }
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }

        // 验证每个区块奖励数额区间正确
        if (null == txData.getSyrupPerBlock() || txData.getSyrupPerBlock().compareTo(BigInteger.ZERO) <= 0) {
            logger.warn("每块奖励数量必须大于0");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_PER_BLOCK_ERROR);
        }
        if (txData.getTotalSyrupAmount() == null || txData.getTotalSyrupAmount().compareTo(txData.getSyrupPerBlock()) <= 0) {
            logger.warn("总奖励数量必须大于每块奖励数");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_TOTAL_SYRUP_ERROR);
        }

        byte[] address;
        try {
            address = SwapUtils.getSingleAddressFromTX(tx, chainId,false);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(SwapErrorCode.FARM_ADMIN_ADDRESS_ERROR);
        }
        if (address == null) {
            logger.warn("交易签名不正确");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_ADMIN_ADDRESS_ERROR);
        }
        //amount和coindata的保持一致
        //转入资产地址为adminAddress
        //接收地址为farm地址
        CoinData coinData;
        try {
            coinData = tx.getCoinDataInstance();
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }
        boolean result = false;
        for (CoinTo to : coinData.getTo()) {
            if (ArraysTool.arrayEquals(to.getAddress(), SwapUtils.getFarmAddress(chain.getChainId()))) {
                if (to.getLockTime() != 0) {
                    logger.warn("锁定时间不正确");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_CANNOT_LOCK);
                }
                if (to.getAssetsChainId() != txData.getSyrupToken().getChainId() || to.getAssetsId() != txData.getSyrupToken().getAssetId()) {
                    logger.warn("糖果资产类型不正确");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_DEPOSIT_ERROR);
                }
                if (to.getAmount().compareTo(txData.getTotalSyrupAmount()) != 0) {
                    logger.warn("金额不正确");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_DEPOSIT_AMOUNT_ERROR);
                }
                result = true;
            }
        }
        if (!result) {
            logger.warn("没有转入farm地址中");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_DEPOSIT_ERROR);
        }

        return ValidaterResult.getSuccess();
    }

    public void setFarmCacher(FarmCacher farmCacher) {
        this.farmCacher = farmCacher;
    }

    public void setStorageService(FarmStorageService storageService) {
        this.storageService = storageService;
    }

    public void setLedgerService(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }
}
