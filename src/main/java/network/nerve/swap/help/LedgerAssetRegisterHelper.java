/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.swap.help;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.swap.cache.LedgerAssetCacher;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.rpc.call.LedgerCall;
import network.nerve.swap.storage.SwapPairStorageService;
import network.nerve.swap.utils.SwapUtils;

import static network.nerve.swap.constant.SwapConstant.LP_TOKEN_DECIMALS;

/**
 * @author: mimi
 * @date: 2020-05-29
 */
@Component
public class LedgerAssetRegisterHelper {

    @Autowired
    private SwapPairStorageService swapPairStorageService;
    @Autowired
    private LedgerAssetCacher ledgerAssetCacher;

    public LedgerAssetDTO lpAssetReg(int chainId, NerveToken tokenA, NerveToken tokenB) throws Exception {
        String assetSymbol = lpTokenSymbol(tokenA, tokenB);
        String assetAddress = SwapUtils.getStringPairAddress(chainId, tokenA, tokenB);
        Integer lpAssetId = LedgerCall.lpAssetReg(chainId, assetSymbol, LP_TOKEN_DECIMALS, assetSymbol, assetAddress);
        byte[] assetAddressBytes = AddressTool.getAddress(assetAddress);
        SwapPairPO po = new SwapPairPO(assetAddressBytes);
        NerveToken tokenLP = new NerveToken(chainId, lpAssetId);
        NerveToken[] tokens = SwapUtils.tokenSort(tokenA, tokenB);
        po.setToken0(tokens[0]);
        po.setToken1(tokens[1]);
        po.setTokenLP(tokenLP);
        swapPairStorageService.savePair(assetAddressBytes, po);
        return new LedgerAssetDTO(chainId, lpAssetId, assetSymbol, assetSymbol, LP_TOKEN_DECIMALS);
    }

    public SwapPairPO deleteLpAsset(int chainId, NerveToken tokenA, NerveToken tokenB) throws Exception {
        String assetAddress = SwapUtils.getStringPairAddress(chainId, tokenA, tokenB);
        SwapPairPO pair = swapPairStorageService.getPair(assetAddress);
        LedgerCall.lpAssetDelete(pair.getTokenLP().getAssetId());
        swapPairStorageService.delelePair(assetAddress);
        return pair;
    }

    private String lpTokenSymbol(NerveToken tokenA, NerveToken tokenB) {
        NerveToken[] tokens = SwapUtils.tokenSort(tokenA, tokenB);
        LedgerAssetDTO ledgerAsset0 = ledgerAssetCacher.getLedgerAsset(tokens[0]);
        LedgerAssetDTO ledgerAsset1 = ledgerAssetCacher.getLedgerAsset(tokens[1]);
        return new StringBuilder(ledgerAsset0.getAssetSymbol()).append("_").append(ledgerAsset1.getAssetSymbol()).append("_LP").toString();
    }


}
