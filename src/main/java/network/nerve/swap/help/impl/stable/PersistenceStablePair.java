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
package network.nerve.swap.help.impl.stable;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.cache.StableSwapPairCacher;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.po.stable.StableSwapPairBalancesPo;
import network.nerve.swap.storage.SwapStablePairBalancesStorageService;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/9
 */
public class PersistenceStablePair extends AbstractStablePair {

    private StableSwapPairDTO stableSwapPairDTO;
    private SwapStablePairBalancesStorageService swapStablePairBalancesStorageService;
    private StableSwapPairCacher stableSwapPairCacher;

    public PersistenceStablePair(StableSwapPairDTO stableSwapPairDTO,
                                 SwapStablePairBalancesStorageService swapStablePairBalancesStorageService,
                                 StableSwapPairCacher stableSwapPairCacher) {
        this.stableSwapPairDTO = stableSwapPairDTO;
        this.swapStablePairBalancesStorageService = swapStablePairBalancesStorageService;
        this.stableSwapPairCacher = stableSwapPairCacher;
    }

    @Override
    protected StableSwapPairDTO getStableSwapPairDTO() {
        return stableSwapPairDTO;
    }

    @Override
    public void _update(byte[] userAddress, BigInteger liquidityChange, BigInteger[] changeBalances, BigInteger[] newBalances, BigInteger[] balances, long blockHeight, long blockTime) throws Exception {
        stableSwapPairDTO.setTotalLP(stableSwapPairDTO.getTotalLP().add(liquidityChange));
        stableSwapPairDTO.setBalances(newBalances);

        String pairAddress = AddressTool.getStringAddressByBytes(getPair().getAddress());
        this.savePairBalances(pairAddress, newBalances, stableSwapPairDTO.getTotalLP(), blockTime, blockHeight);
        /*// 更新用户LP
        StableSwapUserLiquidityPo liquidityPo = swapStableUserLiquidityStorageService.get(userAddress);
        if (liquidityPo == null) {
            //TODO pierre 添加高度为保存的版本号，防止无限回滚
            liquidityPo = new StableSwapUserLiquidityPo(userAddress, liquidityChange, changeBalances);
        } else {
            liquidityPo.addLiquidity(liquidityChange);
            liquidityPo.addAmounts(changeBalances);
        }
        swapStableUserLiquidityStorageService.save(userAddress, liquidityPo);*/

        // 更新缓存
        stableSwapPairCacher.remove(pairAddress);
    }

    @Override
    public void _rollback(byte[] userAddress, BigInteger liquidityChange, BigInteger[] changeBalances, BigInteger[] newBalances, BigInteger[] balances, long blockHeight, long blockTime) throws Exception {
        stableSwapPairDTO.setTotalLP(stableSwapPairDTO.getTotalLP().subtract(liquidityChange));
        stableSwapPairDTO.setBalances(balances);

        String pairAddress = AddressTool.getStringAddressByBytes(getPair().getAddress());
        this.savePairBalances(pairAddress, balances, stableSwapPairDTO.getTotalLP(), blockTime, blockHeight);
        /*// 更新用户LP
        StableSwapUserLiquidityPo liquidityPo = swapStableUserLiquidityStorageService.get(userAddress);
        if (liquidityPo != null) {
            //TODO pierre 验证高度版本号，防止无限回滚
            liquidityPo.addLiquidity(liquidityChange.negate());
            BigInteger[] changeBalancesNagate = new BigInteger[changeBalances.length];
            int i = 0;
            for (BigInteger c : changeBalances) {
                changeBalancesNagate[i++] = c.negate();
            }
            liquidityPo.addAmounts(changeBalancesNagate);
        }
        swapStableUserLiquidityStorageService.save(userAddress, liquidityPo);*/
        // 更新缓存
        stableSwapPairCacher.remove(pairAddress);
    }

    private void savePairBalances(String pairAddress, BigInteger[] balancesCurrent, BigInteger totalLP, Long blockTime, Long blockHeight) throws Exception {
        int lengthCurrent = balancesCurrent.length;
        StableSwapPairBalancesPo pairBalancesPo = swapStablePairBalancesStorageService.getPairBalances(pairAddress);
        if (pairBalancesPo == null) {
            pairBalancesPo = new StableSwapPairBalancesPo(AddressTool.getAddress(pairAddress), lengthCurrent);
        }
        BigInteger[] balancesFromDB = pairBalancesPo.getBalances();
        int lengthFromDB = balancesFromDB.length;
        // 若当前的长度小于DB中的长度，说明交易对中添加了币种，则不能覆盖更新PO
        if (lengthCurrent < lengthFromDB) {
            for (int i = 0; i < lengthCurrent; i++) {
                balancesFromDB[i] = balancesCurrent[i];
            }
            pairBalancesPo.setBalances(balancesFromDB);
        } else if (lengthCurrent == lengthFromDB) {
            pairBalancesPo.setBalances(balancesCurrent);
        } else {
            // 未提供移除币种的功能，这种场景出现即异常
            throw new NulsException(SwapErrorCode.COIN_LENGTH_ERROR);
        }
        pairBalancesPo.setTotalLP(totalLP);
        pairBalancesPo.setBlockTimeLast(blockTime);
        pairBalancesPo.setBlockHeightLast(blockHeight);
        swapStablePairBalancesStorageService.savePairBalances(pairAddress, pairBalancesPo);
    }

}
