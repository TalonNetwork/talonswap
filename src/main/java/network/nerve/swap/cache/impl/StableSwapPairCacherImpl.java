package network.nerve.swap.cache.impl;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.swap.cache.StableSwapPairCacher;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.po.stable.StableSwapPairBalancesPo;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.storage.SwapStablePairBalancesStorageService;
import network.nerve.swap.storage.SwapStablePairStorageService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
@Component
public class StableSwapPairCacherImpl implements StableSwapPairCacher {

    @Autowired
    private SwapStablePairStorageService swapStablePairStorageService;
    @Autowired
    private SwapStablePairBalancesStorageService swapStablePairBalancesStorageService;

    //不同的链地址不会相同，所以不再区分链
    private Map<String, StableSwapPairDTO> CACHE_MAP = new HashMap<>();

    @Override
    public StableSwapPairDTO get(String address) {
        StableSwapPairDTO dto = CACHE_MAP.get(address);
        if (dto == null) {
            StableSwapPairPo pairPo = swapStablePairStorageService.getPair(address);
            if (pairPo == null) {
                return null;
            }
            dto = new StableSwapPairDTO();
            dto.setPo(pairPo);
            StableSwapPairBalancesPo pairBalances = swapStablePairBalancesStorageService.getPairBalances(address);
            dto.setBalances(pairBalances.getBalances());
            dto.setTotalLP(pairBalances.getTotalLP());
            dto.setBlockTimeLast(pairBalances.getBlockTimeLast());
            dto.setBlockHeightLast(pairBalances.getBlockHeightLast());
            CACHE_MAP.put(address, dto);
        }
        return dto;
    }

    @Override
    public StableSwapPairDTO put(String address, StableSwapPairDTO dto) {
        return CACHE_MAP.put(address, dto);
    }

    @Override
    public StableSwapPairDTO remove(String address) {
        return CACHE_MAP.remove(address);
    }

    @Override
    public boolean isExist(String pairAddress) {
        return CACHE_MAP.containsKey(pairAddress);
    }
}
