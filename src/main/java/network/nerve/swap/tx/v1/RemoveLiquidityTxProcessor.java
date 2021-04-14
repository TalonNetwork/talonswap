package network.nerve.swap.tx.v1;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.swap.cache.LedgerAssetCacher;
import network.nerve.swap.cache.SwapPairCacher;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.impl.RemoveLiquidityHandler;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.RemoveLiquidityBus;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.dto.RemoveLiquidityDTO;
import network.nerve.swap.model.txdata.RemoveLiquidityData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.*;

/**
 * @author Niels
 */
@Component("RemoveLiquidityTxProcessorV1")
public class RemoveLiquidityTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private RemoveLiquidityHandler removeLiquidityHandler;
    @Autowired
    private LedgerAssetCacher ledgerAssetCacher;
    @Autowired
    private SwapPairCacher swapPairCacher;

    @Override
    public int getType() {
        return TxType.SWAP_REMOVE_LIQUIDITY;
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
        for (Transaction tx : txs) {
            if (tx.getType() != TxType.SWAP_REMOVE_LIQUIDITY) {
                logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                failsList.add(tx);
                errorCode = SwapErrorCode.DATA_ERROR.getCode();
                continue;
            }
            RemoveLiquidityData txData = new RemoveLiquidityData();
            try {
                txData.parse(tx.getTxData(), 0);
            } catch (NulsException e) {
                Log.error(e);
                failsList.add(tx);
                errorCode = e.getErrorCode().getCode();
                continue;
            }
            BigInteger amountAMin = txData.getAmountAMin();
            BigInteger amountBMin = txData.getAmountBMin();
            long deadline = txData.getDeadline();
            if (blockHeader.getTime() > deadline) {
                logger.error("Tx EXPIRED! hash-{}", tx.getHash().toHex());
                failsList.add(tx);
                errorCode = SwapErrorCode.EXPIRED.getCode();
                continue;
            }
            NerveToken tokenA = new NerveToken(txData.getAssetChainIdA(), txData.getAssetIdA());
            NerveToken tokenB = new NerveToken(txData.getAssetChainIdB(), txData.getAssetIdB());

            // 检查tokenA,B是否存在，pair地址是否合法
            LedgerAssetDTO assetA = ledgerAssetCacher.getLedgerAsset(tokenA);
            LedgerAssetDTO assetB = ledgerAssetCacher.getLedgerAsset(tokenB);
            if (assetA == null || assetB == null) {
                logger.error("Ledger asset not exist! hash-{}", tx.getHash().toHex());
                failsList.add(tx);
                errorCode = SwapErrorCode.LEDGER_ASSET_NOT_EXIST.getCode();
                continue;
            }

            CoinData coinData;
            RemoveLiquidityDTO dto;
            try {
                coinData = tx.getCoinDataInstance();
                dto = removeLiquidityHandler.getRemoveLiquidityInfo(chainId, coinData);
                if (!swapPairCacher.isExist(AddressTool.getStringAddressByBytes(dto.getPairAddress()))) {
                    logger.error("PAIR_NOT_EXIST! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_NOT_EXIST.getCode();
                    continue;
                }
                if (!Arrays.equals(SwapUtils.getPairAddress(chainId, tokenA, tokenB), dto.getPairAddress())) {
                    logger.error("PAIR_INCONSISTENCY! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_INCONSISTENCY.getCode();
                    continue;
                }
                removeLiquidityHandler.calRemoveLiquidityBusiness(chainId, iPairFactory, dto.getPairAddress(), dto.getLiquidity(),
                        tokenA, tokenB, amountAMin, amountBMin);
            } catch (NulsException e) {
                Log.error(e);
                failsList.add(tx);
                errorCode = e.getErrorCode().getCode();
                continue;
            }
        }
        resultMap.put("txList", txs);
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
                // 从执行结果中提取业务数据
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                if (!result.isSuccess()) {
                    return true;
                }
                CoinData coinData = tx.getCoinDataInstance();
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(coinData.getTo().get(0).getAddress()));
                RemoveLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), RemoveLiquidityBus.class);

                // 更新Pair的资金池和发行总量
                pair.update(bus.getLiquidity().negate(), bus.getReserve0().subtract(bus.getAmount0()), bus.getReserve1().subtract(bus.getAmount1()), bus.getReserve0(), bus.getReserve1(), blockHeader.getHeight(), blockHeader.getTime());
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
                CoinData coinData = tx.getCoinDataInstance();
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(coinData.getTo().get(0).getAddress()));
                RemoveLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), RemoveLiquidityBus.class);
                // 回滚Pair的资金池
                pair.rollback(bus.getLiquidity().negate(), bus.getReserve0(), bus.getReserve1(), bus.getPreBlockHeight(), bus.getPreBlockTime());
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}
