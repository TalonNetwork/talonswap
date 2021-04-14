package network.nerve.swap.storage;

import network.nerve.swap.model.po.FarmUserInfoPO;

import java.util.List;

/**
 * @author Niels
 */
public interface FarmUserInfoStorageService {

    FarmUserInfoPO save(FarmUserInfoPO po);

    FarmUserInfoPO delete(FarmUserInfoPO po);

    List<FarmUserInfoPO> getList();
}
