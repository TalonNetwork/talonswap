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
public class CreateFarmData extends BaseNulsData {
    private NerveToken stakeToken;
    private NerveToken syrupToken;
    private BigInteger syrupPerBlock;
    private long startBlockHeight;
    private byte[] adminAddress;
    private int multiple;
    private long multipleStopBlockHeight;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(stakeToken.getChainId());
        stream.writeUint16(stakeToken.getAssetId());
        stream.writeUint16(syrupToken.getChainId());
        stream.writeUint16(syrupToken.getAssetId());
        stream.writeBigInteger(syrupPerBlock);
        stream.writeInt64(startBlockHeight);
        stream.writeUint16(multiple);
        stream.writeInt64(multipleStopBlockHeight);
        stream.write(adminAddress);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.stakeToken = new NerveToken(byteBuffer.readUint16(), byteBuffer.readUint16());
        this.syrupToken = new NerveToken(byteBuffer.readUint16(), byteBuffer.readUint16());
        this.syrupPerBlock = byteBuffer.readBigInteger();
        this.startBlockHeight = byteBuffer.readInt64();
        this.multiple = byteBuffer.readUint16();
        this.multipleStopBlockHeight = byteBuffer.readInt64();
        this.adminAddress = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
    }

    @Override
    public int size() {
        int size = 4;
        size += 4;
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfInt64();
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfInt64();
        size += Address.ADDRESS_LENGTH;
        return size;
    }

    public NerveToken getStakeToken() {
        return stakeToken;
    }

    public void setStakeToken(NerveToken stakeToken) {
        this.stakeToken = stakeToken;
    }

    public NerveToken getSyrupToken() {
        return syrupToken;
    }

    public void setSyrupToken(NerveToken syrupToken) {
        this.syrupToken = syrupToken;
    }

    public BigInteger getSyrupPerBlock() {
        return syrupPerBlock;
    }

    public void setSyrupPerBlock(BigInteger syrupPerBlock) {
        this.syrupPerBlock = syrupPerBlock;
    }

    public long getStartBlockHeight() {
        return startBlockHeight;
    }

    public void setStartBlockHeight(long startBlockHeight) {
        this.startBlockHeight = startBlockHeight;
    }

    public byte[] getAdminAddress() {
        return adminAddress;
    }

    public void setAdminAddress(byte[] adminAddress) {
        this.adminAddress = adminAddress;
    }

    public int getMultiple() {
        return multiple;
    }

    public void setMultiple(int multiple) {
        this.multiple = multiple;
    }

    public long getMultipleStopBlockHeight() {
        return multipleStopBlockHeight;
    }

    public void setMultipleStopBlockHeight(long multipleStopBlockHeight) {
        this.multipleStopBlockHeight = multipleStopBlockHeight;
    }
}
