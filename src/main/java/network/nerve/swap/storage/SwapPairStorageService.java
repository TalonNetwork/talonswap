package network.nerve.swap.storage;

import network.nerve.swap.model.po.SwapPairPO;

import java.util.List;

/**
 * @author Niels
 */
public interface SwapPairStorageService {
    boolean savePair(byte[] address, SwapPairPO po);
    boolean savePair(String address, SwapPairPO po);

    SwapPairPO getPair(byte[] address);
    SwapPairPO getPair(String address);

    boolean delelePair(byte[] address);
    boolean delelePair(String address);

    List<SwapPairPO> getList();


}
