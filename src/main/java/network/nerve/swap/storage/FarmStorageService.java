package network.nerve.swap.storage;

import network.nerve.swap.model.po.FarmPoolPO;

import java.util.List;

/**
 * @author Niels
 */
public interface FarmStorageService {

    FarmPoolPO save(FarmPoolPO po);

    FarmPoolPO delete(byte[] hash);

    List<FarmPoolPO> getList();
}
