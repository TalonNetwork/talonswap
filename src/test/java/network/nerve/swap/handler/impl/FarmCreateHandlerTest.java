package network.nerve.swap.handler.impl;

import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.LoggerBuilder;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.cache.impl.FarmCacherImpl;
import network.nerve.swap.config.ConfigBean;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.FarmTempManager;
import network.nerve.swap.manager.FarmUserInfoTempManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.NonceBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.tx.v1.helpers.FarmCreateTxHelper;
import network.nerve.swap.tx.v1.helpers.converter.LedgerService;
import network.nerve.swap.utils.NerveCallback;
import network.nerve.swap.utils.TxAssembleUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Niels
 */
public class FarmCreateHandlerTest {
    private FarmCreateHandler handler;
    private FarmCacherImpl farmCacher;
    private Chain chain;

    @Before
    public void init() {
        handler = new FarmCreateHandler();
        handler.setHelper(new FarmCreateTxHelper());

        ChainManager chainManager = new ChainManager();
        chain = new Chain();
        chain.setLogger(LoggerBuilder.getLogger(ModuleE.SW.name, 9));
        Chain.putCurrentThreadBlockType(0);
        chain.setBatchInfo(new BatchInfo());
        chain.getBatchInfo().setFarmTempManager(new FarmTempManager());
        chain.getBatchInfo().setFarmUserTempManager(new FarmUserInfoTempManager());
        ConfigBean cfg = new ConfigBean();
        cfg.setChainId(9);
        chain.setConfig(cfg);
        chainManager.getChainMap().put(9, chain);
        handler.setChainManager(chainManager);
        this.farmCacher = new FarmCacherImpl();
        handler.getHelper().setFarmCacher(farmCacher);
        handler.getHelper().setLedgerService(new LedgerService() {
            @Override
            public boolean existNerveAsset(int chainId, int assetChainId, int assetId) throws NulsException {

                return (assetChainId == 1 || assetChainId == 9) && assetId == 1;
            }

            @Override
            public LedgerAssetDTO getNerveAsset(int chainId, int assetChainId, int assetId) {
                return null;
            }

            @Override
            public NonceBalance getBalanceNonce(int chainId, int assetChainId, int assetId, String address) throws NulsException {
                return null;
            }

            @Override
            public LedgerBalance getLedgerBalance(int chainId, int assetChainId, int assetId, String address) throws NulsException {
                return null;
            }
        });
        handler.getHelper().setStorageService(new FarmStorageService() {


            @Override
            public FarmPoolPO save(int chainId, FarmPoolPO po) {
                return po;
            }

            @Override
            public boolean delete(int chainId, byte[] hash) {
                return true;
            }

            @Override
            public List<FarmPoolPO> getList(int chainId) {
                return new ArrayList<>();
            }
        });
    }


    @Test
    public void testExecute() throws IOException {
        List<JunitCase> items = new ArrayList<>();

        items.add(getNormalCase());
        items.add(getCase0());
        items.add(getCase1());
        items.add(getCase2());


        JunitExecuter<FarmCreateHandler> executer = new JunitExecuter<>() {
            @Override
            public Object execute(JunitCase<FarmCreateHandler> junitCase) {
                return junitCase.getObj().execute(9, (Transaction) junitCase.getParams()[0], (long) junitCase.getParams()[1], (long) junitCase.getParams()[2]);
            }
        };
        JunitUtils.execute(items, executer);
    }

    private JunitCase getCase2() throws IOException {
        Transaction tx1 = TxAssembleUtil.asmbFarmCreate(new NerveToken(9, 1), new NerveToken(1, 1), new ECKey().getPrivKeyBytes());
        tx1.setTransactionSignature(null);
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertFalse(result.isSuccess());
                FarmPoolPO farm = chain.getBatchInfo().getFarmTempManager().getFarm(((Transaction) junitCase.getParams()[0]).getHash().toHex());

                assertNull(farm);

                System.out.println("[通过]Test Farm-Create tx execute: wrong signature！");
            }
        };
        return new JunitCase("创建farm-资产不存在", handler, new Object[]{tx1, 10000L, tx1.getTime()}, null, false, null, callback1);
    }

    private JunitCase getCase1() throws IOException {
        ECKey ecKey = new ECKey();
        Transaction tx1 = TxAssembleUtil.asmbFarmCreate(new NerveToken(5, 1), new NerveToken(1, 1), ecKey.getPrivKeyBytes());
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertFalse(result.isSuccess());
                FarmPoolPO farm = chain.getBatchInfo().getFarmTempManager().getFarm(((Transaction) junitCase.getParams()[0]).getHash().toHex());

                assertNull(farm);

                System.out.println("[通过]Test Farm-Create tx execute: 资产不存在！");
            }
        };
        return new JunitCase("创建farm-资产不存在", handler, new Object[]{tx1, 1L, tx1.getTime()}, null, false, null, callback1);
    }

    private JunitCase getCase0() throws IOException {
        ECKey ecKey = new ECKey();
        Transaction tx1 = TxAssembleUtil.asmbFarmCreate(new NerveToken(1, 1), new NerveToken(1, 1), ecKey.getPrivKeyBytes());
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                FarmPoolPO farm = chain.getBatchInfo().getFarmTempManager().getFarm(((Transaction) junitCase.getParams()[0]).getHash().toHex());

                assertNotNull(farm);
                assertNotNull(farm.getFarmHash());
                assertNotNull(farm.getStartBlockHeight());
                assertNotNull(farm.getLockedTime());
                assertNotNull(farm.getCreatorAddress());
                assertNotNull(farm.getSyrupTokenBalance());
                assertNotNull(farm.getStakeToken());
                assertNotNull(farm.getSyrupPerBlock());
                assertNotNull(farm.getSyrupToken());
                assertNotNull(farm.getLastRewardBlock());
                assertNotNull(farm.getAccSyrupPerShare());
                assertNotNull(farm.getStakeTokenBalance());

                System.out.println("[通过]Test Farm-Create tx execute: 2种资产一致！");
            }
        };
        return new JunitCase("创建farm-资产一样", handler, new Object[]{tx1, 1L, tx1.getTime()}, null, false, null, callback1);
    }

    private JunitCase getNormalCase() throws IOException {
        ECKey ecKey = new ECKey();
        Transaction tx1 = TxAssembleUtil.asmbFarmCreate(new NerveToken(1, 1), new NerveToken(9, 1), ecKey.getPrivKeyBytes());
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                FarmPoolPO farm = chain.getBatchInfo().getFarmTempManager().getFarm(((Transaction) junitCase.getParams()[0]).getHash().toHex());

                assertNotNull(farm);
                assertNotNull(farm.getFarmHash());
                assertNotNull(farm.getStartBlockHeight());
                assertNotNull(farm.getLockedTime());
                assertNotNull(farm.getCreatorAddress());
                assertNotNull(farm.getSyrupTokenBalance());
                assertNotNull(farm.getStakeToken());
                assertNotNull(farm.getSyrupPerBlock());
                assertNotNull(farm.getSyrupToken());
                assertNotNull(farm.getLastRewardBlock());
                assertNotNull(farm.getAccSyrupPerShare());
                assertNotNull(farm.getStakeTokenBalance());
                System.out.println("[通过]Test Farm-Create tx execute: Normal process！");
            }
        };
        return new JunitCase("创建farm-成功0", handler, new Object[]{tx1, 1L, tx1.getTime()}, null, false, null, callback1);
    }
}