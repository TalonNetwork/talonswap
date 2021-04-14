package network.nerve.swap.cache;

import network.nerve.swap.model.dto.SwapPairDTO;

import java.util.Collection;

/**
 * @author Niels
 */
public interface SwapPairCacher {

    SwapPairDTO get(String address);

    SwapPairDTO put(String address, SwapPairDTO dto);

    SwapPairDTO remove(String address);

    Collection<SwapPairDTO> getList();

    boolean isExist(String pairAddress);
}