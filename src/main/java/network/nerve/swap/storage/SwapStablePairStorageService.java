package network.nerve.swap.storage;

import network.nerve.swap.model.po.stable.StableSwapPairPo;

/**
 * @author Niels
 */
public interface SwapStablePairStorageService {

    boolean savePair(byte[] address, StableSwapPairPo po) throws Exception;

    StableSwapPairPo getPair(byte[] address);

    StableSwapPairPo getPair(String address);

    boolean delelePair(String address) throws Exception;

}
