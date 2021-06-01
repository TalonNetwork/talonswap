package network.nerve.swap.rpc.cmd;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.Parameter;
import io.nuls.core.rpc.model.Parameters;
import io.nuls.core.rpc.model.TypeDescriptor;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.swap.config.ConfigBean;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.NonceBalance;
import network.nerve.swap.model.business.AddLiquidityBus;
import network.nerve.swap.model.business.RemoveLiquidityBus;
import network.nerve.swap.model.business.SwapTradeBus;
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;
import network.nerve.swap.rpc.call.LedgerCall;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

import static network.nerve.swap.constant.SwapCmdConstant.*;

public class SwapTxSendTest {
    static String awardFeeSystemAddressPublicKey = "031672b023ef35e37eb1d570015b54e1c29a6cc57d5a11c4733e960efe7ca56b80";
    static String awardFeeSystemAddress;
    static String address20 = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
    static String address21 = "TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz";
    static String address22 = "TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw";
    static String address23 = "TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv";
    static String address24 = "TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD";
    static String address25 = "TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5";
    static String address26 = "TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg";
    static String address27 = "TNVTdTSPLqKoNh2uiLAVB76Jyq3D6h3oAR22n";
    static String address28 = "TNVTdTSPNkjaFbabm5P73m7VHBRQef4NDsgYu";
    static String address29 = "TNVTdTSPRMtpGNYRx98WkoqKnExU9pWDQjNPf";
    static String address30 = "TNVTdTSPEn3kK94RqiMffiKkXTQ2anRwhN1J9";
    static String address31 = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
    static String address32 = "TNVTdTSPQj7T5LiVmL974o2YRWa1YPJJzJHhn";
    static String address33 = "TNVTdTSPKy4iLwK6XC52VNqVSnk1vncF5Z2mu";
    static String address34 = "TNVTdTSPRgkZKoNaeAUj6H3UWH29D5ftv7LNN";
    static String address35 = "TNVTdTSPVhoYssF5cgMVGWRYsdai9KLs9rotk";
    static String address36 = "TNVTdTSPN9pNrMMEmhZsNYVn9Lcyu3cxSUbAL";
    static String address37 = "TNVTdTSPLnyxJ4gWi3L4mr6sSQrcfqLqPbCkP";
    static String address38 = "TNVTdTSPJiFqnqW2sGNVZ1do2C6tFoLv7DBgE";
    static String address39 = "TNVTdTSPTgPV9AjgQKFvdT1eviWisVMG7Naah";
    static String address40 = "TNVTdTSPGvLeBDxQiWRH3jZTcrYKwSF2axCfy";
    static String address41 = "TNVTdTSPPyoYQNDgfbF83P3kWJz9bvrNej1RW";
    static String address42 = "TNVTdTSPTNWUw7YiRLuwpFiPiUcpYQbzRU8LT";
    static String address43 = "TNVTdTSPSxopb3jVAdDEhx49S6iaA2CiPa3oa";
    static String address44 = "TNVTdTSPNc8YhE5h7Msd8R9Vebd5DG9W38Hd6";
    static String address45 = "TNVTdTSPPDMJA6eFRAb47vC2Lzx662nj3VVhg";
    static String address46 = "TNVTdTSPMXhkD6FJ9htA9H3aDEVVg8DNoriur";
    static String address47 = "TNVTdTSPPENjnLifQrJ4EK6tWp1HaDhnW5h7y";
    static String address48 = "TNVTdTSPLmeuz7aVsdb2WTcGXKFmcKowTfk46";
    static String address49 = "TNVTdTSPF1mBVywX7BR674SZbaHBn3JoPhyJi";
    static String address50 = "TNVTdTSPHR7jCTZwtEB6FS1BZuBe7RVjshEsB";
    static String address51 = "TNVTdTSPRrYndMR8JZ4wJovLDbRp2o4gGWDAp";
    private String privateKey20 = "9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b";
    private String privateKey21 = "477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75";
    private String privateKey22 = "8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78";
    private String privateKey23 = "4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530";
    private String privateKey24 = "bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7";
    private String privateKey25 = "ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200";
    private String privateKey26 = "4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a";
    private String privateKey27 = "3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1";
    private String privateKey28 = "27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e";
    private String privateKey29 = "76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b";
    private String privateKey30 = "B36097415F57FE0AC1665858E3D007BA066A7C022EC712928D2372B27E8513FF";
    private String privateKey31 = "4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39";
    private String privateKey32 = "e70ea2ebe146d900bf84bc7a96a02f4802546869da44a23c29f599c7e42001da";
    private String privateKey33 = "4c6b4c5d9b07e364d6b306d1afe0f2c37e15c64ac5151a395a4c570f00ce867d";
    private String privateKey34 = "2fea28f438a104062e4dcd79427282573053a6b762e68b942055221462c46f02";
    private String privateKey35 = "08407198c196c950afffd326a00321a5ea563b3beaf640d462f3a274319b753d";
    private String privateKey36 = "be9dd9fb419ede7188b45451445525669d5e9d256bd3f938ecae177194867aa1";
    private String privateKey37 = "9769cdc13af8da759ba985156876072986e9e10deb5fe41fe8406567071b0a71";
    private String privateKey38 = "9887b7e02b098a421b406223c3ec5b92889d294f4ed84a0d53018dced35cff41";
    private String privateKey39 = "7ec6ae2f4da0b80c0120ea96e8ce8973623ccaed36f5c2145032ac453dc006f0";
    private String privateKey40 = "bd08d1cd9a1f319a0c0439b29029f7e46584c56126fd30f02c0b6fb5fb8e4144";
    private String privateKey41 = "48348fff812b049024efcd2b3481ada1cfdeb3deabb56e4ba9d84c2ebb3a8a1f";
    private String privateKey42 = "f13495414167ffc3254ef93a0fc47102f721a556d1fb595f5cc130021cbcc67a";
    private String privateKey43 = "2c30389340122e20b9462a418979dcced200549c4aa7e42f189425ecedb18b2a";
    private String privateKey44 = "19397598342ea2adf2162c0dc9a00381bdaa28a3a481ba9f6fa70afa3040625d";
    private String privateKey45 = "8e81aab76c78c07d3304d0c03e7790423b3e28b9851756d0b1ac962ac1acb504";
    private String privateKey46 = "9d1f84b2b3c1f53498abb436d87a32af793978d22bc76fc2b6fa1971b117ff63";
    private String privateKey47 = "0bd10e2fe13ca8d6d91b43e0518d5ad06adaad9a78520a39d8db00ed62d45dd4";
    private String privateKey48 = "01f72a7d50655939b60c4f79ea6dd2661b435d49ce81467d91f5b85f4a26c112";
    private String privateKey49 = "c0102d3f66edf0fd8939fb149bbe5a5f6503e8a7bf41b80b8b5a0312c6ced3a7";
    private String privateKey50 = "d92a08dafcec90ba2e08cc825c6f74c41058b9bc325f61ffa1fddaf27a358f3b";
    private String privateKey51 = "efc10e6831a87ba71dad9c3769b07875a0eb9b8ced5139125f05a58d0f0c599f";

    static String agentAddress;
    static String packageAddress;
    static String packageAddressPrivateKey;
    String packageAddressZP = "TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd";
    String packageAddressNE = "TNVTdTSPNeoGxTS92S2r1DZAtJegbeucL8tCT";
    String packageAddressHF = "TNVTdTSPLpegzD3B6qaVKhfj6t8cYtnkfR7Wx";// 0x16534991E80117Ca16c724C991aad9EAbd1D7ebe
    String packageAddress6 = "TNVTdTSPF9nBiba1vk4PqRkyQaYqwoAJX95xn";// 0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65
    String packageAddress7 = "TNVTdTSPKDqbuQc6cF3m41CcQKRvzmXSQzouy";// 0xd29E172537A3FB133f790EBE57aCe8221CB8024F
    String packageAddress8 = "TNVTdTSPS9g9pGmjEo2gjjGKsNBGc22ysz25a";// 0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17
    String packageAddressPrivateKeyZP = "b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5";
    String packageAddressPrivateKeyNE = "188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f";
    String packageAddressPrivateKeyHF = "fbcae491407b54aa3904ff295f2d644080901fda0d417b2b427f5c1487b2b499";
    String packageAddressPrivateKey6 = "43DA7C269917207A3CBB564B692CD57E9C72F9FCFDB17EF2190DD15546C4ED9D";
    String packageAddressPrivateKey7 = "0935E3D8C87C2EA5C90E3E3A0509D06EB8496655DB63745FAE4FF01EB2467E85";
    String packageAddressPrivateKey8 = "CCF560337BA3DE2A76C1D08825212073B299B115474B65DE4B38B587605FF7F2";

    private Chain chain;
    static int chainId = 5;
    static int assetId = 1;
    static int ethChainId = 101;
    private String from;
    static String version = "1.0";
    static String password = "nuls123456";//"nuls123456";

    int ethAssetId = 11;
    int bscAssetId = 10;
    int htAssetId = 8;
    int oktAssetId = 6;
    int swapLpAssetId = 0;
    int stableLpAssetId = 14;
    protected NerveToken nvt = new NerveToken(chainId, 1);
    protected NerveToken usdx_eth = new NerveToken(chainId, ethAssetId);
    protected NerveToken usdx_bnb = new NerveToken(chainId, bscAssetId);
    protected NerveToken usdx_ht = new NerveToken(chainId, htAssetId);
    protected NerveToken usdx_okt = new NerveToken(chainId, oktAssetId);

    protected NerveToken usdi_eth = new NerveToken(chainId, 11);
    protected NerveToken goat_ht = new NerveToken(chainId, 8);
    protected NerveToken usdt_okt = new NerveToken(chainId, 6);

    protected NerveToken swap_lp = new NerveToken(chainId, swapLpAssetId);
    protected NerveToken stable_swap_lp = new NerveToken(chainId, stableLpAssetId);

    public static void importPriKey(String priKey, String pwd) {
        try {
            //账户已存在则覆盖 If the account exists, it covers.
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("priKey", priKey);
            params.put("password", pwd);
            params.put("overwrite", true);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_importAccountByPriKey", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_importAccountByPriKey");
            String address = (String) result.get("address");
            Log.debug("importPriKey success! address-{}", address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void beforeClass() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger("io.netty");
        logger.setAdditive(false);
        logger.setLevel(Level.ERROR);
    }

    @Before
    public void before() throws Exception {
        AddressTool.addPrefix(5, "TNVT");
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":8771");
        chain = new Chain();
        chain.setConfig(new ConfigBean(chainId, assetId, "UTF-8"));
        from = address31;
        awardFeeSystemAddress = AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(awardFeeSystemAddressPublicKey, chainId));
        // 设置共识节点地址和出块地址
        packageZP();
    }

    // 0x09534d4692F568BC6e9bef3b4D84d48f19E52501 [Account3]
    // 0xF3c90eF58eC31805af11CE5FA6d39E395c66441f [Account4]
    // 0x6afb1F9Ca069bC004DCF06C51B42992DBD90Adba [Account5]
    // 私钥: 43DA7C269917207A3CBB564B692CD57E9C72F9FCFDB17EF2190DD15546C4ED9D / 0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65 [Account 6] / tNULSeBaMfmpwBtUSHyLCGHq4WqYY5A4Dxak91
    // 私钥: 0935E3D8C87C2EA5C90E3E3A0509D06EB8496655DB63745FAE4FF01EB2467E85 / 0xd29E172537A3FB133f790EBE57aCe8221CB8024F [Account 7] / tNULSeBaMjqtMNhWWyUKZUsGhWaRd88RMrSU6J
    // 私钥: CCF560337BA3DE2A76C1D08825212073B299B115474B65DE4B38B587605FF7F2 / 0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17 [Account 8] / tNULSeBaMrmiuHZg9c2JVAbLQydAxjNvuKRgFj
    @Test
    public void importPriKeyTest() {
        // HF: 0x16534991E80117Ca16c724C991aad9EAbd1D7ebe
        //公钥: 037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863
        //importPriKey("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5", password);//种子出块地址 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp, 0xdd7CBEdDe731e78e8b8E4b2c212bC42fA7C09D03
        //公钥: 036c0c9ae792f043e14d6a3160fa37e9ce8ee3891c34f18559e20d9cb45a877c4b
        //importPriKey("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f", password);//种子出块地址 tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe, 0xD16634629C638EFd8eD90bB096C216e7aEc01A91
        importPriKey("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b", password);//20 tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG
        importPriKey("477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75", password);//21 tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD
        importPriKey("8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78", password);//22 tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24
        importPriKey("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530", password);//23 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD
        importPriKey("bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7", password);//24 tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL
        importPriKey("ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200", password);//25 tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL
        importPriKey("4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a", password);//26 tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm
        importPriKey("3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1", password);//27 tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1
        importPriKey("27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e", password);//28 tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2
        importPriKey("76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b", password);//29 tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn
        importPriKey("B36097415F57FE0AC1665858E3D007BA066A7C022EC712928D2372B27E8513FF", password);//30 ETH 测试网地址 tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ
        importPriKey("4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39", password);//31 测试网地址 tNULSeBaMrQaVh1V7LLvbKa5QSN54bS4sdbXaF, 0xc11D9943805e56b630A401D4bd9A29550353EFa1 [Account 9]
        importPriKey(packageAddressPrivateKey, password);
        //=================================================================//
        importPriKey("e70ea2ebe146d900bf84bc7a96a02f4802546869da44a23c29f599c7e42001da", password);//32 TNVTdTSPQj7T5LiVmL974o2YRWa1YPJJzJHhn
        importPriKey("4c6b4c5d9b07e364d6b306d1afe0f2c37e15c64ac5151a395a4c570f00ce867d", password);//33 TNVTdTSPKy4iLwK6XC52VNqVSnk1vncF5Z2mu
        importPriKey("2fea28f438a104062e4dcd79427282573053a6b762e68b942055221462c46f02", password);//34 TNVTdTSPRgkZKoNaeAUj6H3UWH29D5ftv7LNN
        importPriKey("08407198c196c950afffd326a00321a5ea563b3beaf640d462f3a274319b753d", password);//35 TNVTdTSPVhoYssF5cgMVGWRYsdai9KLs9rotk
        importPriKey("be9dd9fb419ede7188b45451445525669d5e9d256bd3f938ecae177194867aa1", password);//36 TNVTdTSPN9pNrMMEmhZsNYVn9Lcyu3cxSUbAL
        importPriKey("9769cdc13af8da759ba985156876072986e9e10deb5fe41fe8406567071b0a71", password);//37 TNVTdTSPLnyxJ4gWi3L4mr6sSQrcfqLqPbCkP
        importPriKey("9887b7e02b098a421b406223c3ec5b92889d294f4ed84a0d53018dced35cff41", password);//38 TNVTdTSPJiFqnqW2sGNVZ1do2C6tFoLv7DBgE
        importPriKey("7ec6ae2f4da0b80c0120ea96e8ce8973623ccaed36f5c2145032ac453dc006f0", password);//39 TNVTdTSPTgPV9AjgQKFvdT1eviWisVMG7Naah
        importPriKey("bd08d1cd9a1f319a0c0439b29029f7e46584c56126fd30f02c0b6fb5fb8e4144", password);//40 TNVTdTSPGvLeBDxQiWRH3jZTcrYKwSF2axCfy
        importPriKey("48348fff812b049024efcd2b3481ada1cfdeb3deabb56e4ba9d84c2ebb3a8a1f", password);//41 TNVTdTSPPyoYQNDgfbF83P3kWJz9bvrNej1RW
        importPriKey("f13495414167ffc3254ef93a0fc47102f721a556d1fb595f5cc130021cbcc67a", password);//42 TNVTdTSPTNWUw7YiRLuwpFiPiUcpYQbzRU8LT
        importPriKey("2c30389340122e20b9462a418979dcced200549c4aa7e42f189425ecedb18b2a", password);//43 TNVTdTSPSxopb3jVAdDEhx49S6iaA2CiPa3oa
        importPriKey("19397598342ea2adf2162c0dc9a00381bdaa28a3a481ba9f6fa70afa3040625d", password);//44 TNVTdTSPNc8YhE5h7Msd8R9Vebd5DG9W38Hd6
        importPriKey("8e81aab76c78c07d3304d0c03e7790423b3e28b9851756d0b1ac962ac1acb504", password);//45 TNVTdTSPPDMJA6eFRAb47vC2Lzx662nj3VVhg
        importPriKey("9d1f84b2b3c1f53498abb436d87a32af793978d22bc76fc2b6fa1971b117ff63", password);//46 TNVTdTSPMXhkD6FJ9htA9H3aDEVVg8DNoriur
        importPriKey("0bd10e2fe13ca8d6d91b43e0518d5ad06adaad9a78520a39d8db00ed62d45dd4", password);//47 TNVTdTSPPENjnLifQrJ4EK6tWp1HaDhnW5h7y
        importPriKey("01f72a7d50655939b60c4f79ea6dd2661b435d49ce81467d91f5b85f4a26c112", password);//48 TNVTdTSPLmeuz7aVsdb2WTcGXKFmcKowTfk46
        importPriKey("c0102d3f66edf0fd8939fb149bbe5a5f6503e8a7bf41b80b8b5a0312c6ced3a7", password);//49 TNVTdTSPF1mBVywX7BR674SZbaHBn3JoPhyJi
        importPriKey("d92a08dafcec90ba2e08cc825c6f74c41058b9bc325f61ffa1fddaf27a358f3b", password);//50 TNVTdTSPHR7jCTZwtEB6FS1BZuBe7RVjshEsB
        importPriKey("efc10e6831a87ba71dad9c3769b07875a0eb9b8ced5139125f05a58d0f0c599f", password);//51 TNVTdTSPRrYndMR8JZ4wJovLDbRp2o4gGWDAp

        //importPriKey("a0282c3f197bae3345938595aba2296affae60fbafa7e2723910248466718858", password);//
    }

    @Test
    public void transfer() throws Exception {
        Map transferMap = new HashMap();
        transferMap.put("chainId", chainId);
        transferMap.put("remark", "abc");
        List<CoinDTO> inputs = new ArrayList<>();
        List<CoinDTO> outputs = new ArrayList<>();

        outputs.add(new CoinDTO(address22, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address23, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address24, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address25, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address26, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address27, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address28, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address29, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address30, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address31, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));

        outputs.add(new CoinDTO(address32, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address33, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address34, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address35, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address36, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address37, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address38, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address39, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address40, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address41, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));

        outputs.add(new CoinDTO(packageAddressZP, chainId, 1, BigInteger.valueOf(10000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(packageAddressNE, chainId, 1, BigInteger.valueOf(10000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(packageAddressHF, chainId, 1, BigInteger.valueOf(10000_0000_0000L), password, 0));

        BigInteger inAmount = BigInteger.valueOf(10_0000L);
        for (CoinDTO dto : outputs) {
            inAmount = inAmount.add(dto.getAmount());
        }
        inputs.add(new CoinDTO(address21, chainId, 1, inAmount, password, 0));

        transferMap.put("inputs", inputs);
        transferMap.put("outputs", outputs);

        //调用接口
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1")
    })
    @Test
    public void swapCreatePair() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("tokenAStr", nvt.str());
        params.put("tokenBStr", usdx_bnb.str());
        this.sendTx(from, SWAP_CREATE_PAIR, params);
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amountA", parameterType = "String", parameterDes = "添加的资产A的数量"),
            @Parameter(parameterName = "amountB", parameterType = "String", parameterDes = "添加的资产B的数量"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1"),
            @Parameter(parameterName = "amountAMin", parameterType = "String", parameterDes = "资产A最小添加值"),
            @Parameter(parameterName = "amountBMin", parameterType = "String", parameterDes = "资产B最小添加值"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "流动性份额接收地址")
    })
    @Test
    public void swapAddLiquidity() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("amountA", "20000");
        params.put("amountB", "600");
        params.put("tokenAStr", nvt.str());
        params.put("tokenBStr", usdx_bnb.str());
        BigInteger[] minAmounts = this.calMinAmountOnSwapAddLiquidity(params);
        params.put("amountAMin", minAmounts[0].toString());
        params.put("amountBMin", minAmounts[1].toString());
        params.put("deadline", deadline());
        params.put("to", address32);
        this.sendTx(from, SWAP_ADD_LIQUIDITY, params);
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amountLP", parameterType = "String", parameterDes = "移除的资产LP的数量"),
            @Parameter(parameterName = "tokenLPStr", parameterType = "String", parameterDes = "资产LP的类型，示例：1-1"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1"),
            @Parameter(parameterName = "amountAMin", parameterType = "String", parameterDes = "资产A最小移除值"),
            @Parameter(parameterName = "amountBMin", parameterType = "String", parameterDes = "资产B最小移除值"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "资产接收地址")
    })
    @Test
    public void swapRemoveLiquidity() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("amountLP", "1000");
        params.put("tokenLPStr", "5-12");
        params.put("tokenAStr", nvt.str());
        params.put("tokenBStr", usdx_bnb.str());
        BigInteger[] minAmounts = this.calMinAmountOnSwapRemoveLiquidity(params);
        System.out.println(String.format("minAmounts: %s", Arrays.deepToString(minAmounts)));
        params.put("amountAMin", minAmounts[0].toString());
        params.put("amountBMin", minAmounts[1].toString());
        params.put("deadline", deadline());
        params.put("to", address32);
        this.sendTx(address32, SWAP_REMOVE_LIQUIDITY, params);
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amountIn", parameterType = "String", parameterDes = "卖出的资产数量"),
            @Parameter(parameterName = "tokenPath", parameterType = "String[]", parameterDes = "币币交换资产路径，路径中最后一个资产，是用户要买进的资产，如卖A买B: [A, B] or [A, C, B]"),
            @Parameter(parameterName = "amountOutMin", parameterType = "String", parameterDes = "最小买进的资产数量"),
            @Parameter(parameterName = "feeTo", parameterType = "String", parameterDes = "交易手续费取出一部分给指定的接收地址"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "资产接收地址")
    })
    @Test
    public void swapTokenTrade() throws Exception {
        String tokenIn = nvt.str();
        String amountIn = "800";
        String tokenOut = usdx_bnb.str();
        String[] pairs = new String[]{SwapUtils.getStringPairAddress(chainId, nvt, usdx_bnb)};
        Map map = this.bestTradeExactIn(tokenIn, amountIn, tokenOut, 3, pairs);
        List<String> path = (List<String>) map.get("tokenPath");
        Map outMap = (Map) map.get("tokenAmountOut");

        Map<String, Object> params = new HashMap<>();
        params.put("amountIn", amountIn);
        params.put("tokenPath", path.toArray(new String[path.size()]));
        BigInteger amountOutMin = new BigInteger(outMap.get("amount").toString());
        System.out.println(String.format("amountOutMin: %s", amountOutMin));
        params.put("amountOutMin", amountOutMin);
        params.put("feeTo", address51);
        params.put("deadline", deadline());
        params.put("to", address32);
        this.sendTx(from, SWAP_TOKEN_TRADE, params);
    }


    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "coins", parameterType = "String[]", parameterDes = "资产类型列表，示例：[1-1, 1-2]")
    })
    @Test
    public void stableSwapCreatePair() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("coins", new String[]{usdx_eth.str(), usdx_bnb.str(), usdx_ht.str()});
        this.sendTx(from, STABLE_SWAP_CREATE_PAIR, params);
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amounts", parameterType = "String[]", parameterDes = "添加的资产数量列表"),
            @Parameter(parameterName = "tokens", parameterType = "String[]", parameterDes = "添加的资产类型列表，示例：[1-1, 1-2]"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "交易对地址"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "流动性份额接收地址")
    })
    @Test
    public void stableSwapAddLiquidity() throws Exception {
        NulsHash txHash = NulsHash.fromHex("1bc19b3450d8ad6ae96963012b124671f0cbb87964c16de59bf90df648b1c6ea");
        byte[] stablePairAddressBytes = AddressTool.getAddress(txHash.getBytes(), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);

        Map<String, Object> params = new HashMap<>();
        params.put("amounts", new String[]{"10000000000", "500000000"});
        params.put("tokens", new String[]{usdx_ht.str(), usdx_bnb.str()});
        params.put("pairAddress", stablePairAddress);
        params.put("deadline", deadline());
        params.put("to", address32);
        this.sendTx(from, STABLE_SWAP_ADD_LIQUIDITY, params);
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amountLP", parameterType = "String", parameterDes = "移除的资产LP的数量"),
            @Parameter(parameterName = "tokenLPStr", parameterType = "String", parameterDes = "资产LP的类型，示例：1-1"),
            @Parameter(parameterName = "receiveOrderIndexs", parameterType = "int[]", parameterDes = "按币种索引顺序接收资产"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "交易对地址"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "资产接收地址")
    })
    @Test
    public void stableSwapRemoveLiquidity() throws Exception {
        NulsHash txHash = NulsHash.fromHex("1bc19b3450d8ad6ae96963012b124671f0cbb87964c16de59bf90df648b1c6ea");
        byte[] stablePairAddressBytes = AddressTool.getAddress(txHash.getBytes(), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);

        Map<String, Object> params = new HashMap<>();
        params.put("amountLP", "38383800");
        params.put("tokenLPStr", stable_swap_lp.str());
        params.put("receiveOrderIndexs", new int[]{2, 1, 0});
        params.put("pairAddress", stablePairAddress);
        params.put("deadline", deadline());
        params.put("to", address32);
        this.sendTx(address32, STABLE_SWAP_REMOVE_LIQUIDITY, params);
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amountsIn", parameterType = "String[]", parameterDes = "卖出的资产数量列表"),
            @Parameter(parameterName = "tokensIn", parameterType = "String[]", parameterDes = "卖出的资产类型列表"),
            @Parameter(parameterName = "tokenOutIndex", parameterType = "int", parameterDes = "买进的资产索引"),
            @Parameter(parameterName = "feeTo", parameterType = "String", parameterDes = "交易手续费取出一部分给指定的接收地址"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "交易对地址"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "资产接收地址")
    })
    @Test
    public void stableSwapTokenTrade() throws Exception {
        NulsHash txHash = NulsHash.fromHex("1bc19b3450d8ad6ae96963012b124671f0cbb87964c16de59bf90df648b1c6ea");
        byte[] stablePairAddressBytes = AddressTool.getAddress(txHash.getBytes(), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);

        Map<String, Object> params = new HashMap<>();
        params.put("amountsIn", new String[]{"250000000"});
        params.put("tokensIn", new String[]{usdx_eth.str()});
        params.put("tokenOutIndex", 1);
        params.put("feeTo", address51);
        params.put("pairAddress", stablePairAddress);
        params.put("deadline", deadline());
        params.put("to", address32);
        this.sendTx(from, STABLE_SWAP_TOKEN_TRADE, params);
    }

    @Test
    public void getPairInfo() throws Exception {
        Map map = this.getSwapPairInfo(nvt.str(), usdx_bnb.str());
        System.out.println(JSONUtils.obj2PrettyJson(map));
    }

    @Test
    public void getStablePairInfo() throws Exception {
        NulsHash txHash = NulsHash.fromHex("1bc19b3450d8ad6ae96963012b124671f0cbb87964c16de59bf90df648b1c6ea");
        byte[] stablePairAddressBytes = AddressTool.getAddress(txHash.getBytes(), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
        Map map = this.getStableSwapPairInfo(stablePairAddress);
        System.out.println(JSONUtils.obj2PrettyJson(map));
    }

    @Test
    public void getResult() throws Exception {
        String hash = "b5b348a67108168caaac392da982be00feaad4357c722d4f0a752737c757b307";
        Map map = this.getSwapResultInfo(hash);
        System.out.println(JSONUtils.obj2PrettyJson(map));
        System.out.println();
        Object bus = this.desBusStr(map.get("txType"), map.get("business"));
        System.out.println(bus != null ? JSONUtils.obj2PrettyJson(bus) : "");
    }

    @Test
    public void getBestSwapPath() throws Exception {
        String tokenIn = nvt.str();
        String amountIn = "800";
        String tokenOut = usdx_bnb.str();
        String[] pairs = new String[]{SwapUtils.getStringPairAddress(chainId, nvt, usdx_bnb)};
        Map map = this.bestTradeExactIn(tokenIn, amountIn, tokenOut, 3, pairs);
        System.out.println(JSONUtils.obj2PrettyJson(map));
    }

    protected static Map<Integer, Class> busClassMap = new HashMap<>();
    static {
        busClassMap.put(TxType.SWAP_ADD_LIQUIDITY, AddLiquidityBus.class);
        busClassMap.put(TxType.SWAP_REMOVE_LIQUIDITY, RemoveLiquidityBus.class);
        busClassMap.put(TxType.SWAP_TRADE, SwapTradeBus.class);
        busClassMap.put(TxType.SWAP_ADD_LIQUIDITY_STABLE_COIN, StableAddLiquidityBus.class);
        busClassMap.put(TxType.SWAP_REMOVE_LIQUIDITY_STABLE_COIN, StableRemoveLiquidityBus.class);
        busClassMap.put(TxType.SWAP_TRADE_STABLE_COIN, StableSwapTradeBus.class);
    }

    protected Object desBusStr(Object txType, Object busStr) {
        if (txType == null || busStr == null) {
            return null;
        }
        Class aClass = busClassMap.get(Integer.parseInt(txType.toString()));
        if (aClass == null) {
            return null;
        }
        System.out.println(aClass.getSimpleName());
        return SwapDBUtil.getModel(HexUtil.decode(busStr.toString()), aClass);
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "amountA", parameterType = "String", parameterDes = "添加的资产A的数量"),
            @Parameter(parameterName = "amountB", parameterType = "String", parameterDes = "添加的资产B的数量"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1")
    })
    protected BigInteger[] calMinAmountOnSwapAddLiquidity(Map<String, Object> params) throws Exception {
        HashMap data = this.getData(SWAP_MIN_AMOUNT_ADD_LIQUIDITY, params);
        return new BigInteger[]{
                new BigInteger(data.get("amountAMin").toString()),
                new BigInteger(data.get("amountBMin").toString())
        };
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "amountLP", parameterType = "String", parameterDes = "移除的资产LP的数量"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1")
    })
    protected BigInteger[] calMinAmountOnSwapRemoveLiquidity(Map<String, Object> params) throws Exception {
        HashMap data = this.getData(SWAP_MIN_AMOUNT_REMOVE_LIQUIDITY, params);
        return new BigInteger[]{
                new BigInteger(data.get("amountAMin").toString()),
                new BigInteger(data.get("amountBMin").toString())
        };
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "amountIn", parameterType = "String", parameterDes = "卖出的资产数量"),
            @Parameter(parameterName = "tokenPath", parameterType = "String[]", parameterDes = "币币交换资产路径，路径中最后一个资产，是用户要买进的资产，如卖A买B: [A, B] or [A, C, B]")
    })
    protected BigInteger calMinAmountOnSwapTokenTrade(Map<String, Object> params) throws Exception {
        HashMap data = this.getData(SWAP_MIN_AMOUNT_TOKEN_TRADE, params);
        return new BigInteger(data.get("amountOutMin").toString());
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "tokenInStr", parameterType = "String", parameterDes = "卖出资产的类型，示例：1-1"),
            @Parameter(parameterName = "tokenInAmount", requestType = @TypeDescriptor(value = String.class), parameterDes = "卖出资产数量"),
            @Parameter(parameterName = "tokenOutStr", parameterType = "String", parameterDes = "买进资产的类型，示例：1-1"),
            @Parameter(parameterName = "maxPairSize", requestType = @TypeDescriptor(value = int.class), parameterDes = "交易最深路径"),
            @Parameter(parameterName = "pairs", requestType = @TypeDescriptor(value = String[].class), parameterDes = "当前网络所有交易对列表")
    })
    protected Map bestTradeExactIn(String tokenInStr, String tokenInAmount, String tokenOutStr, int maxPairSize, String[] pairs) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("tokenInStr", tokenInStr);
        params.put("tokenInAmount", tokenInAmount);
        params.put("tokenOutStr", tokenOutStr);
        params.put("maxPairSize", maxPairSize);
        params.put("pairs", pairs);
        HashMap data = this.getData(BEST_TRADE_EXACT_IN, params);
        return (Map) (data.get("value"));
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1")
    })
    protected Map getSwapPairInfo(String tokenAStr, String tokenBStr) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("tokenAStr", tokenAStr);
        params.put("tokenBStr", tokenBStr);
        HashMap data = this.getData(SWAP_PAIR_INFO, params);
        return (Map) (data.get("value"));
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "交易对地址")
    })
    protected Map getStableSwapPairInfo(String pairAddress) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("pairAddress", pairAddress);
        HashMap data = this.getData(STABLE_SWAP_PAIR_INFO, params);
        return (Map) (data.get("value"));
    }

    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "txHash", parameterType = "String", parameterDes = "交易hash")
    })
    protected Map getSwapResultInfo(String txHash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("txHash", txHash);
        HashMap data = this.getData(SWAP_RESULT_INFO, params);
        return (Map) (data.get("value"));
    }

    protected long deadline() {
        return System.currentTimeMillis() / 1000 + 300;
    }

    protected void sendTx(String from, String cmd, Map<String, Object> _params) throws Exception {
        Log.info("hash:{}", this.callSwap(from, cmd, _params));
    }

    protected HashMap getData(String cmd, Map<String, Object> _params) throws Exception {
        return (HashMap) this.callSwap(null, cmd, _params);
    }

    protected Object callSwap(String from, String cmd, Map<String, Object> _params) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, version);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", from);
        params.put("password", password);
        params.putAll(_params);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.SW.abbr, cmd, params);
        if (cmdResp.isSuccess()) {
            Object result = ((HashMap) cmdResp.getResponseData()).get(cmd);
            return result;
        } else {
            System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
            throw new Exception(formatError(cmdResp));
        }
    }

    protected String formatError(Response cmdResp) {
        if (cmdResp == null || cmdResp.isSuccess()) {
            return "success";
        }
        return String.format("code: %s, msg: %s", cmdResp.getResponseErrorCode(), cmdResp.getResponseComment());
    }

    @Test
    public void getBalance() throws Exception {
        getBalanceByAddress("address31-用户地址", address31);
        getBalanceByAddress("address32-接收地址", address32);
        getBalanceByAddress("Swap-pair地址", SwapUtils.getStringPairAddress(chainId, nvt, usdx_bnb));
        getBalanceByAddress("Stable-Swap-pair地址", "TNVTdTSQoaic6dkMxSgwGFE9iJrBcjxnPH33g");
        getBalanceByAddress("接收手续费的系统地址", awardFeeSystemAddress);
        getBalanceByAddress("address51-接收手续费的交易指定地址", address51);
    }

    protected void getBalanceByAddress(String address) throws Exception {
        this.getBalanceByAddress("", address);
    }

    protected void getBalanceByAddress(String title, String address) throws Exception {
        System.out.println();
        System.out.println(String.format("%s address: %s", title, address));
        BigInteger balance2 = LedgerCall.getBalance(chainId, chainId, assetId, address);
        System.out.println(String.format("　主资产NVT %s-%s: %s", chainId, assetId, balance2));

        BigInteger balanceOnEth = LedgerCall.getBalance(chainId, chainId, ethAssetId, address);
        System.out.println(String.format("Ethereum-资产USDX %s-%s: %s", chainId, ethAssetId, balanceOnEth));

        BigInteger balanceOnBsc = LedgerCall.getBalance(chainId, chainId, bscAssetId, address);
        System.out.println(String.format("BSC-资产USDX %s-%s: %s", chainId, bscAssetId, balanceOnBsc));

        BigInteger balanceOnHt = LedgerCall.getBalance(chainId, chainId, htAssetId, address);
        System.out.println(String.format("HT-资产USDX %s-%s: %s", chainId, htAssetId, balanceOnHt));

        BigInteger balanceOnOkt = LedgerCall.getBalance(chainId, chainId, oktAssetId, address);
        System.out.println(String.format("OKT-资产USDX %s-%s: %s", chainId, oktAssetId, balanceOnOkt));

        BigInteger balanceOnStableLp = LedgerCall.getBalance(chainId, chainId, stableLpAssetId, address);
        System.out.println(String.format("Stable-LP资产 %s-%s: %s", chainId, stableLpAssetId, balanceOnStableLp));

        BigInteger balanceOnSwapLp = LedgerCall.getBalance(chainId, chainId, swapLpAssetId, address);
        System.out.println(String.format("Swap-LP资产 %s-%s: %s", chainId, swapLpAssetId, balanceOnSwapLp));
    }

    @Test
    public void getNonceAndBalance() throws Exception {
        NonceBalance b = LedgerCall.getBalanceNonce(chainId, chainId, assetId, "TNVTdTSPyT1GGPrbahr9qo7S87dMBatx9NHtP");
        System.out.println(b.getAvailable());
        System.out.println(HexUtil.encode(b.getNonce()));
    }

    protected Map<String, Object> getTxCfmClient(String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("txHash", hash);
        Response dpResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getConfirmedTxClient", params);
        Map record = (Map) dpResp.getResponseData();
        Log.debug(JSONUtils.obj2PrettyJson(record));
        return (Map) record.get("tx_getConfirmedTxClient");
    }

    @Test
    public void getTx() throws Exception {
        String txStr = (String) (getTxCfmClient("6280be66a9d0b7bef774dfc8839bf0d4b08023b5234d942a1de04a073d85f9c9").get("tx"));
        System.out.println(txStr);
        Transaction tx = Transaction.getInstance(HexUtil.decode(txStr), Transaction.class);//最后一条
        System.out.println(tx.format());
    }

    @Test
    public void withdrawalNULS() throws Exception {
        int htgChainId = ethChainId;
        String contract = null;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 19万个NULS
        BigInteger value = new BigInteger("19000000000000");
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        //NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, null);
    }

    protected void withdrawalByParams(String from, String to, BigInteger value, BigInteger fee, int heterogeneousChainId, Object assetInfo) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("heterogeneousAddress", to);
        params.put("amount", value);
        params.put("distributionFee", fee);
        params.put("remark", "提现");
        params.put("address", from);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal", params);
        if (cmdResp.isSuccess()) {
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_withdrawal");
            String hash = (String) result.get("value");
            String txHex = (String) result.get("hex");
            Log.info("hash:{}", hash);
            Log.info("txHex:{}", txHex);
        } else {
            System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
        }
    }


    protected String fieldValue(String fieldName) throws Exception {
        return this.getClass().getDeclaredField(fieldName).get(this).toString();
    }


    @Test
    public void ledgerAssetQueryOne() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, 5);

        params.put("assetChainId", 5);
        params.put("assetId", 1);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "lg_get_asset", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void ledgerAssetQueryAll() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "lg_get_all_asset", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void ledgerAssetInChainQuery() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", chainId);
        params.put("assetId", 2);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getAssetRegInfoByAssetId", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void ledgerAssetInChainQueryWhole() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getAssetRegInfo", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    /**
     * 删除账户
     */
    @Test
    public void removeAccountTest() throws Exception {
        removeAccount("TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd", password);
    }

    protected void removeAccount(String address, String password) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_removeAccount", params);
        Log.debug("{}", JSONUtils.obj2json(cmdResp.getResponseData()));
    }

    private void packageZP() {
        agentAddress = packageAddressZP;
        packageAddress = packageAddressZP;
        packageAddressPrivateKey = packageAddressPrivateKeyZP;
    }

    private void packageNE() {
        agentAddress = packageAddressNE;
        packageAddress = packageAddressNE;
        packageAddressPrivateKey = packageAddressPrivateKeyNE;
    }

    private void packageHF() {
        agentAddress = packageAddressHF;
        packageAddress = packageAddressHF;
        packageAddressPrivateKey = packageAddressPrivateKeyHF;
    }

    private void package6() {
        agentAddress = address26;
        packageAddress = packageAddress6;
        packageAddressPrivateKey = packageAddressPrivateKey6;
    }

    private void package7() {
        agentAddress = address27;
        packageAddress = packageAddress7;
        packageAddressPrivateKey = packageAddressPrivateKey7;
    }

    private void package8() {
        agentAddress = address28;
        packageAddress = packageAddress8;
        packageAddressPrivateKey = packageAddressPrivateKey8;
    }

    static class CoinDTO {
        private String address;
        private Integer assetsChainId;
        private Integer assetsId;
        private BigInteger amount;
        private String password;
        private long lockTime;

        public CoinDTO(String address, Integer assetsChainId, Integer assetsId, BigInteger amount, String password, long lockTime) {
            this.address = address;
            this.assetsChainId = assetsChainId;
            this.assetsId = assetsId;
            this.amount = amount;
            this.password = password;
            this.lockTime = lockTime;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public void setAssetsChainId(Integer assetsChainId) {
            this.assetsChainId = assetsChainId;
        }

        public void setAssetsId(Integer assetsId) {
            this.assetsId = assetsId;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setLockTime(long lockTime) {
            this.lockTime = lockTime;
        }

        public String getAddress() {
            return address;
        }

        public Integer getAssetsChainId() {
            return assetsChainId;
        }

        public Integer getAssetsId() {
            return assetsId;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public String getPassword() {
            return password;
        }

        public long getLockTime() {
            return lockTime;
        }
    }
}
