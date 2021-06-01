package network.nerve.swap.tx.v1.vals;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.swap.cache.SwapPairCacher;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.ValidaterResult;

/**
 * @author Niels
 */
@Component
public class CreatePairTxValidater {

    @Autowired
    private SwapPairCacher cacher;

    /**
     * 判断交易对是否存在
     *
     * @param pairAddress
     * @return
     */
    public ValidaterResult isPairNotExist(String pairAddress) {
        if (cacher.isExist(pairAddress)) {
            return ValidaterResult.getFailed(SwapErrorCode.PAIR_ALREADY_EXISTS);
        }
        return ValidaterResult.getSuccess();
    }

    public SwapPairCacher getCacher() {
        return cacher;
    }

    public void setCacher(SwapPairCacher cacher) {
        this.cacher = cacher;
    }
}
