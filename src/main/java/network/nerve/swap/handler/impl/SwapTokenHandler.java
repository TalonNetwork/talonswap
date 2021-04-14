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
import network.nerve.swap.model.business.SwapTradeBus;
import network.nerve.swap.model.business.TradePairBus;
import network.nerve.swap.model.dto.SwapTradeDTO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.SwapTradeData;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static network.nerve.swap.constant.SwapConstant.*;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class SwapTokenHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired("TemporaryPairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private SwapPairCacher swapPairCacher;

    @Override
    public Integer txType() {
        return TxType.SWAP_TRADE;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        SwapTradeDTO dto = null;
        try {
            // 提取业务参数
            SwapTradeData txData = new SwapTradeData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            CoinData coinData = tx.getCoinDataInstance();
            dto = getSwapTradeInfo(chainId, coinData);
            // 用户卖出的资产数量
            BigInteger amountIn = dto.getAmountIn();

            // 整合计算数据
            SwapTradeBus bus = calSwapTradeBusiness(chainId, iPairFactory, amountIn,
                    txData.getTo(), txData.getPath(), txData.getAmountOutMin());
            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // 组装系统成交交易
            List<TradePairBus> busList = bus.getTradePairBuses();
            SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(tx.getHash().toHex(), blockTime);
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            for (TradePairBus pairBus : busList) {
                NerveToken tokenOut = pairBus.getTokenOut();
                BigInteger amountOut = pairBus.getAmountOut();
                LedgerBalance ledgerBalanceOut = tempBalanceManager.getBalance(dto.getPairAddress(), tokenOut.getChainId(), tokenOut.getAssetId()).getData();
                sysDeal.newFrom()
                        .setFrom(ledgerBalanceOut, amountOut).endFrom()
                       .newTo()
                        .setToAddress(txData.getTo())
                        .setToAssetsChainId(tokenOut.getChainId())
                        .setToAssetsId(tokenOut.getAssetId())
                        .setToAmount(amountOut).endTo();
            }
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
            for (TradePairBus pairBus : busList) {
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                pair.update(BigInteger.ZERO, pairBus.getBalance0(), pairBus.getBalance1(), pairBus.getReserve0(), pairBus.getReserve1(), blockHeight, blockTime);
            }
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
            SwapSystemRefundTransaction refund = new SwapSystemRefundTransaction(tx.getHash().toHex(), blockTime);
            NerveToken tokenIn = dto.getTokenIn();
            BigInteger amountIn = dto.getAmountIn();
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            LedgerBalance ledgerBalanceIn = tempBalanceManager.getBalance(dto.getPairAddress(), tokenIn.getChainId(), tokenIn.getAssetId()).getData();
            Transaction refundTx =
                    refund.newFrom()
                            .setFrom(ledgerBalanceIn, amountIn).endFrom()
                          .newTo()
                            .setToAddress(dto.getUserAddress())
                            .setToAssetsChainId(tokenIn.getChainId())
                            .setToAssetsId(tokenIn.getAssetId())
                            .setToAmount(amountIn).endTo()
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

    public SwapTradeDTO getSwapTradeInfo(int chainId, CoinData coinData) throws NulsException {
        if (coinData == null) {
            return null;
        }
        List<CoinTo> tos = coinData.getTo();
        if (tos.size() != 1) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_TOS_ERROR);
        }
        CoinTo to = tos.get(0);
        if (to.getLockTime() != 0) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_AMOUNT_LOCK_ERROR);
        }
        byte[] pairAddress = to.getAddress();

        List<CoinFrom> froms = coinData.getFrom();
        if (froms.size() != 1) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_FROMS_ERROR);
        }
        CoinFrom from = froms.get(0);
        byte[] userAddress = from.getAddress();
        BigInteger amountIn = to.getAmount();
        return new SwapTradeDTO(userAddress, pairAddress, new NerveToken(to.getAssetsChainId(), to.getAssetsId()), amountIn);
    }

    public SwapTradeBus calSwapTradeBusiness(
            int chainId, IPairFactory iPairFactory,
            BigInteger _amountIn, byte[] _to,
            NerveToken[] path,
            BigInteger amountOutMin) throws NulsException {
        BigInteger[] amounts = SwapUtils.getAmountsOut(chainId, iPairFactory, _amountIn, path);
        if (amounts[amounts.length - 1].compareTo(amountOutMin) < 0) {
            throw new NulsException(SwapErrorCode.INSUFFICIENT_OUTPUT_AMOUNT);
        }

        //TODO pierre 账本改造，获取交易对中token余额balance0, balance1使用账本接口
        //TODO pierre 考虑非正规转入pair地址的token，以及本模块连续交易造成的token余额变化
        //TODO pierre 计算amountIn和amountOut时，要考虑以上因素

        List<TradePairBus> list = new ArrayList<>();
        int length = path.length;
        for (int i = 0; i < length - 1; i++) {
            NerveToken input = path[i];
            NerveToken output = path[i + 1];
            NerveToken[] tokens = SwapUtils.tokenSort(input, output);
            NerveToken token0 = tokens[0];
            BigInteger amountIn = amounts[i];
            BigInteger amountOut = amounts[i + 1];
            BigInteger amount0Out, amount1Out, amount0In, amount1In;
            if (input.equals(token0)) {
                amount0In = amountIn;
                amount1In = BigInteger.ZERO;
                amount0Out = BigInteger.ZERO;
                amount1Out = amountOut;
            } else {
                amount0In = BigInteger.ZERO;
                amount1In = amountIn;
                amount0Out = amountOut;
                amount1Out = BigInteger.ZERO;
            }
            byte[] to = i < length - 2 ? SwapUtils.getPairAddress(chainId, output, path[i + 2]) : _to;
            IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(SwapUtils.getPairAddress(chainId, input, output)));

            SwapPairPO pairPO = pair.getPair();
            NerveToken _token0 = pairPO.getToken0();
            NerveToken _token1 = pairPO.getToken1();
            BigInteger[] reserves = pair.getReserves();
            BigInteger _reserve0 = reserves[0];
            BigInteger _reserve1 = reserves[1];
            if (amount0Out.compareTo(_reserve0) >= 0 || amount1Out.compareTo(_reserve1) >= 0) {
                throw new NulsException(SwapErrorCode.INSUFFICIENT_LIQUIDITY);
            }
            if (to.equals(_token0) || to.equals(_token1)) {
                throw new NulsException(SwapErrorCode.INVALID_TO);
            }
            BigInteger balance0 = _reserve0.add(amount0In).subtract(amount0Out);
            BigInteger balance1 = _reserve1.add(amount1In).subtract(amount1Out);
            BigInteger balance0Adjusted = balance0.multiply(BI_1000).subtract(amount0In.multiply(BI_3));
            BigInteger balance1Adjusted = balance1.multiply(BI_1000).subtract(amount1In.multiply(BI_3));
            if (balance0Adjusted.multiply(balance1Adjusted).compareTo(_reserve0.multiply(_reserve1).multiply(BI_1000_000)) < 0) {
                throw new NulsException(SwapErrorCode.K);
            }

            // 组装业务数据
            TradePairBus _bus = new TradePairBus(pairPO.getAddress(), balance0, balance1, _reserve0, _reserve1, input, amountIn, output, amountOut, to);
            _bus.setPreBlockHeight(pair.getBlockHeightLast());
            _bus.setPreBlockTime(pair.getBlockTimeLast());
            list.add(_bus);
        }
        SwapTradeBus bus = new SwapTradeBus(list);
        return bus;
    }

}
