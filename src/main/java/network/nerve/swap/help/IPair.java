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

package network.nerve.swap.help;

import network.nerve.swap.model.po.SwapPairPO;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/3/30
 */
public interface IPair {

    SwapPairPO getPair();

    BigInteger[] getReserves();

    BigInteger totalSupply();

    long getBlockTimeLast();

    long getBlockHeightLast();

    void update(BigInteger liquidityChange,
                BigInteger balance0, BigInteger balance1,
                BigInteger reserve0, BigInteger reserve1,
                long blockHeight, long blockTime);

    void rollback(BigInteger liquidityChange,
                  BigInteger reserve0, BigInteger reserve1,
                  long blockHeight, long blockTime);
}
