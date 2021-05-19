/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package network.nerve.swap.constant;

import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.rpc.model.ModuleE;

/**
 * @author: qinyifeng
 */
public interface SwapErrorCode extends CommonCodeConstanst {

    ErrorCode PAIR_ALREADY_EXISTS = ErrorCode.init(ModuleE.SW.getPrefix() + "_0000");

    ErrorCode CHAIN_NOT_EXIST = ErrorCode.init(ModuleE.SW.getPrefix() + "_0001");

    ErrorCode INSUFFICIENT_AMOUNT = ErrorCode.init(ModuleE.SW.getPrefix() + "_0002");
    ErrorCode INSUFFICIENT_LIQUIDITY = ErrorCode.init(ModuleE.SW.getPrefix() + "_0003");
    ErrorCode INSUFFICIENT_INPUT_AMOUNT = ErrorCode.init(ModuleE.SW.getPrefix() + "_0004");
    ErrorCode INSUFFICIENT_OUTPUT_AMOUNT = ErrorCode.init(ModuleE.SW.getPrefix() + "_0005");
    ErrorCode INVALID_PATH = ErrorCode.init(ModuleE.SW.getPrefix() + "_0006");
    ErrorCode EXPIRED = ErrorCode.init(ModuleE.SW.getPrefix() + "_0007");
    ErrorCode ADD_LIQUIDITY_TOS_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0008");
    ErrorCode PAIR_ADDRESS_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0009");
    ErrorCode ADD_LIQUIDITY_AMOUNT_LOCK_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0010");
    ErrorCode INSUFFICIENT_B_AMOUNT = ErrorCode.init(ModuleE.SW.getPrefix() + "_0011");
    ErrorCode INSUFFICIENT_A_AMOUNT = ErrorCode.init(ModuleE.SW.getPrefix() + "_0012");
    ErrorCode INSUFFICIENT_LIQUIDITY_MINTED = ErrorCode.init(ModuleE.SW.getPrefix() + "_0013");
    ErrorCode INSUFFICIENT_LIQUIDITY_BURNED = ErrorCode.init(ModuleE.SW.getPrefix() + "_0014");
    ErrorCode ADD_LIQUIDITY_FROMS_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0015");
    ErrorCode IDENTICAL_ADDRESSES = ErrorCode.init(ModuleE.SW.getPrefix() + "_0016");

    ErrorCode REMOVE_LIQUIDITY_TOS_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0017");
    ErrorCode REMOVE_LIQUIDITY_AMOUNT_LOCK_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0018");
    ErrorCode REMOVE_LIQUIDITY_FROMS_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0019");
    ErrorCode LEDGER_ASSET_NOT_EXIST = ErrorCode.init(ModuleE.SW.getPrefix() + "_0020");
    ErrorCode PAIR_NOT_EXIST = ErrorCode.init(ModuleE.SW.getPrefix() + "_0021");

    ErrorCode SWAP_TRADE_TOS_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0022");
    ErrorCode SWAP_TRADE_AMOUNT_LOCK_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0023");
    ErrorCode SWAP_TRADE_FROMS_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0024");
    ErrorCode INVALID_TO = ErrorCode.init(ModuleE.SW.getPrefix() + "_0025");
    ErrorCode K = ErrorCode.init(ModuleE.SW.getPrefix() + "_0026");
    ErrorCode PAIR_INCONSISTENCY = ErrorCode.init(ModuleE.SW.getPrefix() + "_0027");
    ErrorCode BLOCK_HEIGHT_INCONSISTENCY = ErrorCode.init(ModuleE.SW.getPrefix() + "_0028");
    ErrorCode INVALID_COINS = ErrorCode.init(ModuleE.SW.getPrefix() + "_0029");
    ErrorCode INVALID_AMOUNTS = ErrorCode.init(ModuleE.SW.getPrefix() + "_0030");
    ErrorCode SWAP_TRADE_RECEIVE_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0031");
    ErrorCode COIN_LENGTH_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_0032");
    ErrorCode IDENTICAL_TOKEN = ErrorCode.init(ModuleE.SW.getPrefix() + "_0033");

    ErrorCode FARM_SYRUP_PER_BLOCK_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1001");
    ErrorCode FARM_ADMIN_ADDRESS_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1002");
    ErrorCode FARM_TOKEN_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1003");
    ErrorCode FARM_TX_DATA_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1004");
    ErrorCode FARM_LOCK_HEIGHT_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1005");
    ErrorCode FARM_NOT_EXIST = ErrorCode.init(ModuleE.SW.getPrefix() + "_1006");
    ErrorCode FARM_SYRUP_CANNOT_LOCK = ErrorCode.init(ModuleE.SW.getPrefix() + "_1007");
    ErrorCode FARM_SYRUP_DEPOSIT_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1008");
    ErrorCode FARM_SYRUP_DEPOSIT_AMOUNT_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1009");
    ErrorCode FARM_STAKE_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1010");
    ErrorCode FARM_STAKE_AMOUNT_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1011");
    ErrorCode FARM_STAKE_ADDRESS_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1012");
    ErrorCode FARM_NERVE_STAKE_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1013");
    ErrorCode FARM_STAKE_LOCK_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_10014");
    ErrorCode FARM_SYRUP_BALANCE_NOT_ENOUGH = ErrorCode.init(ModuleE.SW.getPrefix() + "_10015");
    ErrorCode ACCOUNT_VALID_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_10016");
    ErrorCode MOUDLE_COMMUNICATION_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_10017");
    ErrorCode BALANCE_NOT_EMOUGH = ErrorCode.init(ModuleE.SW.getPrefix() + "_10018");
    ErrorCode FARM_IS_NOT_START_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_10019");
    ErrorCode FARM_IS_LOCKED_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_10020");
    ErrorCode FARM_TOTAL_SYRUP_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_10021");
    ErrorCode FARM_NERVE_WITHDRAW_ERROR = ErrorCode.init(ModuleE.SW.getPrefix() + "_1022");
}
