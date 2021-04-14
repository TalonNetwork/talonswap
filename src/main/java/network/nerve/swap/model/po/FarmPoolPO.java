package network.nerve.swap.model.po;

import io.nuls.base.data.NulsHash;
import network.nerve.swap.model.NerveToken;

import java.math.BigInteger;

/**
 * @author Niels
 */
public class FarmPoolPO {

    private NulsHash farmHash;
    private NerveToken stakeToken;
    private NerveToken syrupToken;
    private BigInteger syrupPerBlock;
    private long startBlockHeight;
    private byte[] adminAddress;
    private int multiple;
    private long multipleStopBlockHeight;
    private byte[] farmAddress;
    private BigInteger totalAllocPoint = BigInteger.ZERO;
    private BigInteger allocPoint;
    private long lastRewardBlock;
    private BigInteger accCakePerShare;

    public NulsHash getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(NulsHash farmHash) {
        this.farmHash = farmHash;
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

    public byte[] getFarmAddress() {
        return farmAddress;
    }

    public void setFarmAddress(byte[] farmAddress) {
        this.farmAddress = farmAddress;
    }

    public BigInteger getTotalAllocPoint() {
        return totalAllocPoint;
    }

    public void setTotalAllocPoint(BigInteger totalAllocPoint) {
        this.totalAllocPoint = totalAllocPoint;
    }

    public BigInteger getAllocPoint() {
        return allocPoint;
    }

    public void setAllocPoint(BigInteger allocPoint) {
        this.allocPoint = allocPoint;
    }

    public long getLastRewardBlock() {
        return lastRewardBlock;
    }

    public void setLastRewardBlock(long lastRewardBlock) {
        this.lastRewardBlock = lastRewardBlock;
    }

    public BigInteger getAccCakePerShare() {
        return accCakePerShare;
    }

    public void setAccCakePerShare(BigInteger accCakePerShare) {
        this.accCakePerShare = accCakePerShare;
    }
}
