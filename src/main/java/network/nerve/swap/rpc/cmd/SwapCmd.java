package network.nerve.swap.rpc.cmd;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.swap.config.SwapConfig;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.help.StableSwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.service.SwapService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.swap.constant.SwapCmdConstant.*;
import static network.nerve.swap.utils.SwapUtils.wrapperFailed;

/**
 * 异构链信息提供命令
 *
 * @author: Mimi
 * @date: 2020-02-28
 */
@Component
public class SwapCmd extends BaseCmd {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private SwapConfig swapConfig;
    @Autowired
    private SwapService swapService;
    @Autowired
    private StableSwapHelper stableSwapHelper;

    private NulsLogger logger() {
        return SwapContext.logger;
    }

    @CmdAnnotation(cmd = BATCH_BEGIN, version = 1.0, description = "一个批次的开始通知，生成当前批次的信息/batch begin")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "blockType", parameterType = "int", parameterDes = "区块处理模式, 打包区块 - 0, 验证区块 - 1"),
            @Parameter(parameterName = "blockHeight", parameterType = "long", parameterDes = "当前打包的区块高度"),
            @Parameter(parameterName = "preStateRoot", parameterType = "String", parameterDes = "前一个区块的stateRoot"),
            @Parameter(parameterName = "blockTime", parameterType = "long", parameterDes = "当前打包的区块时间")
    })
    @ResponseData(description = "无特定返回值，没有错误即成功")
    public Response batchBegin(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            Integer blockType = (Integer) params.get("blockType");
            ChainManager.chainHandle(chainId, blockType);
            Long blockHeight = Long.parseLong(params.get("blockHeight").toString());
            Long blockTime = Long.parseLong(params.get("blockTime").toString());
            String preStateRoot = (String) params.get("preStateRoot");
            swapService.begin(chainId, blockHeight, blockTime, preStateRoot);
            return success();
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = INVOKE, version = 1.0, description = "批次通知开始后，一笔一笔执行/invoke one by one")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "blockType", parameterType = "int", parameterDes = "区块处理模式, 打包区块 - 0, 验证区块 - 1"),
            @Parameter(parameterName = "blockHeight", parameterType = "long", parameterDes = "当前打包的区块高度"),
            @Parameter(parameterName = "blockTime", parameterType = "long", parameterDes = "当前打包的区块时间"),
            @Parameter(parameterName = "tx", parameterType = "String", parameterDes = "交易序列化的HEX编码字符串")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象，包含两个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "success", valueType = Boolean.class, description = "执行是否成功"),
            @Key(name = "txList", valueType = List.class, valueElement = String.class, description = "新生成的系统交易序列化字符串列表(目前只返回一个交易，成交交易 或者 失败返还交易)")
    }))
    public Response invokeOneByOne(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            Integer blockType = (Integer) params.get("blockType");
            ChainManager.chainHandle(chainId, blockType);
            Long blockHeight = Long.parseLong(params.get("blockHeight").toString());
            Long blockTime = Long.parseLong(params.get("blockTime").toString());
            String txData = (String) params.get("tx");
            Transaction tx = new Transaction();
            tx.parse(RPCUtil.decode(txData), 0);
            Result result = swapService.invokeOneByOne(chainId, blockHeight, blockTime, tx);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            if (result.getData() == null) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("txList", List.of());
                return success(resultData);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = BATCH_END, version = 1.0, description = "通知当前批次结束并返回结果/batch end")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "blockType", parameterType = "int", parameterDes = "区块处理模式, 打包区块 - 0, 验证区块 - 1"),
            @Parameter(parameterName = "blockHeight", parameterType = "long", parameterDes = "当前打包的区块高度")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象，包含两个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "stateRoot", description = "当前stateRoot")
    }))
    public Response batchEnd(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            Integer blockType = (Integer) params.get("blockType");
            ChainManager.chainHandle(chainId, blockType);
            Long blockHeight = Long.parseLong(params.get("blockHeight").toString());

            Result result = swapService.end(chainId, blockHeight);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            Map<String, Object> dataMap = (Map<String, Object>) result.getData();
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("stateRoot", dataMap.get("stateRoot"));
            return success(resultMap);
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = IS_LEGAL_COIN_FOR_ADD_STABLE, version = 1.0, description = "检查在稳定币兑换池中添加的币种是否合法")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "交易对地址"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "币种链ID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "币种资产ID"),
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "执行是否成功")
    }))
    public Response checkLegalCoinForAddStable(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            Integer assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            boolean legalCoin = stableSwapHelper.isLegalCoinForAddStable(chainId, stablePairAddress, assetChainId, assetId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", legalCoin);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = ADD_COIN_FOR_STABLE, version = 1.0, description = "在稳定币兑换池中添加币种")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "交易对地址"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "币种链ID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "币种资产ID"),
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "执行是否成功")
    }))
    public Response addCoinForStable(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            Integer assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            boolean legalCoin = stableSwapHelper.addCoinForStable(chainId, stablePairAddress, assetChainId, assetId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", legalCoin);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

}
