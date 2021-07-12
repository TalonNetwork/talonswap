package network.nerve.swap.tx.v1.stable;

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
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.impl.stable.StableSwapTradeHandler;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;
import network.nerve.swap.model.dto.stable.StableSwapTradeDTO;
import network.nerve.swap.model.txdata.stable.StableSwapTradeData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
@Component("StableSwapTradeTxProcessorV1")
public class StableSwapTradeTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private StableSwapTradeHandler stableSwapTradeHandler;

    @Override
    public int getType() {
        return TxType.SWAP_TRADE_STABLE_COIN;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = chainManager.getChain(chainId);
        if (blockHeader == null) blockHeader = chain.getLatestBasicBlock().toBlockHeader();

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
            if (tx.getType() != getType()) {
                logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                failsList.add(tx);
                errorCode = SwapErrorCode.DATA_ERROR.getCode();
                continue;
            }
            StableSwapTradeData txData = new StableSwapTradeData();
            try {
                txData.parse(tx.getTxData(), 0);
            } catch (NulsException e) {
                Log.error(e);
                failsList.add(tx);
                errorCode = e.getErrorCode().getCode();
                continue;
            }
            CoinData coinData;
            StableSwapTradeDTO dto;
            try {
                byte tokenOutIndex = txData.getTokenOutIndex();
                coinData = tx.getCoinDataInstance();
                dto = stableSwapTradeHandler.getStableSwapTradeInfo(chainId, coinData, iPairFactory, tokenOutIndex);
                SwapUtils.calStableSwapTradeBusiness(chainId, iPairFactory, dto.getAmountsIn(), tokenOutIndex, dto.getPairAddress(), txData.getTo(), txData.getFeeTo());
            } catch (NulsException e) {
                Log.error(e);
                failsList.add(tx);
                errorCode = e.getErrorCode().getCode();
                continue;
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
            if (swapResultMap == null) {
                return true;
            }
            for (Transaction tx : txs) {
                logger.info("[commit] Stable Swap Trade, hash: {}", tx.getHash().toHex());
                // 从执行结果中提取业务数据
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                StableSwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableSwapTradeBus.class);
                CoinData coinData = tx.getCoinDataInstance();
                StableSwapTradeDTO dto = stableSwapTradeHandler.getStableSwapTradeInfo(chainId, coinData, iPairFactory, bus.getTokenOutIndex());
                String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
                IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
                // 更新Pair的资金池和发行总量
                stablePair.update(dto.getUserAddress(), BigInteger.ZERO, bus.getChangeBalances(), bus.getBalances(), blockHeader.getHeight(), blockHeader.getTime());
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
            for (Transaction tx : txs) {
                SwapResult result = swapExecuteResultStorageService.getResult(chainId, tx.getHash());
                if (result == null) {
                    continue;
                }
                if (!result.isSuccess()) {
                    continue;
                }
                StableSwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableSwapTradeBus.class);
                CoinData coinData = tx.getCoinDataInstance();
                StableSwapTradeDTO dto = stableSwapTradeHandler.getStableSwapTradeInfo(chainId, coinData, iPairFactory, bus.getTokenOutIndex());
                String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
                IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
                // 回滚Pair的资金池
                stablePair.rollback(dto.getUserAddress(), BigInteger.ZERO, bus.getChangeBalances(), bus.getBalances(), bus.getPreBlockHeight(), bus.getPreBlockTime());
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
                logger.info("[rollback] Stable Swap Trade, hash: {}", tx.getHash().toHex());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

}
