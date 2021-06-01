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
import io.nuls.core.log.Log;
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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

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
            CoinData coinData = tx.getCoinDataInstance();
            dto = getAddLiquidityInfo(chainId, coinData);
            // 提取业务参数
            AddLiquidityData txData = new AddLiquidityData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            NerveToken tokenA = dto.getTokenA();
            NerveToken tokenB = dto.getTokenB();

            // 计算用户实际注入的资产，以及用户获取的LP资产
            IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(dto.getPairAddress()));
            RealAddLiquidityOrderDTO orderDTO = SwapUtils.calcAddLiquidity(chainId, iPairFactory, tokenA, tokenB,
                    dto.getUserLiquidityA(), dto.getUserLiquidityB(),
                    txData.getAmountAMin(), txData.getAmountBMin());

            BigInteger[] _realAddLiquidity;
            BigInteger[] _reserves;
            BigInteger[] _refund;
            NerveToken[] tokens = SwapUtils.tokenSort(tokenA, tokenB);
            boolean firstTokenA = tokens[0].equals(tokenA);
            if (firstTokenA) {
                _realAddLiquidity = new BigInteger[]{orderDTO.getRealAddLiquidityA(), orderDTO.getRealAddLiquidityB()};
                _reserves = new BigInteger[]{orderDTO.getReservesA(), orderDTO.getReservesB()};
                _refund = new BigInteger[]{orderDTO.getRefundA(), orderDTO.getRefundB()};
            } else {
                _realAddLiquidity = new BigInteger[]{orderDTO.getRealAddLiquidityB(), orderDTO.getRealAddLiquidityA()};
                _reserves = new BigInteger[]{orderDTO.getReservesB(), orderDTO.getReservesA()};
                _refund = new BigInteger[]{orderDTO.getRefundB(), orderDTO.getRefundA()};
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
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTx(bus, dto, tx.getHash().toHex(), tokenA, tokenB, tokenLP, txData.getTo(), blockTime, tempBalanceManager);

            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // 更新临时数据
            pair.update(orderDTO.getLiquidity(), _realAddLiquidity[0].add(_reserves[0]), _realAddLiquidity[1].add(_reserves[1]), _reserves[0], _reserves[1], blockHeight, blockTime);
        } catch (Exception e) {
            Log.error(e);
            // 装填失败的执行结果
            result.setTxType(txType());
            result.setSuccess(false);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setErrorMessage(e instanceof NulsException ? ((NulsException) e).format() : e.getMessage());

            if (dto == null) {
                return result;
            }
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
            String refundTxStr = SwapUtils.nulsData2Hex(refundTx);
            result.setSubTxStr(refundTxStr);
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, refundTx, blockTime);
        } finally {
            batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        }
        return result;
    }

    private Transaction makeSystemDealTx(AddLiquidityBus bus, AddLiquidityDTO dto, String orginTxHash, NerveToken tokenA, NerveToken tokenB, NerveToken tokenLP, byte[] to, long blockTime, LedgerTempBalanceManager tempBalanceManager) {
        SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(orginTxHash, blockTime);
        sysDeal.newTo()
                .setToAddress(to)
                .setToAssetsChainId(tokenLP.getChainId())
                .setToAssetsId(tokenLP.getAssetId())
                .setToAmount(bus.getLiquidity()).endTo();

        if (bus.getRefundAmountA().compareTo(BigInteger.ZERO) > 0) {
            LedgerBalance balanceA = tempBalanceManager.getBalance(dto.getPairAddress(), tokenA.getChainId(), tokenA.getAssetId()).getData();
            sysDeal.newFrom()
                    .setFrom(balanceA, bus.getRefundAmountA()).endFrom();
            sysDeal.newTo()
                    .setToAddress(dto.getFromA())
                    .setToAssetsChainId(tokenA.getChainId())
                    .setToAssetsId(tokenA.getAssetId())
                    .setToAmount(bus.getRefundAmountA()).endTo();
        }
        if (bus.getRefundAmountB().compareTo(BigInteger.ZERO) > 0) {
            LedgerBalance balanceB = tempBalanceManager.getBalance(dto.getPairAddress(), tokenB.getChainId(), tokenB.getAssetId()).getData();
            sysDeal.newFrom()
                    .setFrom(balanceB, bus.getRefundAmountB()).endFrom();
            sysDeal.newTo()
                    .setToAddress(dto.getFromB())
                    .setToAssetsChainId(tokenB.getChainId())
                    .setToAssetsId(tokenB.getAssetId())
                    .setToAmount(bus.getRefundAmountB()).endTo();
        }
        Transaction sysDealTx = sysDeal.build();
        return sysDealTx;
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


}
