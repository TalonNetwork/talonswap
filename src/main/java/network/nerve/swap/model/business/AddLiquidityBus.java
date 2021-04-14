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
package network.nerve.swap.model.business;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
public class AddLiquidityBus extends BaseBus {

    /**
     * tokenA是否为token0
     */
    private boolean firstTokenA;
    /**
     * 实际添加的资产
     */
    private BigInteger realAddAmountA;
    private BigInteger realAddAmountB;
    /**
     * 获得的LP资产
     */
    private BigInteger liquidity;
    /**
     * 当前池子余额（添加前）
     */
    private BigInteger reserveA;
    private BigInteger reserveB;
    /**
     * 最终退回给用户的资产
     */
    private BigInteger refundAmountA;
    private BigInteger refundAmountB;

    public AddLiquidityBus() {
    }

    public AddLiquidityBus(boolean firstTokenA, BigInteger realAddAmountA, BigInteger realAddAmountB, BigInteger liquidity, BigInteger reserveA, BigInteger reserveB, BigInteger refundAmountA, BigInteger refundAmountB) {
        this.firstTokenA = firstTokenA;
        this.realAddAmountA = realAddAmountA;
        this.realAddAmountB = realAddAmountB;
        this.liquidity = liquidity;
        this.reserveA = reserveA;
        this.reserveB = reserveB;
        this.refundAmountA = refundAmountA;
        this.refundAmountB = refundAmountB;
    }

    public boolean isFirstTokenA() {
        return firstTokenA;
    }

    public void setFirstTokenA(boolean firstTokenA) {
        this.firstTokenA = firstTokenA;
    }

    public BigInteger getRealAddAmountA() {
        return realAddAmountA;
    }

    public void setRealAddAmountA(BigInteger realAddAmountA) {
        this.realAddAmountA = realAddAmountA;
    }

    public BigInteger getRealAddAmountB() {
        return realAddAmountB;
    }

    public void setRealAddAmountB(BigInteger realAddAmountB) {
        this.realAddAmountB = realAddAmountB;
    }

    public BigInteger getLiquidity() {
        return liquidity;
    }

    public void setLiquidity(BigInteger liquidity) {
        this.liquidity = liquidity;
    }

    public BigInteger getReserveA() {
        return reserveA;
    }

    public void setReserveA(BigInteger reserveA) {
        this.reserveA = reserveA;
    }

    public BigInteger getReserveB() {
        return reserveB;
    }

    public void setReserveB(BigInteger reserveB) {
        this.reserveB = reserveB;
    }

    public BigInteger getRefundAmountA() {
        return refundAmountA;
    }

    public void setRefundAmountA(BigInteger refundAmountA) {
        this.refundAmountA = refundAmountA;
    }

    public BigInteger getRefundAmountB() {
        return refundAmountB;
    }

    public void setRefundAmountB(BigInteger refundAmountB) {
        this.refundAmountB = refundAmountB;
    }
}
