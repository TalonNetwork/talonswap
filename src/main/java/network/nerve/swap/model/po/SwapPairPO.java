package network.nerve.swap.model.po;

import network.nerve.swap.model.NerveToken;

/**
 * @author Niels
 */
public class SwapPairPO {

    private byte[] address;

    private NerveToken token0;
    private NerveToken token1;
    private NerveToken tokenLP;

    public SwapPairPO(byte[] address) {
        this.address = address;
    }


    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public NerveToken getToken0() {
        return token0;
    }

    public void setToken0(NerveToken token0) {
        this.token0 = token0;
    }

    public NerveToken getToken1() {
        return token1;
    }

    public void setToken1(NerveToken token1) {
        this.token1 = token1;
    }

    public NerveToken getTokenLP() {
        return tokenLP;
    }

    public void setTokenLP(NerveToken tokenLP) {
        this.tokenLP = tokenLP;
    }

    @Override
    public SwapPairPO clone() {
        SwapPairPO po = new SwapPairPO(address);
        po.setToken0(token0.clone());
        po.setToken1(token1.clone());
        po.setTokenLP(tokenLP.clone());
        return po;
    }
}