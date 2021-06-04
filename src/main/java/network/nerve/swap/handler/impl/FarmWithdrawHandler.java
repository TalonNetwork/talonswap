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
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.cache.FarmCacher;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.FarmBus;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.po.FarmUserInfoPO;
import network.nerve.swap.model.tx.FarmSystemTransaction;
import network.nerve.swap.model.txdata.FarmStakeChangeData;
import network.nerve.swap.storage.FarmUserInfoStorageService;
import network.nerve.swap.tx.v1.helpers.FarmWithdrawHelper;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class FarmWithdrawHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private FarmWithdrawHelper helper;
    @Autowired
    private FarmCacher farmCacher;
    @Autowired
    private FarmUserInfoStorageService userInfoStorageService;

    @Override
    public Integer txType() {
        return TxType.FARM_WITHDRAW;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }


    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        Chain chain = chainManager.getChain(chainId);
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        try {
            // 提取业务参数
            FarmStakeChangeData txData = new FarmStakeChangeData();
            txData.parse(tx.getTxData(), 0);
            ValidaterResult validaterResult = helper.validateTxData(chain, tx, txData, batchInfo.getFarmTempManager(), blockTime);
            if (validaterResult.isFailed()) {
                throw new NulsException(validaterResult.getErrorCode());
            }

            FarmPoolPO farm = batchInfo.getFarmTempManager().getFarm(txData.getFarmHash().toHex());
            if (farm == null) {
                FarmPoolPO realPo = farmCacher.get(txData.getFarmHash());
                farm = realPo.copy();
            }
            //处理
            executeBusiness(chain, tx, txData, farm, batchInfo, result, blockHeight, blockTime);

            batchInfo.getFarmTempManager().putFarm(farm);

            // 装填执行结果
            result.setSuccess(true);
            result.setBlockHeight(blockHeight);

        } catch (NulsException e) {
            chain.getLogger().error(e);
            // 装填失败的执行结果
            result.setSuccess(false);
            result.setErrorMessage(e.format());
        }
        result.setTxType(txType());
        result.setHash(tx.getHash().toHex());
        result.setTxTime(tx.getTime());
        result.setBlockHeight(blockHeight);
        batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        return result;
    }


    private void executeBusiness(Chain chain, Transaction tx, FarmStakeChangeData txData, FarmPoolPO farm, BatchInfo batchInfo, SwapResult result, long blockHeight, long blockTime) throws NulsException {
        FarmBus bus = new FarmBus();
        bus.setAccSyrupPerShareOld(farm.getAccSyrupPerShare());
        bus.setLastRewardBlockOld(farm.getLastRewardBlock());
        bus.setStakingBalanceOld(farm.getStakeTokenBalance());
        bus.setSyrupBalanceOld(farm.getSyrupTokenBalance());
        bus.setFarmHash(farm.getFarmHash());
        SwapUtils.updatePool(farm, blockHeight);

        byte[] address = SwapUtils.getSingleAddressFromTX(tx, chain.getChainId(), false);
        bus.setUserAddress(address);
        //获取用户状态数据
        FarmUserInfoPO user = batchInfo.getFarmUserTempManager().getUserInfo(farm.getFarmHash(), address);
        if (null == user) {
            user = this.userInfoStorageService.load(chain.getChainId(), farm.getFarmHash(), address);
        }
        if (null == user) {
            throw new NulsException(SwapErrorCode.FARM_NERVE_STAKE_ERROR);
        }
        bus.setUserAmountOld(user.getAmount());
        bus.setUserRewardDebtOld(user.getRewardDebt());
        //生成领取奖励的交易
//        uint256 pending = user.amount.mul(pool.accSushiPerShare).div(1e12).sub(user.rewardDebt);
        BigInteger reward = user.getAmount().multiply(farm.getAccSyrupPerShare()).divide(SwapConstant.BI_1E12).subtract(user.getRewardDebt());
        farm.setSyrupTokenBalance(farm.getSyrupTokenBalance().subtract(reward));

        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        Transaction subTx = transferReward(chain.getChainId(), farm, address, reward, tx, blockTime, txData.getAmount(), tempBalanceManager);
        result.setSubTx(subTx);
        try {
            result.setSubTxStr(HexUtil.encode(subTx.serialize()));
        } catch (IOException e) {
            throw new NulsException(SwapErrorCode.IO_ERROR, e);
        }
        farm.setStakeTokenBalance(farm.getStakeTokenBalance().subtract(txData.getAmount()));
        //更新池子信息
        batchInfo.getFarmTempManager().putFarm(farm);
        //更新用户状态数据
        user.setAmount(user.getAmount().subtract(txData.getAmount()));
        user.setRewardDebt(user.getAmount().multiply(farm.getAccSyrupPerShare()).divide(SwapConstant.BI_1E12));

        bus.setAccSyrupPerShareNew(farm.getAccSyrupPerShare());
        bus.setLastRewardBlockNew(farm.getLastRewardBlock());
        bus.setStakingBalanceNew(farm.getStakeTokenBalance());
        bus.setSyrupBalanceNew(farm.getSyrupTokenBalance());
        bus.setUserAmountNew(user.getAmount());
        bus.setUserRewardDebtNew(user.getRewardDebt());
        result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
        batchInfo.getFarmUserTempManager().putUserInfo(user);
    }

    private Transaction transferReward(int chainId, FarmPoolPO farm, byte[] address, BigInteger reward, Transaction tx, long blockTime, BigInteger withdrawAmount, LedgerTempBalanceManager tempBalanceManager) {
        FarmSystemTransaction sysWithdrawTx = new FarmSystemTransaction(tx.getHash().toHex(), blockTime);

        LedgerBalance balance = tempBalanceManager.getBalance(SwapUtils.getFarmAddress(chainId), farm.getStakeToken().getChainId(), farm.getStakeToken().getAssetId()).getData();

        if (farm.getStakeToken().getChainId() == farm.getSyrupToken().getChainId() && farm.getStakeToken().getAssetId() == farm.getSyrupToken().getAssetId()) {
            BigInteger amount = reward.add(withdrawAmount);

            sysWithdrawTx.newFrom().setFrom(balance, amount).endFrom();
            sysWithdrawTx.newTo()
                    .setToAddress(address)
                    .setToAssetsChainId(farm.getStakeToken().getChainId())
                    .setToAssetsId(farm.getStakeToken().getAssetId())
                    .setToAmount(amount).endTo();
            return sysWithdrawTx.build();
        }
        sysWithdrawTx.newFrom().setFrom(balance, withdrawAmount).endFrom();
        sysWithdrawTx.newTo()
                .setToAddress(address)
                .setToAssetsChainId(farm.getStakeToken().getChainId())
                .setToAssetsId(farm.getStakeToken().getAssetId())
                .setToAmount(withdrawAmount).endTo();
        if (reward.compareTo(BigInteger.ZERO) > 0) {
            LedgerBalance balance1 = tempBalanceManager.getBalance(SwapUtils.getFarmAddress(chainId), farm.getSyrupToken().getChainId(), farm.getSyrupToken().getAssetId()).getData();
            sysWithdrawTx.newFrom().setFrom(balance1, reward).endFrom();
            sysWithdrawTx.newTo()
                    .setToAddress(address)
                    .setToAssetsChainId(farm.getSyrupToken().getChainId())
                    .setToAssetsId(farm.getSyrupToken().getAssetId())
                    .setToAmount(reward).endTo();
        }
        return sysWithdrawTx.build();
    }

    public ChainManager getChainManager() {
        return chainManager;
    }

    public void setChainManager(ChainManager chainManager) {
        this.chainManager = chainManager;
    }

    public FarmWithdrawHelper getHelper() {
        return helper;
    }

    public void setHelper(FarmWithdrawHelper helper) {
        this.helper = helper;
    }

    public FarmCacher getFarmCacher() {
        return farmCacher;
    }

    public void setFarmCacher(FarmCacher farmCacher) {
        this.farmCacher = farmCacher;
    }

    public FarmUserInfoStorageService getUserInfoStorageService() {
        return userInfoStorageService;
    }

    public void setUserInfoStorageService(FarmUserInfoStorageService userInfoStorageService) {
        this.userInfoStorageService = userInfoStorageService;
    }
}
