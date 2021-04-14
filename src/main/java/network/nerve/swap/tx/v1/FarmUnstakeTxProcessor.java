package network.nerve.swap.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;

import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
@Component("FarmUnstakeTxProcessorV1")
public class FarmUnstakeTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;

    @Override
    public int getType() {
        return TxType.FARM_WITHDRAW;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = chainManager.getChain(chainId);
        //todo 
        return null;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        //todo
        return false;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        //todo
        return false;
    }
}
