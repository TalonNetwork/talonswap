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
package network.nerve.swap.service.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.SwapInvoker;
import network.nerve.swap.manager.*;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.service.SwapService;
import network.nerve.swap.utils.SwapDBUtil;

import java.util.*;

/**
 * @author: PierreLuo
 * @date: 2021/4/15
 */
@Component
public class SwapServiceImpl implements SwapService {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private SwapInvoker swapInvoker;

    @Override
    public Result begin(int chainId, long blockHeight, long blockTime, String preStateRoot) {
        Chain chain = chainManager.getChain(chainId);
        BatchInfo batchInfo = new BatchInfo();
        batchInfo.setPreStateRoot(preStateRoot);
        // 初始化批量执行基本数据
        chain.setBatchInfo(batchInfo);
        // 准备临时余额
        LedgerTempBalanceManager tempBalanceManager = LedgerTempBalanceManager.newInstance(chainId);
        batchInfo.setLedgerTempBalanceManager(tempBalanceManager);
        // 准备当前区块头
        BlockHeader tempHeader = new BlockHeader();
        tempHeader.setHeight(blockHeight);
        tempHeader.setTime(blockTime);
        batchInfo.setCurrentBlockHeader(tempHeader);
        // 准备临时交易对
        SwapTempPairManager tempPairManager = SwapTempPairManager.newInstance(chainId);
        batchInfo.setSwapTempPairManager(tempPairManager);
        //准备临时Farm管理器
        FarmTempManager farmTempManager = new FarmTempManager();
        batchInfo.setFarmTempManager(farmTempManager);
        //准备临时FarmUserInfo管理器
        FarmUserInfoTempManager farmUserInfoTempManager = new FarmUserInfoTempManager();
        batchInfo.setFarmUserTempManager(farmUserInfoTempManager);
        return Result.getSuccess(null);
    }

    @Override
    public Result invokeOneByOne(int chainId, long blockHeight, long blockTime, Transaction tx) {
        try {
            SwapResult swapResult = swapInvoker.invoke(chainId, tx, blockHeight, blockTime);
            Map<String, Object> _result = new HashMap<>();
            _result.put("success", swapResult.isSuccess());
            if (null != swapResult.getSubTxStr()) {
                _result.put("txList", Arrays.asList(swapResult.getSubTxStr()));
            }
            return Result.getSuccess(_result);
        } catch (NulsException e) {
            SwapContext.logger.error(e.format(), e);
            return Result.getFailed(e.getErrorCode());
        }
    }

    @Override
    public Result end(int chainId, long blockHeight) {
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        // 缓存高度必须一致
        if (blockHeight != batchInfo.getCurrentBlockHeader().getHeight()) {
            return Result.getFailed(SwapErrorCode.BLOCK_HEIGHT_INCONSISTENCY);
        }
        Map<String, SwapResult> resultMap = batchInfo.getSwapResultMap();
        Set<Map.Entry<String, SwapResult>> entries = resultMap.entrySet();
        byte[] bytes = new byte[32 * (entries.size() + 1)];
        int i = 0;
        for (Map.Entry<String, SwapResult> entry : entries) {
            Log.info(entry.getValue().getSubTxStr());
            byte[] modelSerialize = SwapDBUtil.getModelSerialize(entry.getValue());
            byte[] hash = Sha256Hash.hash(modelSerialize);
            System.arraycopy(hash, 0, bytes, i * 32, 32);
            i++;
        }
        String preStateRoot = batchInfo.getPreStateRoot();
        System.arraycopy(HexUtil.decode(preStateRoot), 0, bytes, i * 32, 32);
        Map<String, Object> result = new HashMap<>();
        result.put("stateRoot", HexUtil.encode(Sha256Hash.hash(bytes)));
        return Result.getSuccess(result);
    }


}
