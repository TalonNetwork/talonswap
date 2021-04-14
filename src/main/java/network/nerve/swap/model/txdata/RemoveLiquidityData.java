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
public class RemoveLiquidityData extends BaseNulsData {

    private int assetChainIdA;
    private int assetIdA;
    private int assetChainIdB;
    private int assetIdB;
    private byte[] to;
    private long deadline;
    private BigInteger amountAMin;
    private BigInteger amountBMin;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(assetChainIdA);
        stream.writeUint16(assetIdA);
        stream.writeUint16(assetChainIdB);
        stream.writeUint16(assetIdB);
        stream.write(to);
        stream.writeUint32(deadline);
        stream.writeBigInteger(amountAMin);
        stream.writeBigInteger(amountBMin);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.assetChainIdA = byteBuffer.readUint16();
        this.assetIdA = byteBuffer.readUint16();
        this.assetChainIdB = byteBuffer.readUint16();
        this.assetIdB = byteBuffer.readUint16();
        this.to = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.deadline = byteBuffer.readUint32();
        this.amountAMin = byteBuffer.readBigInteger();
        this.amountBMin = byteBuffer.readBigInteger();
    }

    @Override
    public int size() {
        int size = 8;
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfBigInteger();
        return size;
    }

    public int getAssetChainIdA() {
        return assetChainIdA;
    }

    public void setAssetChainIdA(int assetChainIdA) {
        this.assetChainIdA = assetChainIdA;
    }

    public int getAssetIdA() {
        return assetIdA;
    }

    public void setAssetIdA(int assetIdA) {
        this.assetIdA = assetIdA;
    }

    public int getAssetChainIdB() {
        return assetChainIdB;
    }

    public void setAssetChainIdB(int assetChainIdB) {
        this.assetChainIdB = assetChainIdB;
    }

    public int getAssetIdB() {
        return assetIdB;
    }

    public void setAssetIdB(int assetIdB) {
        this.assetIdB = assetIdB;
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
