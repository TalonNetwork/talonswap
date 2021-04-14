/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.swap.handler.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import network.nerve.swap.cache.LedgerAssetCacher;
import network.nerve.swap.cache.SwapPairCacher;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.RemoveLiquidityBus;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.dto.RemoveLiquidityDTO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.RemoveLiquidityData;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static network.nerve.swap.constant.SwapErrorCode.*;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class RemoveLiquidityHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired("TemporaryPairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private SwapPairCacher swapPairCacher;
    @Autowired
    private LedgerAssetCacher ledgerAssetCacher;

    @Override
    public Integer txType() {
        return TxType.SWAP_REMOVE_LIQUIDITY;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        RemoveLiquidityDTO dto = null;
        try {
            // 提取业务参数
            RemoveLiquidityData txData = new RemoveLiquidityData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            NerveToken tokenA = new NerveToken(txData.getAssetChainIdA(), txData.getAssetIdA());
            NerveToken tokenB = new NerveToken(txData.getAssetChainIdB(), txData.getAssetIdB());
            // 检查tokenA,B是否存在，pair地址是否合法
            LedgerAssetDTO assetA = ledgerAssetCacher.getLedgerAsset(tokenA);
            LedgerAssetDTO assetB = ledgerAssetCacher.getLedgerAsset(tokenB);
            if (assetA == null || assetB == null) {
                throw new NulsException(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
            }

            CoinData coinData = tx.getCoinDataInstance();
            dto = getRemoveLiquidityInfo(chainId, coinData);
            if (!swapPairCacher.isExist(AddressTool.getStringAddressByBytes(dto.getPairAddress()))) {
                throw new NulsException(SwapErrorCode.PAIR_NOT_EXIST);
            }
            if (!Arrays.equals(SwapUtils.getPairAddress(chainId, tokenA, tokenB), dto.getPairAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_INCONSISTENCY);
            }
            // 销毁的LP资产
            BigInteger liquidity = dto.getLiquidity();

            // 整合计算数据
            RemoveLiquidityBus bus = calRemoveLiquidityBusiness(chainId, iPairFactory, dto.getPairAddress(), liquidity,
                    tokenA, tokenB, txData.getAmountAMin(), txData.getAmountBMin());

            IPair pair = bus.getPair();
            NerveToken token0 = bus.getToken0();
            NerveToken token1 = bus.getToken1();
            BigInteger amount0 = bus.getAmount0();
            BigInteger amount1 = bus.getAmount1();

            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // 组装系统成交交易
            NerveToken tokenLP = pair.getPair().getTokenLP();
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            LedgerBalance ledgerBalance0 = tempBalanceManager.getBalance(dto.getPairAddress(), token0.getChainId(), token0.getAssetId()).getData();
            LedgerBalance ledgerBalance1 = tempBalanceManager.getBalance(dto.getPairAddress(), token1.getChainId(), token1.getAssetId()).getData();
            LedgerBalance ledgerBalanceLp = tempBalanceManager.getBalance(dto.getPairAddress(), tokenLP.getChainId(), tokenLP.getAssetId()).getData();

            SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(tx.getHash().toHex(), blockTime);
            sysDeal.newFrom()
                     .setFrom(ledgerBalance0, amount0).endFrom()
                   .newFrom()
                     .setFrom(ledgerBalance1, amount1).endFrom()
                   .newFrom()
                     .setFrom(ledgerBalanceLp, liquidity).endFrom()
                   .newTo()
                     .setToAddress(txData.getTo())
                     .setToAssetsChainId(token0.getChainId())
                     .setToAssetsId(token0.getAssetId())
                     .setToAmount(amount0).endTo()
                   .newTo()
                     .setToAddress(txData.getTo())
                     .setToAssetsChainId(token1.getChainId())
                     .setToAssetsId(token1.getAssetId())
                     .setToAmount(amount1).endTo();

            Transaction sysDealTx = sysDeal.build();
            result.setSubTx(sysDealTx);
            try {
                result.setSubTxStr(HexUtil.encode(sysDealTx.serialize()));
            } catch (IOException e) {
                throw new NulsException(SwapErrorCode.IO_ERROR, e);
            }
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx);
            // 更新临时数据
            BigInteger balance0 = bus.getReserve0().subtract(amount0);
            BigInteger balance1 = bus.getReserve1().subtract(amount1);
            pair.update(liquidity.negate(), balance0, balance1, bus.getReserve0(), bus.getReserve1(), blockHeight, blockTime);


        } catch (NulsException e) {
            Log.error(e);
            // 装填失败的执行结果
            result.setTxType(txType());
            result.setSuccess(false);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setErrorMessage(e.format());

            // 组装系统退还交易
            IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(dto.getPairAddress()));
            NerveToken tokenLP = pair.getPair().getTokenLP();
            SwapSystemRefundTransaction refund = new SwapSystemRefundTransaction(tx.getHash().toHex(), blockTime);
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            LedgerBalance ledgerBalanceLp = tempBalanceManager.getBalance(dto.getPairAddress(), tokenLP.getChainId(), tokenLP.getAssetId()).getData();
            Transaction refundTx =
                refund.newFrom()
                        .setFrom(ledgerBalanceLp, dto.getLiquidity()).endFrom()
                      .newTo()
                        .setToAddress(dto.getUserAddress())
                        .setToAssetsChainId(tokenLP.getChainId())
                        .setToAssetsId(tokenLP.getAssetId())
                        .setToAmount(dto.getLiquidity()).endTo()
                      .build();
            result.setSubTx(refundTx);
            try {
                String refundTxStr = HexUtil.encode(refundTx.serialize());
                result.setSubTxStr(refundTxStr);
            } catch (IOException e2) {
                throw new NulsRuntimeException(SwapErrorCode.IO_ERROR, e2);
            }
            // 更新临时余额
            try {
                tempBalanceManager.refreshTempBalance(chainId, refundTx);
            } catch (NulsException e3) {
                throw new NulsRuntimeException(e3.getErrorCode(), e3);
            }
        }
        batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        return result;
    }

    public RemoveLiquidityDTO getRemoveLiquidityInfo(int chainId, CoinData coinData) throws NulsException {
        if (coinData == null) {
            return null;
        }
        List<CoinTo> tos = coinData.getTo();
        if (tos.size() != 1) {
            throw new NulsException(SwapErrorCode.REMOVE_LIQUIDITY_TOS_ERROR);
        }
        CoinTo to = tos.get(0);
        if (to.getLockTime() != 0) {
            throw new NulsException(SwapErrorCode.REMOVE_LIQUIDITY_AMOUNT_LOCK_ERROR);
        }
        byte[] pairAddress = to.getAddress();

        List<CoinFrom> froms = coinData.getFrom();
        if (froms.size() != 1) {
            throw new NulsException(SwapErrorCode.REMOVE_LIQUIDITY_FROMS_ERROR);
        }
        CoinFrom from = froms.get(0);
        byte[] userAddress = from.getAddress();
        return new RemoveLiquidityDTO(userAddress, pairAddress, to.getAmount());
    }

    public RemoveLiquidityBus calRemoveLiquidityBusiness(
            int chainId, IPairFactory iPairFactory,
            byte[] pairAddress, BigInteger liquidity,
            NerveToken tokenA,
            NerveToken tokenB,
            BigInteger amountAMin,
            BigInteger amountBMin) throws NulsException {
        IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairAddress));
        BigInteger[] reserves = pair.getReserves();
        SwapPairPO pairPO = pair.getPair();
        NerveToken token0 = pairPO.getToken0();
        NerveToken token1 = pairPO.getToken1();
        //TODO pierre 账本改造，获取balance0, balance1使用账本接口
        BigInteger balance0 = reserves[0];
        BigInteger balance1 = reserves[1];
        BigInteger totalSupply = pair.totalSupply();
        // 可赎回的资产
        BigInteger amount0 = liquidity.multiply(balance0).divide(totalSupply);
        BigInteger amount1 = liquidity.multiply(balance1).divide(totalSupply);
        if (amount0.compareTo(BigInteger.ZERO) <= 0 || amount1.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsException(INSUFFICIENT_LIQUIDITY_BURNED);
        }

        boolean firstTokenA = tokenA.equals(token0);
        BigInteger amountA, amountB;
        if (firstTokenA) {
            amountA = amount0;
            amountB = amount1;
        } else {
            amountA = amount1;
            amountB = amount0;
        }
        if (amountA.compareTo(amountAMin) < 0) {
            throw new NulsException(INSUFFICIENT_A_AMOUNT);
        }
        if (amountB.compareTo(amountBMin) < 0) {
            throw new NulsException(INSUFFICIENT_B_AMOUNT);
        }
        RemoveLiquidityBus bus = new RemoveLiquidityBus(amount0, amount1, balance0, balance1, liquidity, pair, token0, token1);
        bus.setPreBlockHeight(pair.getBlockHeightLast());
        bus.setPreBlockTime(pair.getBlockTimeLast());
        return bus;
    }

}
