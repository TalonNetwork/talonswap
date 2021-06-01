package network.nerve.swap.model.dto.stable;

import network.nerve.swap.model.po.stable.StableSwapPairPo;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author Niels
 */
public class StableSwapPairDTO {
    private StableSwapPairPo po;
    private BigInteger totalLP;
    private BigInteger[] balances;
    private long blockTimeLast;
    private long blockHeightLast;

    public StableSwapPairPo getPo() {
        return po;
    }

    public void setPo(StableSwapPairPo po) {
        this.po = po;
    }

    public BigInteger getTotalLP() {
        return totalLP == null ? BigInteger.ZERO : totalLP;
    }

    public void setTotalLP(BigInteger totalLP) {
        this.totalLP = totalLP;
    }


    public BigInteger[] getBalances() {
        return balances;
    }

    public void setBalances(BigInteger[] balances) {
        this.balances = balances;
    }

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

    @Override
    public StableSwapPairDTO clone() {
        StableSwapPairDTO dto = new StableSwapPairDTO();
        dto.setPo(po.clone());
        dto.setTotalLP(new BigInteger(totalLP.toString()));
        BigInteger[] cloneBalances = new BigInteger[balances.length];
        int i = 0;
        for (BigInteger balance: balances) {
            cloneBalances[i++] = new BigInteger(balance.toString());
        }
        dto.setBalances(cloneBalances);
        dto.setBlockHeightLast(blockHeightLast);
        dto.setBlockTimeLast(blockTimeLast);
        return dto;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"po\":")
                .append(po.toString());
        sb.append(",\"totalLP\":")
                .append(totalLP);
        sb.append(",\"balances\":")
                .append(Arrays.deepToString(balances));
        sb.append(",\"blockTimeLast\":")
                .append(blockTimeLast);
        sb.append(",\"blockHeightLast\":")
                .append(blockHeightLast);
        sb.append('}');
        return sb.toString();
    }
}
