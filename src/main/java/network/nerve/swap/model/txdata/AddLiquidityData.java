package network.nerve.swap.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author Niels
 */
public class AddLiquidityData extends BaseNulsData {

    private byte[] to;
    private long deadline;
    private BigInteger amountAMin;
    private BigInteger amountBMin;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(to);
        stream.writeUint32(deadline);
        stream.writeBigInteger(amountAMin);
        stream.writeBigInteger(amountBMin);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.to = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.deadline = byteBuffer.readUint32();
        this.amountAMin = byteBuffer.readBigInteger();
        this.amountBMin = byteBuffer.readBigInteger();
    }

    @Override
    public int size() {
        int size = 0;
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfBigInteger();
        return size;
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

    public BigInteger getAmountAMin() {
        return amountAMin;
    }

    public void setAmountAMin(BigInteger amountAMin) {
        this.amountAMin = amountAMin;
    }

    public BigInteger getAmountBMin() {
        return amountBMin;
    }

    public void setAmountBMin(BigInteger amountBMin) {
        this.amountBMin = amountBMin;
    }
}
