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
package network.nerve.swap.utils;

import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.TokenAmount;
import network.nerve.swap.model.vo.RouteVO;
import network.nerve.swap.model.vo.SwapPairVO;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2021/5/19
 */
public class SwapTokenTradeRouteTest {

    List<PairTest> pairs;
    @Before
    public void before() {
        pairs = new ArrayList<>();
        pairs.add(new PairTest(sortTokens('A', 'B')));
        pairs.add(new PairTest(sortTokens('A', 'C')));
        pairs.add(new PairTest(sortTokens('B', 'C')));
        pairs.add(new PairTest(sortTokens('B', 'E')));
        pairs.add(new PairTest(sortTokens('C', 'D')));
        pairs.add(new PairTest(sortTokens('C', 'E')));
        pairs.add(new PairTest(sortTokens('D', 'E')));
        pairs.add(new PairTest(sortTokens('F', 'C')));
        pairs.add(new PairTest(sortTokens('C', 'H')));
        pairs.add(new PairTest(sortTokens('A', 'E')));
    }

    @Test
    public void testSubList() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.add(6);
        List<Integer> subList0 = subList(list, 0, 2);
        subList0.addAll(subList(list, 3, list.size()));
        System.out.println(subList0);
    }

    @Test
    public void sortRouteVOTest() {
        List<RouteVO> routes = new ArrayList<>();
        int k0 = 5;
        routes.add(new RouteVO(List.of(
                new SwapPairVO(new NerveToken(k0, 1), new NerveToken(k0, 2)), 
                new SwapPairVO(new NerveToken(k0, 2), new NerveToken(k0, 3)),
                new SwapPairVO(new NerveToken(k0, 3), new NerveToken(k0, 4))),
                new TokenAmount(new NerveToken(k0, 1), BigInteger.valueOf(200)), new TokenAmount(new NerveToken(k0, 4), BigInteger.valueOf(800))));
        k0 = 6;
        routes.add(new RouteVO(List.of(
                new SwapPairVO(new NerveToken(k0, 1), new NerveToken(k0, 2)),
                new SwapPairVO(new NerveToken(k0, 2), new NerveToken(k0, 3)),
                new SwapPairVO(new NerveToken(k0, 3), new NerveToken(k0, 4))),
                new TokenAmount(new NerveToken(k0, 1), BigInteger.valueOf(200)), new TokenAmount(new NerveToken(k0, 4), BigInteger.valueOf(600))));
        k0 = 7;
        routes.add(new RouteVO(List.of(
                new SwapPairVO(new NerveToken(k0, 1), new NerveToken(k0, 2)),
                new SwapPairVO(new NerveToken(k0, 2), new NerveToken(k0, 3)),
                new SwapPairVO(new NerveToken(k0, 3), new NerveToken(k0, 4))),
                new TokenAmount(new NerveToken(k0, 1), BigInteger.valueOf(200)), new TokenAmount(new NerveToken(k0, 4), BigInteger.valueOf(700))));
        k0 = 8;
        routes.add(new RouteVO(List.of(
                new SwapPairVO(new NerveToken(k0, 1), new NerveToken(k0, 2)),
                new SwapPairVO(new NerveToken(k0, 2), new NerveToken(k0, 3)),
                new SwapPairVO(new NerveToken(k0, 3), new NerveToken(k0, 4))),
                new TokenAmount(new NerveToken(k0, 1), BigInteger.valueOf(200)), new TokenAmount(new NerveToken(k0, 4), BigInteger.valueOf(900))));

        routes.sort(RouteVOSort.INSTANCE);

        System.out.println(Arrays.deepToString(routes.toArray()));
    }

    @Test
    public void testCalPaths() {
        List<RouteTest> routeTests = calPaths(pairs, 'A', 'E', new LinkedHashSet<>(), new ArrayList<>(), 0, 2);
        System.out.println(Arrays.deepToString(routeTests.toArray()));
    }

    public List<RouteTest> calPaths(List<PairTest> pairs, char in, char out, LinkedHashSet<PairTest> currentPath, List<RouteTest> bestRouteTest, int depth, int maxPairSize) {
        System.out.println(String.format("depth: %s, in: %s, out: %s, pairs: %s", depth, in, out, Arrays.deepToString(pairs.toArray())));
        int length = pairs.size();
        for (int i = 0; i < length; i++) {
            System.out.println(String.format("depth: %s, i: %s", depth, i));
            PairTest pair = pairs.get(i);
            if (pair.token0 != in && pair.token1 != in) continue;
            //if (currentPath.contains(pair)) continue;
            char tokenOut = pair.token0 == in ? pair.token1 : pair.token0;
            if (containsCurrency(currentPath, tokenOut)) continue;
            System.out.println(String.format("depth: %s, i: %s", depth, i));

            System.out.println(String.format("depth: %s, 选中的Pair: %s, 选中的tokenOut: %s", depth, pair.toString(), tokenOut));
            if (tokenOut == out) {
                currentPath.add(pair);
                bestRouteTest.add(new RouteTest(currentPath.stream().collect(Collectors.toList()), depth));
            } else if (depth < (maxPairSize - 1) && pairs.size() > 1){
                LinkedHashSet cloneLinkedHashSet = cloneLinkedHashSet(currentPath);
                cloneLinkedHashSet.add(pair);
                System.out.println(String.format("depth: %s, 移除的Pair: %s", depth, pair.toString()));
                System.out.println();
                List<PairTest> subList = subList(pairs, 0, i);
                subList.addAll(subList(pairs, i + 1, length));
                calPaths(subList, tokenOut, out, cloneLinkedHashSet, bestRouteTest, depth + 1, maxPairSize);
            }
        }
        return bestRouteTest;
    }

    LinkedHashSet cloneLinkedHashSet(LinkedHashSet set) {
        LinkedHashSet<Object> objects = new LinkedHashSet<>();
        objects.addAll(set);
        return objects;
    }

    List subList(List list, int fromIndex, int toIndex) {
        List objs = new ArrayList();
        for (int i = fromIndex, length = toIndex; i < length; i++) {
            objs.add(list.get(i));
        }
        return objs;
    }

    private boolean containsCurrency(LinkedHashSet<PairTest> currentPath, char tokenOut) {
        for (PairTest pair : currentPath) {
            if (pair.hasToken(tokenOut)) {
                return true;
            }
        }
        return false;
    }

    public char[] sortTokens(char tokenA, char tokenB) {
        return tokenA > tokenB ? new char[]{tokenB, tokenA} : new char[]{tokenA, tokenB};
    }

    static class RouteTest {
        List<PairTest> path = new ArrayList<>();
        int depth;

        public RouteTest(List<PairTest> path, int depth) {
            this.path = path;
            this.depth = depth;
        }

        public List<PairTest> getPath() {
            return path;
        }

        public void setPath(List<PairTest> path) {
            this.path = path;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{");
            sb.append("\"path\":")
                    .append(Arrays.deepToString(path.toArray()));
            sb.append(", \"depth\":")
                    .append(depth);
            sb.append('}');
            return sb.toString();
        }
    }

    static class PairTest {
        char token0;
        char token1;

        public PairTest(char[] tokens) {
            this.token0 = tokens[0];
            this.token1 = tokens[1];
        }

        public PairTest(char token0, char token1) {
            this.token0 = token0;
            this.token1 = token1;
        }

        public char getToken0() {
            return token0;
        }

        public void setToken0(char token0) {
            this.token0 = token0;
        }

        public char getToken1() {
            return token1;
        }

        public void setToken1(char token1) {
            this.token1 = token1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PairTest pairTest = (PairTest) o;

            if (token0 != pairTest.token0) return false;
            if (token1 != pairTest.token1) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) token0;
            result = 31 * result + (int) token1;
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{");
            sb.append("\"pair\":")
                    .append('\"').append(token0).append("-").append(token1).append('\"');
            sb.append('}');
            return sb.toString();
        }

        public boolean hasToken(char tokenOut) {
            return token0 == tokenOut || token1 == tokenOut;
        }
    }

}
