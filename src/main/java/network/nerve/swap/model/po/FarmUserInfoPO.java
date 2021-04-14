package network.nerve.swap.model.po;

import java.math.BigInteger;

/**
 * @author Niels
 */
public class FarmUserInfoPO {
    private byte[] farmAddress;
    private byte[] userAddress;
    private BigInteger amount;
    private BigInteger rewardDebt;

    public byte[] getFarmAddress() {
        return farmAddress;
    }

    public void setFarmAddress(byte[] farmAddress) {
        this.farmAddress = farmAddress;
    }

    public byte[] getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(byte[] userAddress) {
        this.userAddress = userAddress;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public BigInteger getRewardDebt() {
        return rewardDebt;
    }

    public void setRewardDebt(BigInteger rewardDebt) {
        this.rewardDebt = rewardDebt;
    }
}
