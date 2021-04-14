package network.nerve.swap.utils;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.NulsHash;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.model.NerveToken;

import java.math.BigInteger;
import java.util.Base64;

import static network.nerve.swap.constant.SwapConstant.BI_1000;
import static network.nerve.swap.constant.SwapConstant.BI_997;
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

    public static byte[] getFarmAddress(int chainId, NulsHash farmHash) {
        return AddressTool.getAddress(farmHash.getBytes(), chainId, SwapConstant.FARM_ADDRESS_TYPE);
    }

    public static String getStringFarmAddress(int chainId, NulsHash farmHash) {
        return AddressTool.getStringAddressByBytes(getFarmAddress(chainId, farmHash));
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
}
