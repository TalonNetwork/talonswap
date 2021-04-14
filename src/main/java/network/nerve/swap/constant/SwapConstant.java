package network.nerve.swap.constant;

import io.nuls.core.crypto.HexUtil;

import java.math.BigInteger;

/**
 * @author: Loki
 * @date: 2018/11/12
 */
public interface SwapConstant {

    String SWAP_CMD_PATH = "network.nerve.swap.rpc.cmd";
    /**
     * system params
     */
    String SYS_ALLOW_NULL_ARRAY_ELEMENT = "protostuff.runtime.allow_null_array_element";
    String SYS_FILE_ENCODING = "file.encoding";

    String RPC_VERSION = "1.0";

    /** nonce值初始值 */
    byte[] DEFAULT_NONCE = HexUtil.decode("0000000000000000");

    byte[] ZERO_BYTES = new byte[]{0};
    //todo 确定4可不可以用
    byte PAIR_ADDRESS_TYPE = 5;
    byte FARM_ADDRESS_TYPE = 6;

    int LP_TOKEN_DECIMALS = 18;
    BigInteger MINIMUM_LIQUIDITY = BigInteger.valueOf(1000);

    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_2 = 2;

    String LINE = "_";

    BigInteger BI_1000_000 = BigInteger.valueOf(1000000);
    BigInteger BI_1000 = BigInteger.valueOf(1000);
    BigInteger BI_997 = BigInteger.valueOf(997);
    BigInteger BI_3 = BigInteger.valueOf(3);
}
