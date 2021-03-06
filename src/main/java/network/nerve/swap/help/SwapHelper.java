/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.TokenAmount;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.vo.RouteVO;
import network.nerve.swap.model.vo.SwapPairVO;
import network.nerve.swap.utils.SwapUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2021/5/8
 */
@Component
public class SwapHelper {

    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;

    public List<RouteVO> bestTradeExactIn(int chainId, List<String> pairs, TokenAmount tokenAmountIn, NerveToken out, int maxPairSize) throws NulsException {
        if (ledgerAssetCache.getLedgerAsset(tokenAmountIn.getToken()) == null || ledgerAssetCache.getLedgerAsset(out) == null) {
            throw new NulsException(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
        }
        List<SwapPairVO> swapPairs = new ArrayList<>();
        for (String pairAddress : pairs) {
            SwapPairDTO pairDTO = swapPairCache.get(pairAddress);
            if (pairDTO == null) {
                throw new NulsException(SwapErrorCode.PAIR_NOT_EXIST);
            }
            swapPairs.add(new SwapPairVO(pairDTO.getPo().getToken0(), pairDTO.getPo().getToken1()));
        }
        List<RouteVO> routes = SwapUtils.bestTradeExactIn(chainId, iPairFactory, swapPairs, tokenAmountIn, out, new LinkedHashSet<>(), new ArrayList<>(), tokenAmountIn, maxPairSize);
        return routes;
    }

}
