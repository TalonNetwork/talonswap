package network.nerve.swap.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;

import java.io.IOException;

/**
 * @author Niels
 */
public class FarmStakeData  extends BaseNulsData {

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        //todo
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        //todo
    }

    @Override
    public int size() {
        //todo
        return 0;
    }
}
