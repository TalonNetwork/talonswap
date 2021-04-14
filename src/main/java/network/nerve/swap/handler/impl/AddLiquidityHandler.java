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
import network.nerve.swap.constant.SwapConstant;
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
import network.nerve.swap.model.business.AddLiquidityBus;
import network.nerve.swap.model.dto.AddLiquidityDTO;
import network.nerve.swap.model.dto.RealAddLiquidityOrderDTO;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.AddLiquidityData;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static network.nerve.swap.constant.SwapErrorCode.INSUFFICIENT_A_AMOUNT;
import static network.nerve.swap.constant.SwapErrorCode.INSUFFICIENT_B_AMOUNT;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class AddLiquidityHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired("TemporaryPairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private ChainManager chainManager;

    @Override
    public Integer txType() {
        return TxType.SWAP_ADD_LIQUIDITY;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        AddLiquidityDTO dto = null;
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        try {
            // 提取业务参数
            AddLiquidityData txData = new AddLiquidityData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            CoinData coinData = tx.getCoinDataInstance();
            dto = getAddLiquidityInfo(chainId, coinData);
            NerveToken tokenA = dto.getTokenA();
            NerveToken tokenB = dto.getTokenB();
            BigInteger[] reserves = SwapUtils.getReserves(chainId, iPairFactory, tokenA, tokenB);

            // 计算用户实际注入的资产，以及用户获取的LP资产
            IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(dto.getPairAddress()));
            RealAddLiquidityOrderDTO orderDTO = calcAddLiquidity(chainId, iPairFactory, tokenA, tokenB,
                    dto.getUserLiquidityA(), dto.getUserLiquidityB(),
                    txData.getAmountAMin(), txData.getAmountBMin(),
                    reserves[0], reserves[1]);
            BigInteger[] orderRealAddLiquidity = orderDTO.getRealAddLiquidity();
            BigInteger[] orderReserves = orderDTO.getReserves();
            BigInteger[] orderRefund = orderDTO.getRefund();

            BigInteger[] _realAddLiquidity;
            BigInteger[] _reserves;
            BigInteger[] _refund;
            NerveToken[] tokens = SwapUtils.tokenSort(tokenA, tokenB);
            boolean firstTokenA = tokens[0].equals(tokenA);
            if (firstTokenA) {
                _realAddLiquidity = orderRealAddLiquidity;
                _reserves = orderReserves;
                _refund = orderRefund;
            } else {
                _realAddLiquidity = new BigInteger[]{orderRealAddLiquidity[1], orderRealAddLiquidity[0]};
                _reserves = new BigInteger[]{orderReserves[1], orderReserves[0]};
                _refund = new BigInteger[]{orderRefund[1], orderRefund[0]};
            }

            // 整合计算数据
            AddLiquidityBus bus = new AddLiquidityBus(
                    firstTokenA,
                    _realAddLiquidity[0], _realAddLiquidity[1],
                    orderDTO.getLiquidity(),
                    _reserves[0], _reserves[1],
                    _refund[0], _refund[1]
            );
            bus.setPreBlockHeight(pair.getBlockHeightLast());
            bus.setPreBlockTime(pair.getBlockTimeLast());

            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // 组装系统成交交易
            NerveToken tokenLP = pair.getPair().getTokenLP();
            SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(tx.getHash().toHex(), blockTime);
            sysDeal.newTo()
                    .setToAddress(txData.getTo())
                    .setToAssetsChainId(tokenLP.getChainId())
                    .setToAssetsId(tokenLP.getAssetId())
                    .setToAmount(orderDTO.getLiquidity()).endTo();

            if (bus.getRefundAmountA().compareTo(BigInteger.ZERO) > 0) {
                sysDeal.newTo()
                        .setToAddress(dto.getFromA())
                        .setToAssetsChainId(tokenA.getChainId())
                        .setToAssetsId(tokenA.getAssetId())
                        .setToAmount(bus.getRefundAmountA()).endTo();
            }
            if (bus.getRefundAmountB().compareTo(BigInteger.ZERO) > 0) {
                sysDeal.newTo()
                        .setToAddress(dto.getFromB())
                        .setToAssetsChainId(tokenB.getChainId())
                        .setToAssetsId(tokenB.getAssetId())
                        .setToAmount(bus.getRefundAmountB()).endTo();
            }
            Transaction sysDealTx = sysDeal.build();
            result.setSubTx(sysDealTx);
            try {
                result.setSubTxStr(HexUtil.encode(sysDealTx.serialize()));
            } catch (IOException e) {
                throw new NulsException(SwapErrorCode.IO_ERROR, e);
            }
            // 更新临时余额
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx);
            // 更新临时数据
            pair.update(orderDTO.getLiquidity(), orderRealAddLiquidity[0].add(orderReserves[0]), orderRealAddLiquidity[1].add(orderReserves[1]), orderReserves[0], orderReserves[1], blockHeight, blockTime);
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
            NerveToken tokenA = dto.getTokenA();
            NerveToken tokenB = dto.getTokenB();
            SwapSystemRefundTransaction refund = new SwapSystemRefundTransaction(tx.getHash().toHex(), blockTime);
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            LedgerBalance balanceA = tempBalanceManager.getBalance(dto.getPairAddress(), tokenA.getChainId(), tokenA.getAssetId()).getData();
            LedgerBalance balanceB = tempBalanceManager.getBalance(dto.getPairAddress(), tokenB.getChainId(), tokenB.getAssetId()).getData();
            Transaction refundTx =
                refund.newFrom()
                        .setFrom(balanceA, dto.getUserLiquidityA()).endFrom()
                      .newFrom()
                        .setFrom(balanceB, dto.getUserLiquidityB()).endFrom()
                      .newTo()
                        .setToAddress(dto.getFromA())
                        .setToAssetsChainId(tokenA.getChainId())
                        .setToAssetsId(tokenA.getAssetId())
                        .setToAmount(dto.getUserLiquidityA()).endTo()
                      .newTo()
                        .setToAddress(dto.getFromB())
                        .setToAssetsChainId(tokenB.getChainId())
                        .setToAssetsId(tokenB.getAssetId())
                        .setToAmount(dto.getUserLiquidityB()).endTo()
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

    public AddLiquidityDTO getAddLiquidityInfo(int chainId, CoinData coinData) throws NulsException {
        if (coinData == null) {
            return null;
        }
        List<CoinTo> tos = coinData.getTo();
        if (tos.size() != 2) {
            throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_TOS_ERROR);
        }
        CoinTo coinToA = tos.get(0);
        CoinTo coinToB = tos.get(1);
        if (coinToA.getLockTime() != 0 || coinToB.getLockTime() != 0) {
            throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_AMOUNT_LOCK_ERROR);
        }
        NerveToken tokenA = new NerveToken(coinToA.getAssetsChainId(), coinToA.getAssetsId());
        NerveToken tokenB = new NerveToken(coinToB.getAssetsChainId(), coinToB.getAssetsId());
        byte[] pairAddress = SwapUtils.getPairAddress(chainId, tokenA, tokenB);
        if (!Arrays.equals(coinToA.getAddress(), pairAddress) || !Arrays.equals(coinToB.getAddress(), pairAddress)) {
            throw new NulsException(SwapErrorCode.PAIR_INCONSISTENCY);
        }

        List<CoinFrom> froms = coinData.getFrom();
        if (froms.size() != 2) {
            throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_FROMS_ERROR);
        }
        byte[] fromA, fromB;
        CoinFrom from0 = froms.get(0);
        CoinFrom from1 = froms.get(1);
        if (from0.getAssetsChainId() == coinToA.getAssetsChainId() && from0.getAssetsId() == coinToA.getAssetsId()) {
            fromA = from0.getAddress();
            fromB = from1.getAddress();
        } else {
            fromA = from1.getAddress();
            fromB = from0.getAddress();
        }
        return new AddLiquidityDTO(fromA, fromB, pairAddress, tokenA, tokenB, coinToA.getAmount(), coinToB.getAmount());
    }

    public RealAddLiquidityOrderDTO calcAddLiquidity(
            int chainId, IPairFactory iPairFactory,
            NerveToken tokenA,
            NerveToken tokenB,
            BigInteger amountADesired,
            BigInteger amountBDesired,
            BigInteger amountAMin,
            BigInteger amountBMin,
            BigInteger reserveA,
            BigInteger reserveB
    ) throws NulsException {
        BigInteger[] realAddLiquidity;
        BigInteger[] refund;
        if (reserveA.equals(BigInteger.ZERO) && reserveB.equals(BigInteger.ZERO)) {
            realAddLiquidity = new BigInteger[]{amountADesired, amountBDesired};
            refund = new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO};
        } else {
            BigInteger amountBOptimal = SwapUtils.quote(amountADesired, reserveA, reserveB);
            if (amountBOptimal.compareTo(amountBDesired) <= 0) {
                if (amountBOptimal.compareTo(amountBMin) < 0) {
                    throw new NulsException(INSUFFICIENT_B_AMOUNT);
                }
                realAddLiquidity = new BigInteger[] {amountADesired, amountBOptimal};
                refund = new BigInteger[]{BigInteger.ZERO, amountBDesired.subtract(amountBOptimal)};
            } else {
                BigInteger amountAOptimal = SwapUtils.quote(amountBDesired, reserveB, reserveA);
                if (amountAOptimal.compareTo(amountADesired) > 0) {
                    throw new NulsException(INSUFFICIENT_A_AMOUNT);
                }
                if (amountAOptimal.compareTo(amountAMin) < 0) {
                    throw new NulsException(INSUFFICIENT_A_AMOUNT);
                }
                realAddLiquidity = new BigInteger[] {amountAOptimal, amountBDesired};
                refund = new BigInteger[]{amountADesired.subtract(amountAOptimal), BigInteger.ZERO};
            }
        }
        NerveToken[] tokens = SwapUtils.tokenSort(tokenA, tokenB);
        realAddLiquidity = tokens[0].equals(tokenA) ? realAddLiquidity : new BigInteger[]{realAddLiquidity[1], realAddLiquidity[0]};
        BigInteger[] reserves = tokens[0].equals(tokenA) ? new BigInteger[]{reserveA, reserveB} : new BigInteger[]{reserveB, reserveA};

        // 计算用户获取的LP资产
        //TODO pierre 账本改造，获取amount0, amount1使用账本接口
        IPair pair = iPairFactory.getPair(SwapUtils.getStringPairAddress(chainId, tokenA, tokenB));
        BigInteger totalSupply = pair.totalSupply();
        BigInteger liquidity;
        if (totalSupply.equals(BigInteger.ZERO)) {
            liquidity = realAddLiquidity[0].multiply(realAddLiquidity[1]).sqrt().subtract(SwapConstant.MINIMUM_LIQUIDITY);
        } else {
            BigInteger _liquidity0 = realAddLiquidity[0].multiply(totalSupply).divide(reserves[0]);
            BigInteger _liquidity1 = realAddLiquidity[1].multiply(totalSupply).divide(reserves[1]);
            liquidity = _liquidity0.compareTo(_liquidity1) < 0 ? _liquidity0 : _liquidity1;
        }
        if (liquidity.compareTo(BigInteger.ZERO) < 0) {
            throw new NulsException(SwapErrorCode.INSUFFICIENT_LIQUIDITY_MINTED);
        }

        return new RealAddLiquidityOrderDTO(realAddLiquidity, reserves, refund, liquidity);
    }
}