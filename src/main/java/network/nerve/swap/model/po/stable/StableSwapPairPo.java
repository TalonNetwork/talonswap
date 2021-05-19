package network.nerve.swap.model.po.stable;

import io.nuls.base.basic.AddressTool;
import network.nerve.swap.model.NerveToken;

import java.util.Arrays;

/**
 * @author Niels
 */
public class StableSwapPairPo {
    private byte[] address;
    private NerveToken tokenLP;
    private NerveToken[] coins;

    public StableSwapPairPo(byte[] address) {
        this.address = address;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public NerveToken getTokenLP() {
        return tokenLP;
    }

    public void setTokenLP(NerveToken tokenLP) {
        this.tokenLP = tokenLP;
    }

    public NerveToken[] getCoins() {
        return coins;
    }

    public void setCoins(NerveToken[] coins) {
        this.coins = coins;
    }

    @Override
    public StableSwapPairPo clone() {
        StableSwapPairPo po = new StableSwapPairPo(address);
        po.setTokenLP(tokenLP);
        po.setCoins(coins);
        return po;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"address\":")
                .append('\"').append(AddressTool.getStringAddressByBytes(address)).append('\"');
        sb.append(",\"tokenLP\":")
                .append('\"').append(tokenLP).append('\"');
        sb.append(",\"coins\":")
                .append(Arrays.deepToString(coins));
        sb.append('}');
        return sb.toString();
    }
}