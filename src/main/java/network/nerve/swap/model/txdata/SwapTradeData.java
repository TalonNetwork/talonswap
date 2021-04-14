package network.nerve.swap.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.swap.model.NerveToken;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author Niels
 */
public class SwapTradeData extends BaseNulsData {

    private BigInteger amountOutMin;
    private byte[] to;
    private long deadline;
    private NerveToken[] path;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBigInteger(amountOutMin);
        stream.write(to);
        stream.writeUint32(deadline);
        short length = (short) path.length;
        stream.writeUint8(length);
        for (int i = 0; i < length; i++) {
            stream.writeNulsData(path[i]);
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.amountOutMin = byteBuffer.readBigInteger();
        this.to = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.deadline = byteBuffer.readUint32();
        short length = byteBuffer.readUint8();
        this.path = new NerveToken[length];
        for (int i = 0; i < length; i++) {
            path[i] = byteBuffer.readNulsData(new NerveToken());
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfBigInteger();
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint8();
        size += SerializeUtils.sizeOfNulsData(new NerveToken()) * path.length;
        return size;
    }

    public BigInteger getAmountOutMin() {
        return amountOutMin;
    }

    public void setAmountOutMin(BigInteger amountOutMin) {
        this.amountOutMin = amountOutMin;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public NerveToken[] getPath() {
        return path;
    }

    public void setPath(NerveToken[] path) {
        this.path = path;
    }
}
