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

import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
public class SwapTradeBus extends BaseBus {

    /**
     * 交易对的变化数据
     */
    private List<TradePairBus> tradePairBuses;

    public SwapTradeBus() {
    }

    public SwapTradeBus(List<TradePairBus> tradePairBuses) {
        this.tradePairBuses = tradePairBuses;
    }

    public List<TradePairBus> getTradePairBuses() {
        return tradePairBuses;
    }

    public void setTradePairBuses(List<TradePairBus> tradePairBuses) {
        this.tradePairBuses = tradePairBuses;
    }
}