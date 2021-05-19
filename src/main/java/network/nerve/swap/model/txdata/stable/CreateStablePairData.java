package network.nerve.swap.model.txdata.stable;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.swap.model.NerveToken;

import java.io.IOException;

/**
 * @author Niels
 */
public class CreateStablePairData extends BaseNulsData {

    private NerveToken[] coins;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        short length = (short) coins.length;
        stream.writeUint8(length);
        for (int i = 0; i < length; i++) {
            stream.writeNulsData(coins[i]);
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        short length = byteBuffer.readUint8();
        this.coins = new NerveToken[length];
        for (int i = 0; i < length; i++) {
            coins[i] = byteBuffer.readNulsData(new NerveToken());
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfUint8();
        size += SerializeUtils.sizeOfNulsData(new NerveToken()) * coins.length;
        return size;
    }

    public NerveToken[] getCoins() {
        return coins;
    }

    public void setCoins(NerveToken[] coins) {
        this.coins = coins;
    }
}
