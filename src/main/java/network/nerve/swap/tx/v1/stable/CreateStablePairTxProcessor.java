package network.nerve.swap.tx.v1.stable;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.swap.cache.LedgerAssetCacher;
import network.nerve.swap.cache.StableSwapPairCacher;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.help.LedgerAssetRegisterHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.txdata.stable.CreateStablePairData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
@Component("CreateStablePairTxProcessorV1")
public class CreateStablePairTxProcessor implements TransactionProcessor {

    @Autowired
    private StableSwapPairCacher stableSwapPairCacher;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private LedgerAssetCacher ledgerAssetCacher;
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;

    @Override
    public int getType() {
        return TxType.CREATE_SWAP_PAIR;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = chainManager.getChain(chainId);
        Map<String, Object> resultMap = new HashMap<>(SwapConstant.INIT_CAPACITY_2);
        if (chain == null) {
            Log.error("Chains do not exist.");
            resultMap.put("txList", txs);
            resultMap.put("errorCode", SwapErrorCode.CHAIN_NOT_EXIST.getCode());
            return resultMap;
        }
        NulsLogger logger = chain.getLogger();
        List<Transaction> failsList = new ArrayList<>();
        String errorCode = SwapErrorCode.SUCCESS.getCode();
        C1:
        for (Transaction tx : txs) {
            if (tx.getType() != getType()) {
                logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                failsList.add(tx);
                errorCode = SwapErrorCode.DATA_ERROR.getCode();
                continue;
            }

            CreateStablePairData txData = new CreateStablePairData();
            try {
                txData.parse(tx.getTxData(), 0);
            } catch (NulsException e) {
                Log.error(e);
                failsList.add(tx);
                errorCode = e.getErrorCode().getCode();
                continue;
            }
            NerveToken[] coins = txData.getCoins();
            int length = coins.length;
            if (length < 2) {
                logger.error("INVALID_COINS! hash-{}", tx.getHash().toHex());
                failsList.add(tx);
                errorCode = SwapErrorCode.INVALID_COINS.getCode();
                continue;
            }
            for (int i = 0; i < length; i++) {
                NerveToken token = coins[i];
                LedgerAssetDTO asset = ledgerAssetCacher.getLedgerAsset(token);
                if (asset == null) {
                    logger.error("Ledger asset not exist! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.LEDGER_ASSET_NOT_EXIST.getCode();
                    continue C1;
                }
            }
        }
        resultMap.put("txList", failsList);
        resultMap.put("errorCode", errorCode);
        return resultMap;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger logger = chain.getLogger();
            Map<String, SwapResult> swapResultMap = chain.getBatchInfo().getSwapResultMap();
            for (Transaction tx : txs) {
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                if (!result.isSuccess()) {
                    return true;
                }
                byte[] stablePairAddressBytes = AddressTool.getAddress(tx.getHash().getBytes(), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
                String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
                CreateStablePairData txData = new CreateStablePairData();
                txData.parse(tx.getTxData(), 0);
                LedgerAssetDTO dto = ledgerAssetRegisterHelper.lpAssetRegForStable(chainId, stablePairAddress, txData.getCoins());
                logger.info("[commit] Create Pair Info: {}-{}, symbol: {}, decimals: {}", dto.getChainId(), dto.getAssetId(), dto.getAssetSymbol(), dto.getDecimalPlace());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger logger = chain.getLogger();
            Map<String, SwapResult> swapResultMap = chain.getBatchInfo().getSwapResultMap();
            for (Transaction tx : txs) {
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                if (result == null) {
                    result = swapExecuteResultStorageService.getResult(chainId, tx.getHash());
                }
                if (!result.isSuccess()) {
                    return true;
                }
                byte[] stablePairAddressBytes = AddressTool.getAddress(tx.getHash().getBytes(), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
                String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
                CreateStablePairData txData = new CreateStablePairData();
                txData.parse(tx.getTxData(), 0);
                StableSwapPairPo pairPO = ledgerAssetRegisterHelper.deleteLpAssetForStable(chainId, stablePairAddress);
                logger.info("[rollback] Remove Stable Pair: {}-{}", pairPO.getTokenLP().getChainId(), pairPO.getTokenLP().getAssetId());
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}
