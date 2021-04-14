package network.nerve.swap.model.dto;

import network.nerve.swap.model.po.SwapPairPO;

import java.math.BigInteger;

/**
 * @author Niels
 */
public class SwapPairDTO {
    private SwapPairPO po;
    private BigInteger reserve0;
    private BigInteger reserve1;
    private BigInteger totalLP;
    private long blockTimeLast;
    private long blockHeightLast;

    public long getBlockTimeLast() {
        return blockTimeLast;
    }

    public void setBlockTimeLast(long blockTimeLast) {
        this.blockTimeLast = blockTimeLast;
    }

    public long getBlockHeightLast() {
        return blockHeightLast;
    }

    public void setBlockHeightLast(long blockHeightLast) {
        this.blockHeightLast = blockHeightLast;
    }

    public SwapPairPO getPo() {
        return po;
    }

    public void setPo(SwapPairPO po) {
        this.po = po;
    }

    public BigInteger getReserve0() {
        return reserve0;
    }

    public void setReserve0(BigInteger reserve0) {
        this.reserve0 = reserve0;
    }

    public BigInteger getReserve1() {
        return reserve1;
    }

    public void setReserve1(BigInteger reserve1) {
        this.reserve1 = reserve1;
    }

    public BigInteger getTotalLP() {
        return totalLP;
    }

    public void setTotalLP(BigInteger totalLP) {
        this.totalLP = totalLP;
    }

    @Override
    public SwapPairDTO clone() {
        SwapPairDTO dto = new SwapPairDTO();
        dto.setPo(po.clone());
        dto.setReserve0(new BigInteger(reserve0.toString()));
        dto.setReserve1(new BigInteger(reserve1.toString()));
        dto.setTotalLP(new BigInteger(totalLP.toString()));
        dto.setBlockHeightLast(blockHeightLast);
        dto.setBlockTimeLast(blockTimeLast);
        return dto;
    }
}