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

import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.SwapTempPairManager;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.txdata.CreatePairData;
import network.nerve.swap.utils.SwapUtils;

import static network.nerve.swap.constant.SwapErrorCode.IDENTICAL_ADDRESSES;
import static network.nerve.swap.constant.SwapErrorCode.PAIR_ALREADY_EXISTS;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class CreatePairHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired
    private ChainManager chainManager;

    @Override
    public Integer txType() {
        return TxType.CREATE_SWAP_PAIR;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        try {
            // 提取业务参数
            CreatePairData txData = new CreatePairData();
            txData.parse(tx.getTxData(), 0);
            if (txData.getToken0().equals(txData.getToken1())) {
                throw new NulsException(IDENTICAL_ADDRESSES);
            }
            String address = SwapUtils.getStringPairAddress(chainId, txData.getToken0(), txData.getToken1());
            SwapTempPairManager swapTempPairManager = batchInfo.getSwapTempPairManager();
            if (swapTempPairManager.isExist(address)) {
                throw new NulsException(PAIR_ALREADY_EXISTS);
            }
            // 保存临时创建的交易对
            swapTempPairManager.add(address);
            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);

        } catch (NulsException e) {
            Log.error(e);
            // 装填失败的执行结果
            result.setTxType(txType());
            result.setSuccess(false);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setErrorMessage(e.format());
        }
        batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        return result;
    }


}
