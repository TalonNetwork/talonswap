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
package network.nerve.swap.model.dto;

import network.nerve.swap.model.NerveToken;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
public class AddLiquidityDTO {

    private byte[] fromA;
    private byte[] fromB;
    private byte[] pairAddress;
    private NerveToken tokenA;
    private NerveToken tokenB;
    private BigInteger userLiquidityA;
    private BigInteger userLiquidityB;

    public AddLiquidityDTO(byte[] fromA, byte[] fromB, byte[] pairAddress, NerveToken tokenA, NerveToken tokenB, BigInteger userLiquidityA, BigInteger userLiquidityB) {
        this.fromA = fromA;
        this.fromB = fromB;
        this.pairAddress = pairAddress;
        this.tokenA = tokenA;
        this.tokenB = tokenB;
        this.userLiquidityA = userLiquidityA;
        this.userLiquidityB = userLiquidityB;
    }

    public byte[] getFromA() {
        return fromA;
    }

    public void setFromA(byte[] fromA) {
        this.fromA = fromA;
    }

    public byte[] getFromB() {
        return fromB;
    }

    public void setFromB(byte[] fromB) {
        this.fromB = fromB;
    }

    public byte[] getPairAddress() {
        return pairAddress;
    }

    public void setPairAddress(byte[] pairAddress) {
        this.pairAddress = pairAddress;
    }

    public NerveToken getTokenA() {
        return tokenA;
    }

    public void setTokenA(NerveToken tokenA) {
        this.tokenA = tokenA;
    }

    public NerveToken getTokenB() {
        return tokenB;
    }

    public void setTokenB(NerveToken tokenB) {
        this.tokenB = tokenB;
    }

    public BigInteger getUserLiquidityA() {
        return userLiquidityA;
    }

    public void setUserLiquidityA(BigInteger userLiquidityA) {
        this.userLiquidityA = userLiquidityA;
    }

    public BigInteger getUserLiquidityB() {
        return userLiquidityB;
    }

    public void setUserLiquidityB(BigInteger userLiquidityB) {
        this.userLiquidityB = userLiquidityB;
    }
}
