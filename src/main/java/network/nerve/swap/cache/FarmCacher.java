package network.nerve.swap.cache;

import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.po.FarmPoolPO;

import java.util.Collection;

/**
 * @author Niels
 */
//TODO pierre 实现类
public interface FarmCacher {

    FarmPoolPO get(String hash);

    FarmPoolPO put(String hash, FarmPoolPO po);

    FarmPoolPO remove(String hash);

    Collection<FarmPoolPO> getList();
}
