package network.nerve.swap.model.txdata.stable;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * @author Niels
 */
public class StableSwapTradeData extends BaseNulsData {

    private byte[] to;
    private long deadline;
    private byte receiveIndex;
    /**
     * 手续费接收地址
     */
    private byte[] feeTo;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(to);
        stream.writeUint32(deadline);
        stream.writeByte(receiveIndex);
        if (feeTo != null) {
            stream.write(feeTo);
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.to = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.deadline = byteBuffer.readUint32();
        this.receiveIndex = byteBuffer.readByte();
        if (!byteBuffer.isFinished()) {
            this.feeTo = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfUint32();
        size += 1;
        if (feeTo != null) {
            size += Address.ADDRESS_LENGTH;
        }
        return size;
    }


    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public byte[] getFeeTo() {
        return feeTo;
    }

    public void setFeeTo(byte[] feeTo) {
        this.feeTo = feeTo;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public byte getReceiveIndex() {
        return receiveIndex;
    }

    public void setReceiveIndex(byte receiveIndex) {
        this.receiveIndex = receiveIndex;
    }
}
