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
import network.nerve.swap.handler.impl.SwapTokenHandler;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.SwapTradeBus;
import network.nerve.swap.model.business.TradePairBus;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.dto.SwapTradeDTO;
import network.nerve.swap.model.txdata.SwapTradeData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.*;

/**
 * @author Niels
 */
@Component("SwapTradeTxProcessorV1")
public class SwapTradeTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private SwapTokenHandler swapTokenHandler;
    @Autowired
    private LedgerAssetCacher ledgerAssetCacher;
    @Autowired
    private SwapPairCacher swapPairCacher;

    @Override
    public int getType() {
        return TxType.SWAP_TRADE;
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
            if (tx.getType() != TxType.SWAP_TRADE) {
                logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                failsList.add(tx);
                errorCode = SwapErrorCode.DATA_ERROR.getCode();
                continue;
            }
            SwapTradeData txData = new SwapTradeData();
            try {
                txData.parse(tx.getTxData(), 0);
            } catch (NulsException e) {
                Log.error(e);
                failsList.add(tx);
                errorCode = e.getErrorCode().getCode();
                continue;
            }
            long deadline = txData.getDeadline();
            if (blockHeader.getTime() > deadline) {
                logger.error("Tx EXPIRED! hash-{}", tx.getHash().toHex());
                failsList.add(tx);
                errorCode = SwapErrorCode.EXPIRED.getCode();
                continue;
            }
            NerveToken[] path = txData.getPath();
            int length = path.length;
            for (int i=0;i<length;i++) {
                NerveToken token = path[i];
                LedgerAssetDTO asset = ledgerAssetCacher.getLedgerAsset(token);
                if (asset == null) {
                    logger.error("Ledger asset not exist! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.LEDGER_ASSET_NOT_EXIST.getCode();
                    continue C1;
                }
                if (i == length - 1) {
                    continue;
                }
                if (!swapPairCacher.isExist(SwapUtils.getStringPairAddress(chainId, token, path[i + 1]))) {
                    logger.error("PAIR_ADDRESS_ERROR! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_ADDRESS_ERROR.getCode();
                    continue C1;
                }
            }

            CoinData coinData;
            SwapTradeDTO dto;
            try {
                coinData = tx.getCoinDataInstance();
                dto = swapTokenHandler.getSwapTradeInfo(chainId, coinData);
                if (!swapPairCacher.isExist(AddressTool.getStringAddressByBytes(dto.getPairAddress()))) {
                    logger.error("PAIR_NOT_EXIST! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_NOT_EXIST.getCode();
                    continue;
                }
                if (!Arrays.equals(SwapUtils.getPairAddress(chainId, path[0], path[1]), dto.getPairAddress())) {
                    logger.error("PAIR_INCONSISTENCY! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_INCONSISTENCY.getCode();
                    continue;
                }
                swapTokenHandler.calSwapTradeBusiness(chainId, iPairFactory, dto.getAmountIn(),
                        txData.getTo(), txData.getPath(), txData.getAmountOutMin());
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
                SwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), SwapTradeBus.class);

                // 更新Pair的资金池和发行总量
                List<TradePairBus> busList = bus.getTradePairBuses();
                for (TradePairBus pairBus : busList) {
                    IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                    pair.update(BigInteger.ZERO, pairBus.getBalance0(), pairBus.getBalance1(), pairBus.getReserve0(), pairBus.getReserve1(), blockHeader.getHeight(), blockHeader.getTime());
                }
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
                SwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), SwapTradeBus.class);
                // 回滚Pair的资金池
                List<TradePairBus> busList = bus.getTradePairBuses();
                for (TradePairBus pairBus : busList) {
                    IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                    pair.rollback(BigInteger.ZERO, pairBus.getReserve0(), pairBus.getReserve1(), bus.getPreBlockHeight(), bus.getPreBlockTime());
                }
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}
