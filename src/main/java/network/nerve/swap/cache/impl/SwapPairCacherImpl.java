package network.nerve.swap.cache.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.swap.cache.SwapPairCacher;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.po.SwapPairReservesPO;
import network.nerve.swap.rpc.call.LedgerCall;
import network.nerve.swap.storage.SwapPairReservesStorageService;
import network.nerve.swap.storage.SwapPairStorageService;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
@Component
public class SwapPairCacherImpl implements SwapPairCacher {

    @Autowired
    private SwapPairStorageService swapPairStorageService;
    @Autowired
    private SwapPairReservesStorageService swapPairReservesStorageService;
    @Autowired
    private ChainManager chainManager;

    //不同的链地址不会相同，所以不再区分链
    private Map<String, SwapPairDTO> CACHE_MAP = new HashMap<>();

    @Override
    public SwapPairDTO get(String address) {
        SwapPairDTO dto = CACHE_MAP.get(address);
        if (dto == null) {
            SwapPairPO pairPO = swapPairStorageService.getPair(address);
            if (pairPO == null) {
                return null;
            }
            dto = new SwapPairDTO();
            dto.setPo(pairPO);
            SwapPairReservesPO reservesPO = swapPairReservesStorageService.getPairReserves(address);
            dto.setReserve0(reservesPO.getReserve0());
            dto.setReserve1(reservesPO.getReserve1());
            dto.setTotalLP(reservesPO.getTotalLP());
            dto.setBlockTimeLast(reservesPO.getBlockTimeLast());
            dto.setBlockHeightLast(reservesPO.getBlockHeightLast());
            CACHE_MAP.put(address, dto);
        }
        return dto;
    }

    @Override
    public SwapPairDTO put(String address, SwapPairDTO dto) {
        return CACHE_MAP.put(address, dto);
    }

    @Override
    public SwapPairDTO remove(String address) {
        return CACHE_MAP.remove(address);
    }

    @Override
    public Collection<SwapPairDTO> getList() {
        return CACHE_MAP.values();
    }

    @Override
    public boolean isExist(String pairAddress) {
        return CACHE_MAP.containsKey(pairAddress);
    }
}
