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
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.impl.AddLiquidityHandler;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.AddLiquidityBus;
import network.nerve.swap.model.dto.AddLiquidityDTO;
import network.nerve.swap.model.txdata.AddLiquidityData;
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
@Component("AddLiquidityTxProcessorV1")
public class AddLiquidityTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private AddLiquidityHandler addLiquidityHandler;

    @Override
    public int getType() {
        return TxType.SWAP_ADD_LIQUIDITY;
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
            if (tx.getType() != TxType.SWAP_ADD_LIQUIDITY) {
                logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                failsList.add(tx);
                errorCode = SwapErrorCode.DATA_ERROR.getCode();
                continue;
            }
            AddLiquidityData txData = new AddLiquidityData();
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
            CoinData coinData;
            AddLiquidityDTO dto;
            try {
                coinData = tx.getCoinDataInstance();
                dto = addLiquidityHandler.getAddLiquidityInfo(chainId, coinData);
                BigInteger[] reserves = SwapUtils.getReserves(chainId, iPairFactory, dto.getTokenA(), dto.getTokenB());
                addLiquidityHandler.calcAddLiquidity(chainId, iPairFactory, dto.getTokenA(),dto.getTokenB(),
                                dto.getUserLiquidityA(), dto.getUserLiquidityB(),
                                amountAMin, amountBMin,
                                reserves[0], reserves[1]);
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
                AddLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), AddLiquidityBus.class);

                // 更新Pair的资金池和发行总量
                if (bus.isFirstTokenA()) {
                    pair.update(bus.getLiquidity(), bus.getRealAddAmountA().add(bus.getReserveA()), bus.getRealAddAmountB().add(bus.getReserveB()), bus.getReserveA(), bus.getReserveB(), blockHeader.getHeight(), blockHeader.getTime());
                } else {
                    pair.update(bus.getLiquidity(), bus.getRealAddAmountB().add(bus.getReserveB()), bus.getRealAddAmountA().add(bus.getReserveA()), bus.getReserveB(), bus.getReserveA(), blockHeader.getHeight(), blockHeader.getTime());
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
                CoinData coinData = tx.getCoinDataInstance();
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(coinData.getTo().get(0).getAddress()));
                AddLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), AddLiquidityBus.class);
                // 回滚Pair的资金池
                if (bus.isFirstTokenA()) {
                    pair.rollback(bus.getLiquidity(), bus.getReserveA(), bus.getReserveB(), bus.getPreBlockHeight(), bus.getPreBlockTime());
                } else {
                    pair.rollback(bus.getLiquidity(), bus.getReserveB(), bus.getReserveA(), bus.getPreBlockHeight(), bus.getPreBlockTime());
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
