package network.nerve.swap.utils;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.model.message.MessageUtil;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.TokenAmount;
import network.nerve.swap.model.business.RemoveLiquidityBus;
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;
import network.nerve.swap.model.dto.RealAddLiquidityOrderDTO;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.vo.SwapPairVO;
import network.nerve.swap.model.vo.RouteVO;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static network.nerve.swap.constant.SwapConstant.*;
import static network.nerve.swap.constant.SwapErrorCode.*;

/**
 * @author Niels
 */
public class SwapUtils {

    public static String getStringPairAddress(int chainId, NerveToken token0, NerveToken token1) {
        return AddressTool.getStringAddressByBytes(getPairAddress(chainId, token0, token1));
    }

    public static String getStringPairAddress(int chainId, NerveToken token0, NerveToken token1, String prefix) {
        return AddressTool.getStringAddressByBytes(getPairAddress(chainId, token0, token1), prefix);
    }

    public static byte[] getPairAddress(int chainId, NerveToken token0, NerveToken token1) {
        return getSwapAddress(chainId, token0, token1, SwapConstant.PAIR_ADDRESS_TYPE);
    }

    private static byte[] getSwapAddress(int chainId, NerveToken token0, NerveToken token1, byte addressType) {
        if (token0 == null || token1 == null) {
            throw new NulsRuntimeException(CommonCodeConstanst.NULL_PARAMETER);
        }
        NerveToken[] array = tokenSort(token0, token1);
        byte[] all = ArraysTool.concatenate(
                Sha256Hash.hash(SerializeUtils.int32ToBytes(array[0].getChainId())),
                Sha256Hash.hash(SerializeUtils.int32ToBytes(array[0].getAssetId())),
                Sha256Hash.hash(SerializeUtils.int32ToBytes(array[1].getChainId())),
                Sha256Hash.hash(SerializeUtils.int32ToBytes(array[1].getAssetId()))
        );
        return AddressTool.getAddress(Sha256Hash.hash(all), chainId, addressType);
    }

    public static byte[] getFarmAddress(int chainId) {
        return AddressTool.getAddress(NulsHash.EMPTY_NULS_HASH.getBytes(), chainId, SwapConstant.FARM_ADDRESS_TYPE);
    }

    public static String getStringFarmAddress(int chainId) {
        return AddressTool.getStringAddressByBytes(getFarmAddress(chainId));
    }

    public static NerveToken[] tokenSort(NerveToken token0, NerveToken token1) {
        if (token0 == null || token1 == null) {
            throw new NulsRuntimeException(CommonCodeConstanst.NULL_PARAMETER);
        }
        if (token0.getChainId() == token1.getChainId() && token0.getAssetId() == token1.getAssetId()) {
            throw new NulsRuntimeException(CommonCodeConstanst.PARAMETER_ERROR);
        }
        boolean positiveSequence = token0.getChainId() < token1.getChainId() || (token0.getChainId() == token1.getChainId() && token0.getAssetId() < token1.getAssetId());
        if (positiveSequence) {
            return new NerveToken[]{token0, token1};
        }
        return new NerveToken[]{token1, token0};
    }

    public static BigInteger quote(BigInteger amountA, BigInteger reserveA, BigInteger reserveB) {
        if (amountA.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_AMOUNT);
        }
        if (reserveA.compareTo(BigInteger.ZERO) <= 0 || reserveB.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_LIQUIDITY);
        }
        BigInteger amountB = amountA.multiply(reserveB).divide(reserveA);
        return amountB;
    }

    public static BigInteger getAmountOut(BigInteger amountIn, BigInteger reserveIn, BigInteger reserveOut) {
        if (amountIn.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_INPUT_AMOUNT);
        }
        if (reserveIn.compareTo(BigInteger.ZERO) <= 0 || reserveOut.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_LIQUIDITY);
        }
        BigInteger amountInWithFee = amountIn.multiply(BI_997);
        BigInteger numerator = amountInWithFee.multiply(reserveOut);
        BigInteger denominator = reserveIn.multiply(BI_1000).add(amountInWithFee);
        BigInteger amountOut = numerator.divide(denominator);
        return amountOut;
    }

    public static BigInteger getAmountIn(BigInteger amountOut, BigInteger reserveIn, BigInteger reserveOut) {
        if (amountOut.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_OUTPUT_AMOUNT);
        }
        if (reserveIn.compareTo(BigInteger.ZERO) <= 0 || reserveOut.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_LIQUIDITY);
        }
        BigInteger numerator = reserveIn.multiply(amountOut).multiply(BI_1000);
        BigInteger denominator = reserveOut.subtract(amountOut).multiply(BI_997);
        BigInteger amountIn = numerator.divide(denominator).add(BigInteger.ONE);
        return amountIn;
    }

    public static BigInteger[] getReserves(int chainId, IPairFactory pairFactory, NerveToken tokenA, NerveToken tokenB) {
        NerveToken[] nerveTokens = tokenSort(tokenA, tokenB);
        NerveToken token0 = nerveTokens[0];
        BigInteger[] reserves = pairFactory.getPair(getStringPairAddress(chainId, tokenA, tokenB)).getReserves();
        BigInteger[] result = tokenA.equals(token0) ? reserves : new BigInteger[]{reserves[1], reserves[0]};
        return result;
    }

    public static BigInteger[] getAmountsOut(int chainId, IPairFactory pairFactory, BigInteger amountIn, NerveToken[] path) {
        int pathLength = path.length;
        if (pathLength < 2) {
            throw new NulsRuntimeException(INVALID_PATH);
        }
        BigInteger[] amounts = new BigInteger[pathLength];
        amounts[0] = amountIn;
        BigInteger reserveIn, reserveOut;
        for (int i = 0; i < pathLength - 1; i++) {
            BigInteger[] reserves = getReserves(chainId, pairFactory, path[i], path[i + 1]);
            reserveIn = reserves[0];
            reserveOut = reserves[1];
            amounts[i + 1] = getAmountOut(amounts[i], reserveIn, reserveOut);
        }
        return amounts;
    }

    public static BigInteger[] getAmountsIn(int chainId, IPairFactory pairFactory, BigInteger amountOut, NerveToken[] path) {
        int pathLength = path.length;
        if (pathLength < 2) {
            throw new NulsRuntimeException(INVALID_PATH);
        }
        BigInteger[] amounts = new BigInteger[pathLength];
        amounts[pathLength - 1] = amountOut;
        BigInteger reserveIn, reserveOut;
        for (int i = pathLength - 1; i > 0; i--) {
            BigInteger[] reserves = getReserves(chainId, pairFactory, path[i - 1], path[i]);
            reserveIn = reserves[0];
            reserveOut = reserves[1];
            amounts[i - 1] = getAmountIn(amounts[i], reserveIn, reserveOut);
        }
        return amounts;
    }

    public static RealAddLiquidityOrderDTO calcAddLiquidity(
            int chainId, IPairFactory iPairFactory,
            NerveToken tokenA,
            NerveToken tokenB,
            BigInteger amountADesired,
            BigInteger amountBDesired,
            BigInteger amountAMin,
            BigInteger amountBMin
    ) throws NulsException {
        BigInteger[] _reserves = SwapUtils.getReserves(chainId, iPairFactory, tokenA, tokenB);
        BigInteger reserveA = _reserves[0];
        BigInteger reserveB = _reserves[1];
        BigInteger[] realAddLiquidity;
        BigInteger[] refund;
        if (reserveA.equals(BigInteger.ZERO) && reserveB.equals(BigInteger.ZERO)) {
            realAddLiquidity = new BigInteger[]{amountADesired, amountBDesired};
            refund = new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO};
        } else {
            BigInteger amountBOptimal = SwapUtils.quote(amountADesired, reserveA, reserveB);
            if (amountBOptimal.compareTo(amountBDesired) <= 0) {
                if (amountBOptimal.compareTo(amountBMin) < 0) {
                    throw new NulsException(INSUFFICIENT_B_AMOUNT);
                }
                realAddLiquidity = new BigInteger[]{amountADesired, amountBOptimal};
                refund = new BigInteger[]{BigInteger.ZERO, amountBDesired.subtract(amountBOptimal)};
            } else {
                BigInteger amountAOptimal = SwapUtils.quote(amountBDesired, reserveB, reserveA);
                if (amountAOptimal.compareTo(amountADesired) > 0) {
                    throw new NulsException(INSUFFICIENT_A_AMOUNT);
                }
                if (amountAOptimal.compareTo(amountAMin) < 0) {
                    throw new NulsException(INSUFFICIENT_A_AMOUNT);
                }
                realAddLiquidity = new BigInteger[]{amountAOptimal, amountBDesired};
                refund = new BigInteger[]{amountADesired.subtract(amountAOptimal), BigInteger.ZERO};
            }
        }
        NerveToken[] tokens = SwapUtils.tokenSort(tokenA, tokenB);
        realAddLiquidity = tokens[0].equals(tokenA) ? realAddLiquidity : new BigInteger[]{realAddLiquidity[1], realAddLiquidity[0]};
        BigInteger[] reserves = tokens[0].equals(tokenA) ? new BigInteger[]{reserveA, reserveB} : new BigInteger[]{reserveB, reserveA};

        // 计算用户获取的LP资产
        IPair pair = iPairFactory.getPair(SwapUtils.getStringPairAddress(chainId, tokenA, tokenB));
        BigInteger totalSupply = pair.totalSupply();
        BigInteger liquidity;
        if (totalSupply.equals(BigInteger.ZERO)) {
            liquidity = realAddLiquidity[0].multiply(realAddLiquidity[1]).sqrt().subtract(SwapConstant.MINIMUM_LIQUIDITY);
        } else {
            BigInteger _liquidity0 = realAddLiquidity[0].multiply(totalSupply).divide(reserves[0]);
            BigInteger _liquidity1 = realAddLiquidity[1].multiply(totalSupply).divide(reserves[1]);
            liquidity = _liquidity0.compareTo(_liquidity1) < 0 ? _liquidity0 : _liquidity1;
        }
        if (liquidity.compareTo(BigInteger.ZERO) < 0) {
            throw new NulsException(SwapErrorCode.INSUFFICIENT_LIQUIDITY_MINTED);
        }

        return new RealAddLiquidityOrderDTO(realAddLiquidity, reserves, refund, liquidity);
    }

    public static RemoveLiquidityBus calRemoveLiquidityBusiness(
            int chainId, IPairFactory iPairFactory,
            byte[] pairAddress, BigInteger liquidity,
            NerveToken tokenA,
            NerveToken tokenB,
            BigInteger amountAMin,
            BigInteger amountBMin) throws NulsException {
        IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairAddress));
        BigInteger[] reserves = pair.getReserves();
        SwapPairPO pairPO = pair.getPair();
        NerveToken token0 = pairPO.getToken0();
        NerveToken token1 = pairPO.getToken1();
        BigInteger balance0 = reserves[0];
        BigInteger balance1 = reserves[1];
        BigInteger totalSupply = pair.totalSupply();
        // 可赎回的资产
        BigInteger amount0 = liquidity.multiply(balance0).divide(totalSupply);
        BigInteger amount1 = liquidity.multiply(balance1).divide(totalSupply);
        if (amount0.compareTo(BigInteger.ZERO) <= 0 || amount1.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsException(INSUFFICIENT_LIQUIDITY_BURNED);
        }

        boolean firstTokenA = tokenA.equals(token0);
        BigInteger amountA, amountB;
        if (firstTokenA) {
            amountA = amount0;
            amountB = amount1;
        } else {
            amountA = amount1;
            amountB = amount0;
        }
        if (amountA.compareTo(amountAMin) < 0) {
            throw new NulsException(INSUFFICIENT_A_AMOUNT);
        }
        if (amountB.compareTo(amountBMin) < 0) {
            throw new NulsException(INSUFFICIENT_B_AMOUNT);
        }
        RemoveLiquidityBus bus = new RemoveLiquidityBus(amount0, amount1, balance0, balance1, liquidity, pair, token0, token1);
        bus.setPreBlockHeight(pair.getBlockHeightLast());
        bus.setPreBlockTime(pair.getBlockTimeLast());
        return bus;
    }

    public static StableAddLiquidityBus calStableAddLiquididy(int chainId, IPairFactory iPairFactory, String pairAddress, byte[] from, BigInteger[] amounts, byte[] to) throws NulsException {

        // 获取用户添加流动性的币中数量最小的
        BigInteger min = SwapUtils.min(amounts);
        int length = amounts.length;
        // 计算实际添加的资产，退还多余的资产
        BigInteger[] realAmounts;
        BigInteger[] refundAmounts = null;
        if (min.compareTo(BigInteger.ZERO) > 0) {
            realAmounts = new BigInteger[length];
            refundAmounts = new BigInteger[length];
            for (int i = 0; i < length; i++) {
                BigInteger amount = amounts[i];
                if (amount.equals(BigInteger.ZERO)) {
                    refundAmounts[i] = amount;
                    realAmounts[i] = amount;
                } else {
                    refundAmounts[i] = amount.subtract(min);
                    realAmounts[i] = min;
                }
            }
        } else {
            realAmounts = amounts;
        }
        // 计算用户获取的LP资产
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        BigInteger[] balances = stablePair.getBalances();
        BigInteger totalSupply = stablePair.totalSupply();
        BigInteger liquidity;
        if (totalSupply.equals(BigInteger.ZERO)) {
            liquidity = SwapUtils.getCumulativeAmounts(realAmounts);
        } else {
            // 获取池子总币量
            BigInteger poolAmounts = SwapUtils.getCumulativeAmounts(balances);
            BigInteger currentInAmounts = SwapUtils.getCumulativeAmounts(realAmounts);
            liquidity = currentInAmounts.multiply(totalSupply).divide(poolAmounts);
        }
        StableAddLiquidityBus bus = new StableAddLiquidityBus(from, realAmounts, liquidity, balances, refundAmounts, to);
        bus.setPreBlockHeight(stablePair.getBlockHeightLast());
        bus.setPreBlockTime(stablePair.getBlockTimeLast());
        return bus;
    }

    public static StableRemoveLiquidityBus calStableRemoveLiquidityBusiness(
            int chainId, IPairFactory iPairFactory,
            BigInteger liquidity, byte[] indexs, byte[] pairAddressBytes, byte[] to) throws NulsException {

        // 计算用户赎回的资产
        String pairAddress = AddressTool.getStringAddressByBytes(pairAddressBytes);
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        BigInteger[] balances = stablePair.getBalances();
        BigInteger totalSupply = stablePair.totalSupply();
        // 检查indexs合法与重复
        StableSwapPairPo pairPo = stablePair.getPair();
        NerveToken[] coins = pairPo.getCoins();
        int length = coins.length;
        // indexs本身是等差数列，根据等差数列求和公式算出期望值
        int expect = (length - 1) * length / 2;
        // 累加indexs的实际值
        int fact = 0;
        for (byte index : indexs) {
            if (index > length - 1) {
                throw new NulsException(SwapErrorCode.INVALID_COINS);
            }
            fact = fact + index;
        }
        if (fact != expect) {
            throw new NulsException(SwapErrorCode.INVALID_COINS);
        }
        // 获取池子总币量
        BigInteger poolAmounts = SwapUtils.getCumulativeAmounts(balances);
        // 用户可赎回资产数量
        BigInteger totalReceives = poolAmounts.multiply(liquidity).divide(totalSupply);
        final BigInteger finalTotalReceives = totalReceives;
        // 按照用户选择的提取顺序，扣减池子数量
        BigInteger[] receives = new BigInteger[length];
        for (byte index : indexs) {
            BigInteger balance = balances[index];
            if (totalReceives.compareTo(balance) < 0) {
                receives[index] = totalReceives;
                break;
            } else {
                receives[index] = balance;
                totalReceives = totalReceives.subtract(balance);
                if (totalReceives.equals(BigInteger.ZERO)) {
                    break;
                }
            }
        }
        receives = SwapUtils.emptyFillZero(receives);
        BigInteger _totalReceives = SwapUtils.getCumulativeAmounts(receives);
        if (!_totalReceives.equals(finalTotalReceives)) {
            throw new NulsException(SwapErrorCode.INVALID_AMOUNTS);
        }
        StableRemoveLiquidityBus bus = new StableRemoveLiquidityBus(receives, balances, liquidity, pairAddressBytes, to);
        bus.setPreBlockHeight(stablePair.getBlockHeightLast());
        bus.setPreBlockTime(stablePair.getBlockTimeLast());
        return bus;
    }

    public static StableSwapTradeBus calStableSwapTradeBusiness(
            int chainId, IPairFactory iPairFactory,
            BigInteger[] amountsIn, byte tokenOutIndex, byte[] pairAddressBytes, byte[] to) throws NulsException {
        String pairAddress = AddressTool.getStringAddressByBytes(pairAddressBytes);
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        StableSwapPairPo pairPo = stablePair.getPair();
        NerveToken[] coins = pairPo.getCoins();
        int length = coins.length;
        BigInteger[] unLiquidityAwardFees = new BigInteger[length];
        BigInteger[] changeBalances = new BigInteger[length];
        BigInteger[] balances = stablePair.getBalances();
        BigInteger outBalance = balances[tokenOutIndex];
        BigInteger totalAmountOut = BigInteger.ZERO;
        for (int i = 0; i < length; i++) {
            BigInteger amountIn = amountsIn[i];
            if (amountIn.equals(BigInteger.ZERO)) {
                unLiquidityAwardFees[i] = BigInteger.ZERO;
                continue;
            }
            BigInteger coinFee = amountIn.multiply(SwapContext.FEE_PERMILLAGE_STABLE_SWAP).divide(BI_1000);
            BigInteger amountOut = amountIn.subtract(coinFee);
            if (outBalance.compareTo(amountOut) < 0) {
                throw new NulsException(SwapErrorCode.INSUFFICIENT_OUTPUT_AMOUNT);
            }
            unLiquidityAwardFees[i] = coinFee.multiply(SwapContext.FEE_PERCENT_ALLOCATION_UN_LIQUIDIDY_STABLE_SWAP).divide(BI_100);
            outBalance = outBalance.subtract(amountOut);
            totalAmountOut = totalAmountOut.add(amountOut);
        }
        changeBalances = SwapUtils.emptyFillZero(changeBalances);
        for (int i = 0; i < length; i++) {
            changeBalances[i] = changeBalances[i].add(amountsIn[i]).subtract(unLiquidityAwardFees[i]);
        }
        changeBalances[tokenOutIndex] = changeBalances[tokenOutIndex].subtract(totalAmountOut);
        StableSwapTradeBus bus = new StableSwapTradeBus(pairAddressBytes, changeBalances, balances, amountsIn, unLiquidityAwardFees, tokenOutIndex, totalAmountOut, to);
        bus.setPreBlockHeight(stablePair.getBlockHeightLast());
        bus.setPreBlockTime(stablePair.getBlockTimeLast());
        return bus;
    }

    public static List<RouteVO> bestTradeExactIn(int chainId, IPairFactory iPairFactory, List<SwapPairVO> pairs, TokenAmount tokenAmountIn,
                                                 NerveToken out, LinkedHashSet<SwapPairVO> currentPath,
                                                 List<RouteVO> bestTrade, TokenAmount orginTokenAmountIn, int maxPairSize) {
        List<RouteVO> routes = bestTradeExactIn(chainId, iPairFactory, pairs, tokenAmountIn, out, currentPath, bestTrade, orginTokenAmountIn, 0, maxPairSize);
        routes.sort(RouteVOSort.INSTANCE);
        return routes;
    }

    private static List<RouteVO> bestTradeExactIn(int chainId, IPairFactory iPairFactory, List<SwapPairVO> pairs, TokenAmount tokenAmountIn,
                                                 NerveToken out, LinkedHashSet<SwapPairVO> currentPath,
                                                 List<RouteVO> bestTrade, TokenAmount orginTokenAmountIn, int depth, int maxPairSize) {
        int length = pairs.size();
        for (int i = 0; i < length; i++) {
            SwapPairVO pair = pairs.get(i);
            NerveToken tokenIn = tokenAmountIn.getToken();
            if (!pair.getToken0().equals(tokenIn) && !pair.getToken1().equals(tokenIn)) continue;
            NerveToken tokenOut = pair.getToken0().equals(tokenIn) ? pair.getToken1() : pair.getToken0();
            if (containsCurrency(currentPath, tokenOut)) continue;
            BigInteger[] reserves = SwapUtils.getReserves(chainId, iPairFactory, tokenIn, tokenOut);
            BigInteger amountOut = SwapUtils.getAmountOut(tokenAmountIn.getAmount(), reserves[0], reserves[1]);

            if (tokenOut.equals(out)) {
                currentPath.add(pair);
                bestTrade.add(new RouteVO(currentPath.stream().collect(Collectors.toList()), orginTokenAmountIn, new TokenAmount(tokenOut, amountOut)));
            } else if (depth < (maxPairSize - 1) && pairs.size() > 1){
                LinkedHashSet cloneLinkedHashSet = cloneLinkedHashSet(currentPath);
                cloneLinkedHashSet.add(pair);
                List<SwapPairVO> subList = subList(pairs, 0, i);
                subList.addAll(subList(pairs, i + 1, length));
                bestTradeExactIn(chainId, iPairFactory, subList, new TokenAmount(tokenOut, amountOut), out, cloneLinkedHashSet, bestTrade, orginTokenAmountIn, depth + 1, maxPairSize);
            }
        }
        return bestTrade;
    }

    private static LinkedHashSet cloneLinkedHashSet(LinkedHashSet set) {
        LinkedHashSet<Object> objects = new LinkedHashSet<>();
        objects.addAll(set);
        return objects;
    }

    private static boolean containsCurrency(LinkedHashSet<SwapPairVO> currentPath, NerveToken tokenOut) {
        for (SwapPairVO pair : currentPath) {
            if (pair.hasToken(tokenOut)) {
                return true;
            }
        }
        return false;
    }

    public static List subList(List list, int fromIndex, int toIndex) {
        List objs = new ArrayList();
        for (int i = fromIndex, length = toIndex; i < length; i++) {
            objs.add(list.get(i));
        }
        return objs;
    }

    public static int extractTxTypeFromTx(String txString) throws NulsException {
        String txTypeHexString = txString.substring(0, 4);
        NulsByteBuffer byteBuffer = new NulsByteBuffer(RPCUtil.decode(txTypeHexString));
        return byteBuffer.readUint16();
    }

    public static BigInteger minus(BigInteger a, BigInteger b) {
        BigInteger result = a.subtract(b);
        if (result.compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("Negative number detected.");
        }
        return result;
    }

    public static String asStringByBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] asBytesByBase64(String string) {
        return Base64.getDecoder().decode(string);
    }

    public static String nulsData2Hex(BaseNulsData nulsData) {
        return HexUtil.encode(nulsData2HexBytes(nulsData));
    }

    public static byte[] nulsData2HexBytes(BaseNulsData nulsData) {
        try {
            return nulsData.serialize();
        } catch (IOException e) {
            throw new NulsRuntimeException(e);
        }
    }

    public static Response wrapperFailed(Result result) {
        String msg;
        ErrorCode errorCode;
        if (result != null) {
            errorCode = result.getErrorCode();
            msg = result.getMsg();
            if (StringUtils.isBlank(msg)) {
                msg = errorCode.getMsg();
            }
            Response res = MessageUtil.newFailResponse("", msg);
            res.setResponseErrorCode(errorCode.getCode());
            return res;
        } else {
            return MessageUtil.newFailResponse("", FAILED);
        }
    }

    /**
     * 获取交易签名地址
     *
     * @param tx 交易
     */
    public static byte[] getSingleAddressFromTX(Transaction tx, int chainId,boolean verifySign) throws NulsException {

        if (tx.getTransactionSignature() == null || tx.getTransactionSignature().length == 0) {
            return null;
        }
        try {
            if (tx.isMultiSignTx()) {
                MultiSignTxSignature txSignature = new MultiSignTxSignature();
                txSignature.parse(tx.getTransactionSignature(), 0);
                List<String> pubkeyList = new ArrayList<>();


                if(verifySign){
                    if ((txSignature.getP2PHKSignatures() == null || txSignature.getP2PHKSignatures().size() == 0)) {
                        throw new NulsException(SwapErrorCode.SIGNATURE_ERROR);
                    }
                    List<P2PHKSignature> validSignatures = txSignature.getValidSignature();
                    int validCount = 0;
                    for (P2PHKSignature signature : validSignatures) {
                        if (ECKey.verify(tx.getHash().getBytes(), signature.getSignData().getSignBytes(), signature.getPublicKey())) {
                            validCount++;
                        }else {
                            throw new NulsException(SwapErrorCode.SIGNATURE_ERROR);
                        }
                    }
                    if (validCount < txSignature.getM()) {
                        throw new NulsException(SwapErrorCode.SIGNATURE_ERROR);
                    }
                }


                for (byte[] pub : txSignature.getPubKeyList()) {
                    pubkeyList.add(HexUtil.encode(pub));
                }
                Address address ;
                try {
                    address = new Address(chainId, BaseConstant.P2SH_ADDRESS_TYPE, SerializeUtils.sha256hash160(AddressTool.createMultiSigAccountOriginBytes(chainId, txSignature.getM(), pubkeyList)));
                } catch (Exception e) {
                    Log.error(e);
                    throw new NulsException(SwapErrorCode.FARM_SYRUP_DEPOSIT_ERROR);
                }
                return address.getAddressBytes();
            } else {
                TransactionSignature transactionSignature = new TransactionSignature();
                transactionSignature.parse(tx.getTransactionSignature(), 0);
                List<P2PHKSignature> p2PHKSignatures = transactionSignature.getP2PHKSignatures();
                if (p2PHKSignatures.size() > 1) {
                    throw new NulsException(SwapErrorCode.SIGNATURE_ERROR);
                }
                if(verifySign){
                    boolean r = ECKey.verify(tx.getHash().getBytes(),p2PHKSignatures.get(0).getSignData().getSignBytes(),p2PHKSignatures.get(0).getPublicKey());
                    if(!r){
                        throw new NulsException(SwapErrorCode.SIGNATURE_ERROR);
                    }
                }
                return AddressTool.getAddress(p2PHKSignatures.get(0).getPublicKey(), chainId);
            }
        } catch (NulsException e) {
            Log.error("TransactionSignature parse error!");
            throw e;
        }
    }


    public static void updatePool(FarmPoolPO farm, long blockHeight) {
        if (blockHeight <= farm.getLastRewardBlock()) {
            return;
        }
        BigInteger lpSupply = farm.getStakeTokenBalance();
        if (lpSupply.compareTo(BigInteger.ZERO) <= 0) {
            farm.setLastRewardBlock(blockHeight);
            return;
        }
        BigInteger syrupReward = BigInteger.valueOf(blockHeight - farm.getLastRewardBlock()).multiply(farm.getSyrupPerBlock());
        BigInteger accSyrupPerShare = farm.getAccSyrupPerShare();
//        pool.accSushiPerShare.add(sushiReward.mul(1e12).div(lpSupply));  // 计算每个lp可分到的sushi数量
        accSyrupPerShare = accSyrupPerShare.add(syrupReward.multiply(SwapConstant.BI_1E12).divide(lpSupply));
        farm.setAccSyrupPerShare(accSyrupPerShare);
        farm.setLastRewardBlock(blockHeight);
    }

    public static NerveToken parseTokenStr(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        String s[] = str.split("-");
        if (s.length != 2) {
            return null;
        }
        return new NerveToken(Integer.parseInt(s[0].trim()), Integer.parseInt(s[1].trim()));
    }

    public static void signTx(Transaction tx, byte[] prikey) throws IOException {
        P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx.getHash(), ECKey.fromPrivate(prikey));
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> list = new ArrayList<>();
        list.add(p2PHKSignature);
        transactionSignature.setP2PHKSignatures(list);
        tx.setTransactionSignature(transactionSignature.serialize());
    }

    public static BigInteger getCumulativeAmounts(BigInteger[] amounts) {
        BigInteger result = BigInteger.ZERO;
        if (amounts == null) {
            return result;
        }
        for (BigInteger amount : amounts) {
            if (amount != null) {
                result = result.add(amount);
            }
        }
        return result;
    }

    public static BigInteger min(BigInteger[] amounts) {
        if (amounts == null) {
            return BigInteger.ZERO;
        }
        BigInteger min = null;
        for (BigInteger amount : amounts) {
            if (min == null) {
                min = amount;
                continue;
            }
            if (min.compareTo(amount) > 0) {
                min = amount;
            }
        }
        return min;
    }

    public static BigInteger[] convertNegate(BigInteger[] amounts) {
        if (amounts == null) {
            return null;
        }
        int length = amounts.length;
        BigInteger[] nagates = new BigInteger[length];
        for (int i = 0; i < length; i++) {
            if (amounts[i] == null) {
                nagates[i] = BigInteger.ZERO;
            } else {
                nagates[i] = amounts[i].negate();
            }
        }
        return nagates;
    }

    public static BigInteger[] emptyFillZero(BigInteger[] amounts) {
        if (amounts == null) {
            return null;
        }
        int length = amounts.length;
        // 空值填充
        for (int i = 0; i < length; i++) {
            if (amounts[i] == null) {
                amounts[i] = BigInteger.ZERO;
            }
        }
        return amounts;
    }
}
