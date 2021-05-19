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
package network.nerve.swap.constant;

/**
 * @author: PierreLuo
 * @date: 2021/4/15
 */
public interface SwapCmdConstant {
    String BATCH_BEGIN = "sw_batch_begin";
    String INVOKE = "sw_invoke";
    String BATCH_END = "sw_batch_end";
    String IS_LEGAL_COIN_FOR_ADD_STABLE = "sw_is_legal_coin_for_add_stable";
    String ADD_COIN_FOR_STABLE = "sw_add_coin_for_stable";

    String CREATE_FARM = "sw_createFarm";
    String FARM_STAKE = "sw_farmstake";
    String FARM_WAITHDRAW = "sw_farmwithdraw";
    String FARM_INFO = "sw_getfarm";
    String FARM_USER_INFO = "sw_getstakeinfo";
}
