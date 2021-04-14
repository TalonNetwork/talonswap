package network.nerve.swap.tx.v1.vals;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.BaseConstant;
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
import network.nerve.swap.model.txdata.SyrupDepositData;

/**
 * @author Niels
 */
@Component
public class FarmSyrupDepositValidater {

    @Autowired
    private FarmCacher farmCacher;


    public ValidaterResult validate(Chain chain, Transaction tx) {
        SyrupDepositData txdata = new SyrupDepositData();
        NulsLogger logger = chain.getLogger();
        try {
            txdata.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }
        //farm 存在
        FarmPoolPO farm = farmCacher.get(txdata.getFarmHash().toHex());
        if (null == farm) {
            logger.warn("Farm not exist");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }
        //amount和coindata的保持一致
        //转入资产地址为adminAddress
        //接收地址为farm地址
        CoinData coinData = null;
        try {
            coinData = tx.getCoinDataInstance();
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }
        boolean result = false;
        for (CoinTo to : coinData.getTo()) {
            if (ArraysTool.arrayEquals(to.getAddress(), farm.getFarmAddress())) {
                if (to.getLockTime() != 0) {
                    logger.warn("锁定时间不正确");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_CANNOT_LOCK);
                }
                if (to.getAssetsChainId() != farm.getSyrupToken().getChainId() || to.getAssetsId() != farm.getSyrupToken().getAssetId()) {
                    logger.warn("糖果资产类型不正确");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_DEPOSIT_ERROR);
                }
                if (to.getAmount().compareTo(txdata.getAmount()) != 0) {
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

        byte adminType = farm.getAdminAddress()[2];
        if (adminType == BaseConstant.DEFAULT_ADDRESS_TYPE) {
            TransactionSignature signature = new TransactionSignature();
            try {
                signature.parse(tx.getTransactionSignature(), 0);
            } catch (NulsException e) {
                logger.error(e);
                return ValidaterResult.getFailed(e.getErrorCode());
            }
            if (signature.getSignersCount() != 1) {
                logger.warn("签名数量不正确");
                return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_DEPOSIT_ERROR);
            }

            if (!ArraysTool.arrayEquals(farm.getAdminAddress(), AddressTool.getAddress(signature.getP2PHKSignatures().get(0).getPublicKey(), chain.getChainId()))) {
                logger.warn("签名人不是管理员");
                return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_DEPOSIT_ERROR);
            }

        } else if (adminType == BaseConstant.P2SH_ADDRESS_TYPE) {

        }


        return ValidaterResult.getSuccess();
    }
}
