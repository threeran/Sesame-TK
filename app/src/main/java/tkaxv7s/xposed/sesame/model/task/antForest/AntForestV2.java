package tkaxv7s.xposed.sesame.model.task.antForest;

import de.robv.android.xposed.XposedHelpers;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import tkaxv7s.xposed.sesame.data.ConfigV2;
import tkaxv7s.xposed.sesame.data.ModelFields;
import tkaxv7s.xposed.sesame.data.ModelGroup;
import tkaxv7s.xposed.sesame.data.RuntimeInfo;
import tkaxv7s.xposed.sesame.data.modelFieldExt.*;
import tkaxv7s.xposed.sesame.data.task.ModelTask;
import tkaxv7s.xposed.sesame.entity.*;
import tkaxv7s.xposed.sesame.hook.ApplicationHook;
import tkaxv7s.xposed.sesame.hook.Toast;
import tkaxv7s.xposed.sesame.model.base.TaskCommon;
import tkaxv7s.xposed.sesame.model.normal.base.BaseModel;
import tkaxv7s.xposed.sesame.model.task.antFarm.AntFarm.TaskStatus;
import tkaxv7s.xposed.sesame.rpc.intervallimit.FixedOrRangeIntervalLimit;
import tkaxv7s.xposed.sesame.rpc.intervallimit.RpcIntervalLimit;
import tkaxv7s.xposed.sesame.ui.ObjReference;
import tkaxv7s.xposed.sesame.util.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 蚂蚁森林V2
 */
public class AntForestV2 extends ModelTask {

    private static final String TAG = AntForestV2.class.getSimpleName();

    private static final AverageMath offsetTimeMath = new AverageMath(5);

    private static final Set<String> AntForestTaskTypeSet;

    static {
        AntForestTaskTypeSet = new HashSet<>();
        AntForestTaskTypeSet.add("VITALITYQIANDAOPUSH"); //
        AntForestTaskTypeSet.add("ONE_CLICK_WATERING_V1");// 给随机好友一键浇水
        AntForestTaskTypeSet.add("GYG_YUEDU_2");// 去森林图书馆逛15s
        AntForestTaskTypeSet.add("GYG_TBRS");// 逛一逛淘宝人生
        AntForestTaskTypeSet.add("TAOBAO_tab2_2023");// 去淘宝看科普视频
        AntForestTaskTypeSet.add("GYG_diantao");// 逛一逛点淘得红包
        AntForestTaskTypeSet.add("GYG-taote");// 逛一逛淘宝特价版
        AntForestTaskTypeSet.add("NONGCHANG_20230818");// 逛一逛淘宝芭芭农场
        // AntForestTaskTypeSet.add("GYG_haoyangmao_20240103");//逛一逛淘宝薅羊毛
        // AntForestTaskTypeSet.add("YAOYIYAO_0815");//去淘宝摇一摇领奖励
        // AntForestTaskTypeSet.add("GYG-TAOCAICAI");//逛一逛淘宝买菜
    }

    private final AtomicInteger taskCount = new AtomicInteger(0);

    private String selfId;

    private Integer tryCountInt;

    private Integer retryIntervalInt;

    private Integer advanceTimeInt;

    private Integer checkIntervalInt;

    private FixedOrRangeIntervalLimit collectIntervalEntity;

    private FixedOrRangeIntervalLimit doubleCollectIntervalEntity;

    private volatile long doubleEndTime = 0;
    private volatile long stealthEndTime = 0;

    private final AverageMath delayTimeMath = new AverageMath(5);

    private final ObjReference<Long> collectEnergyLockLimit = new ObjReference<>(0L);

    private final Object doubleCardLockObj = new Object();

    private BooleanModelField collectEnergy;
    private BooleanModelField energyRain;
    private IntegerModelField advanceTime;
    private IntegerModelField tryCount;
    private IntegerModelField retryInterval;
    private SelectModelField dontCollectList;
    private BooleanModelField collectWateringBubble;
    private BooleanModelField batchRobEnergy;
    private BooleanModelField balanceNetworkDelay;
    private BooleanModelField closeWhackMole;
    private BooleanModelField collectProp;
    private StringModelField queryInterval;
    private StringModelField collectInterval;
    private StringModelField doubleCollectInterval;
    private BooleanModelField doubleCard;
    private ListModelField.ListJoinCommaToStringModelField doubleCardTime;
    @Getter
    private IntegerModelField doubleCountLimit;
    private BooleanModelField doubleCardConstant;
    private BooleanModelField stealthCard;
    private BooleanModelField stealthCardConstant;
    private BooleanModelField helpFriendCollect;
    private ChoiceModelField helpFriendCollectType;
    private SelectModelField helpFriendCollectList;
    private IntegerModelField returnWater33;
    private IntegerModelField returnWater18;
    private IntegerModelField returnWater10;
    private BooleanModelField receiveForestTaskAward;
    private SelectAndCountModelField waterFriendList;
    private IntegerModelField waterFriendCount;
    private SelectModelField giveEnergyRainList;
    private BooleanModelField exchangeEnergyDoubleClick;
    @Getter
    private IntegerModelField exchangeEnergyDoubleClickCount;
    private BooleanModelField exchangeEnergyDoubleClickLongTime;
    @Getter
    private IntegerModelField exchangeEnergyDoubleClickCountLongTime;
    private BooleanModelField exchangeCollectHistoryAnimal7Days;
    private BooleanModelField exchangeCollectToFriendTimes7Days;
    private BooleanModelField exchangeEnergyShield;
    private BooleanModelField userPatrol;
    private BooleanModelField collectGiftBox;
    private BooleanModelField medicalHealthFeeds;
    private BooleanModelField sendEnergyByAction;
    private BooleanModelField combineAnimalPiece;
    private BooleanModelField consumeAnimalProp;
    private SelectModelField whoYouWantToGiveTo;
    private BooleanModelField ecoLifeTick;
    private BooleanModelField ecoLifeOpen;
    private BooleanModelField photoGuangPan;
    private TextModelField photoGuangPanBefore;
    private TextModelField photoGuangPanAfter;

    private int totalCollected = 0;
    private int totalHelpCollected = 0;

    @Getter
    private Set<String> dontCollectMap = new HashSet<>();

    @Override
    public String getName() {
        return "森林";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(collectEnergy = new BooleanModelField("collectEnergy", "收集能量", false));
        modelFields.addField(batchRobEnergy = new BooleanModelField("batchRobEnergy", "一键收取", false));
        modelFields.addField(queryInterval = new StringModelField("queryInterval", "查询间隔(毫秒或毫秒范围)", "500-1000"));
        modelFields.addField(collectInterval = new StringModelField("collectInterval", "收取间隔(毫秒或毫秒范围)", "1000-1500"));
        modelFields.addField(doubleCollectInterval = new StringModelField("doubleCollectInterval", "双击间隔(毫秒或毫秒范围)", "50-150"));
        modelFields.addField(balanceNetworkDelay = new BooleanModelField("balanceNetworkDelay", "平衡网络延迟", true));
        modelFields.addField(advanceTime = new IntegerModelField("advanceTime", "提前时间(毫秒)", 0, Integer.MIN_VALUE, 500));
        modelFields.addField(tryCount = new IntegerModelField("tryCount", "尝试收取(次数)", 1, 0, 10));
        modelFields.addField(retryInterval = new IntegerModelField("retryInterval", "重试间隔(毫秒)", 1000, 0, 10000));
        modelFields.addField(dontCollectList = new SelectModelField("dontCollectList", "不收取能量列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(doubleCard = new BooleanModelField("doubleCard", "双击卡 | 使用", false));
        modelFields.addField(doubleCountLimit = new IntegerModelField("doubleCountLimit", "双击卡 | 使用次数", 6));
        modelFields.addField(doubleCardTime = new ListModelField.ListJoinCommaToStringModelField("doubleCardTime", "双击卡 | 使用时间(范围)", ListUtil.newArrayList("0700-0730")));
        modelFields.addField(doubleCardConstant = new BooleanModelField("DoubleCardConstant", "双击卡 | 限时双击永动机", false));
        modelFields.addField(stealthCard = new BooleanModelField("stealthCard", "隐身卡 | 使用", false));
        modelFields.addField(stealthCardConstant = new BooleanModelField("stealthCardConstant", "隐身卡 | 限时隐身永动机", false));
        modelFields.addField(returnWater10 = new IntegerModelField("returnWater10", "返水 | 10克需收能量(关闭:0)", 0));
        modelFields.addField(returnWater18 = new IntegerModelField("returnWater18", "返水 | 18克需收能量(关闭:0)", 0));
        modelFields.addField(returnWater33 = new IntegerModelField("returnWater33", "返水 | 33克需收能量(关闭:0)", 0));
        modelFields.addField(waterFriendList = new SelectAndCountModelField("waterFriendList", "浇水 | 好友列表", new LinkedHashMap<>(), AlipayUser::getList));
        modelFields.addField(waterFriendCount = new IntegerModelField("waterFriendCount", "浇水 | 克数(10 18 33 66)", 66));
        modelFields.addField(helpFriendCollect = new BooleanModelField("helpFriendCollect", "复活能量 | 开启", false));
        modelFields.addField(helpFriendCollectType = new ChoiceModelField("helpFriendCollectType", "复活能量 | 动作", HelpFriendCollectType.HELP, HelpFriendCollectType.nickNames));
        modelFields.addField(helpFriendCollectList = new SelectModelField("helpFriendCollectList", "复活能量 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(exchangeEnergyDoubleClick = new BooleanModelField("exchangeEnergyDoubleClick", "活力值 | 兑换限时双击卡", false));
        modelFields.addField(exchangeEnergyDoubleClickCount = new IntegerModelField("exchangeEnergyDoubleClickCount", "活力值 | 兑换限时双击卡数量", 6));
        modelFields.addField(exchangeEnergyDoubleClickLongTime = new BooleanModelField("exchangeEnergyDoubleClickLongTime", "活力值 | 兑换永久双击卡", false));
        modelFields.addField(exchangeEnergyDoubleClickCountLongTime = new IntegerModelField("exchangeEnergyDoubleClickCountLongTime", "活力值 | 兑换永久双击卡数量", 6));
        modelFields.addField(exchangeEnergyShield = new BooleanModelField("exchangeEnergyShield", "活力值 | 兑换能量保护罩", false));
        modelFields.addField(exchangeCollectHistoryAnimal7Days = new BooleanModelField("exchangeCollectHistoryAnimal7Days", "活力值 | 兑换物种历史卡", false));
        modelFields.addField(exchangeCollectToFriendTimes7Days = new BooleanModelField("exchangeCollectToFriendTimes7Days", "活力值 | 兑换物种好友卡", false));
        modelFields.addField(closeWhackMole = new BooleanModelField("closeWhackMole", "自动关闭6秒拼手速", true));
        modelFields.addField(collectProp = new BooleanModelField("collectProp", "收集道具", false));
        modelFields.addField(collectWateringBubble = new BooleanModelField("collectWateringBubble", "收金球", false));
        modelFields.addField(energyRain = new BooleanModelField("energyRain", "能量雨", false));
        modelFields.addField(userPatrol = new BooleanModelField("userPatrol", "保护地巡护", false));
        modelFields.addField(combineAnimalPiece = new BooleanModelField("combineAnimalPiece", "合成动物碎片", false));
        modelFields.addField(consumeAnimalProp = new BooleanModelField("consumeAnimalProp", "派遣动物伙伴", false));
        modelFields.addField(receiveForestTaskAward = new BooleanModelField("receiveForestTaskAward", "森林任务", false));
        modelFields.addField(collectGiftBox = new BooleanModelField("collectGiftBox", "领取礼盒", false));
        modelFields.addField(medicalHealthFeeds = new BooleanModelField("medicalHealthFeeds", "健康医疗", false));
        modelFields.addField(sendEnergyByAction = new BooleanModelField("sendEnergyByAction", "森林集市", false));
        modelFields.addField(giveEnergyRainList = new SelectModelField("giveEnergyRainList", "赠送能量雨列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(whoYouWantToGiveTo = new SelectModelField("whoYouWantToGiveTo", "赠送道具好友列表（所有可送道具）", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(ecoLifeTick = new BooleanModelField("ecoLifeTick", "绿色 | 行动打卡", false));
        modelFields.addField(ecoLifeOpen = new BooleanModelField("ecoLifeOpen", "绿色 | 自动开通", false));
        modelFields.addField(photoGuangPan = new BooleanModelField("photoGuangPan", "绿色 | 光盘行动", false));
        modelFields.addField(photoGuangPanBefore = new TextModelField("photoGuangPanBefore", "绿色 | 光盘前图片ID", ""));
        modelFields.addField(photoGuangPanAfter = new TextModelField("photoGuangPanAfter", "绿色 | 光盘后图片ID", ""));
        modelFields.addField(new EmptyModelField("photoGuangPanClear", "绿色 | 清空图片ID", () -> {
            photoGuangPanBefore.reset();
            photoGuangPanAfter.reset();
        }));
        return modelFields;
    }

    @Override
    public Boolean check() {
        if (RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime) > System.currentTimeMillis()) {
            Log.record("异常等待中，暂不执行检测！");
            return false;
        }
        return true;
    }

    @Override
    public Boolean isSync() {
        return true;
    }

    @Override
    public void boot(ClassLoader classLoader) {
        super.boot(classLoader);
        FixedOrRangeIntervalLimit queryIntervalLimit = new FixedOrRangeIntervalLimit(queryInterval.getValue(), 10, 10000);
        RpcIntervalLimit.addIntervalLimit("alipay.antforest.forest.h5.queryHomePage", queryIntervalLimit);
        RpcIntervalLimit.addIntervalLimit("alipay.antforest.forest.h5.queryFriendHomePage", queryIntervalLimit);
        RpcIntervalLimit.addIntervalLimit("alipay.antmember.forest.h5.collectEnergy", 0);
        RpcIntervalLimit.addIntervalLimit("alipay.antmember.forest.h5.queryEnergyRanking", 100);
        RpcIntervalLimit.addIntervalLimit("alipay.antforest.forest.h5.fillUserRobFlag", 500);
        tryCountInt = tryCount.getValue();
        retryIntervalInt = retryInterval.getValue();
        advanceTimeInt = advanceTime.getValue();
        checkIntervalInt = BaseModel.getCheckInterval().getValue();
        dontCollectMap = dontCollectList.getValue();
        collectIntervalEntity = new FixedOrRangeIntervalLimit(collectInterval.getValue(), 50, 10000);
        doubleCollectIntervalEntity = new FixedOrRangeIntervalLimit(doubleCollectInterval.getValue(), 10, 5000);
        delayTimeMath.clear();
        AntForestRpcCall.init();
    }

    @Override
    public void run() {
        try {
            Log.record("执行开始-蚂蚁森林");
            NotificationUtil.setStatusTextExec();

            taskCount.set(0);
            selfId = UserIdMap.getCurrentUid();

            JSONObject selfHomeObject = collectSelfEnergy();
            try {
                JSONObject friendsObject = new JSONObject(AntForestRpcCall.queryEnergyRanking());
                if ("SUCCESS".equals(friendsObject.getString("resultCode"))) {
                    collectFriendsEnergy(friendsObject);
                    int pos = 20;
                    List<String> idList = new ArrayList<>();
                    JSONArray totalDatas = friendsObject.getJSONArray("totalDatas");
                    while (pos < totalDatas.length()) {
                        JSONObject friend = totalDatas.getJSONObject(pos);
                        idList.add(friend.getString("userId"));
                        pos++;
                        if (pos % 20 == 0) {
                            collectFriendsEnergy(idList);
                            idList.clear();
                        }
                    }
                    if (!idList.isEmpty()) {
                        collectFriendsEnergy(idList);
                    }
                } else {
                    Log.record(friendsObject.getString("resultDesc"));
                }
            } catch (Throwable t) {
                Log.i(TAG, "queryEnergyRanking err:");
                Log.printStackTrace(TAG, t);
            }

            if (!TaskCommon.IS_ENERGY_TIME && selfHomeObject != null) {
                String whackMoleStatus = selfHomeObject.optString("whackMoleStatus");
                if ("CAN_PLAY".equals(whackMoleStatus) || "CAN_INITIATIVE_PLAY".equals(whackMoleStatus) || "NEED_MORE_FRIENDS".equals(whackMoleStatus)) {
                    whackMole();
                }
                boolean hasMore = false;
                do {
                    if (hasMore) {
                        hasMore = false;
                        selfHomeObject = querySelfHome();
                    }
                    if (collectWateringBubble.getValue()) {
                        JSONArray wateringBubbles = selfHomeObject.has("wateringBubbles")
                                ? selfHomeObject.getJSONArray("wateringBubbles")
                                : new JSONArray();
                        if (wateringBubbles.length() > 0) {
                            int collected = 0;
                            for (int i = 0; i < wateringBubbles.length(); i++) {
                                JSONObject wateringBubble = wateringBubbles.getJSONObject(i);
                                String bizType = wateringBubble.getString("bizType");
                                if ("jiaoshui".equals(bizType)) {
                                    String str = AntForestRpcCall.collectEnergy(bizType, selfId,
                                            wateringBubble.getLong("id"));
                                    JSONObject joEnergy = new JSONObject(str);
                                    if ("SUCCESS".equals(joEnergy.getString("resultCode"))) {
                                        JSONArray bubbles = joEnergy.getJSONArray("bubbles");
                                        for (int j = 0; j < bubbles.length(); j++) {
                                            collected = bubbles.getJSONObject(j).getInt("collectedEnergy");
                                        }
                                        if (collected > 0) {
                                            String msg = "收取金球🍯浇水[" + collected + "g]";
                                            Log.forest(msg);
                                            Toast.show(msg);
                                            totalCollected += collected;
                                            Statistics.addData(Statistics.DataType.COLLECTED, collected);
                                        } else {
                                            Log.record("收取[我]的浇水金球失败");
                                        }
                                    } else {
                                        Log.record("收取[我]的浇水金球失败:" + joEnergy.getString("resultDesc"));
                                        Log.i(str);
                                    }
                                } else if ("fuhuo".equals(bizType)) {
                                    String str = AntForestRpcCall.collectRebornEnergy();
                                    JSONObject joEnergy = new JSONObject(str);
                                    if ("SUCCESS".equals(joEnergy.getString("resultCode"))) {
                                        collected = joEnergy.getInt("energy");
                                        String msg = "收取金球🍯复活[" + collected + "g]";
                                        Log.forest(msg);
                                        Toast.show(msg);
                                        totalCollected += collected;
                                        Statistics.addData(Statistics.DataType.COLLECTED, collected);
                                    } else {
                                        Log.record("收取[我]的复活金球失败:" + joEnergy.getString("resultDesc"));
                                        Log.i(str);
                                    }
                                } else if ("baohuhuizeng".equals(bizType)) {
                                    String friendId = wateringBubble.getString("userId");
                                    String str = AntForestRpcCall.collectEnergy(bizType, selfId,
                                            wateringBubble.getLong("id"));
                                    JSONObject joEnergy = new JSONObject(str);
                                    if ("SUCCESS".equals(joEnergy.getString("resultCode"))) {
                                        JSONArray bubbles = joEnergy.getJSONArray("bubbles");
                                        for (int j = 0; j < bubbles.length(); j++) {
                                            collected = bubbles.getJSONObject(j).getInt("collectedEnergy");
                                        }
                                        if (collected > 0) {
                                            String msg = "收取金球🍯[" + UserIdMap.getMaskName(friendId) + "]复活回赠[" + collected + "g]";
                                            Log.forest(msg);
                                            Toast.show(msg);
                                            totalCollected += collected;
                                            Statistics.addData(Statistics.DataType.COLLECTED, collected);
                                        } else {
                                            Log.record("收取[" + UserIdMap.getMaskName(friendId) + "]的复活回赠金球失败");
                                        }
                                    } else {
                                        Log.record("收取[" + UserIdMap.getMaskName(friendId) + "]的复活回赠金球失败:" + joEnergy.getString("resultDesc"));
                                        Log.i(str);
                                    }
                                }
                                Thread.sleep(1000L);
                            }
                            if (wateringBubbles.length() >= 20) {
                                hasMore = true;
                            }
                        }
                    }
                    if (collectProp.getValue()) {
                        JSONArray givenProps = selfHomeObject.has("givenProps")
                                ? selfHomeObject.getJSONArray("givenProps")
                                : new JSONArray();
                        if (givenProps.length() > 0) {
                            for (int i = 0; i < givenProps.length(); i++) {
                                JSONObject jo = givenProps.getJSONObject(i);
                                String giveConfigId = jo.getString("giveConfigId");
                                String giveId = jo.getString("giveId");
                                String propName = jo.getJSONObject("propConfig").getString("propName");
                                jo = new JSONObject(AntForestRpcCall.collectProp(giveConfigId, giveId));
                                if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                    Log.forest("领取道具🎭[" + propName + "]");
                                } else {
                                    Log.record("领取道具失败:" + jo.getString("resultDesc"));
                                    Log.i(jo.toString());
                                }
                                Thread.sleep(1000L);
                            }
                            if (givenProps.length() >= 20) {
                                hasMore = true;
                            }
                        }
                    }
                } while (hasMore);
                JSONArray usingUserProps = selfHomeObject.has("usingUserProps")
                        ? selfHomeObject.getJSONArray("usingUserProps")
                        : new JSONArray();
                boolean canConsumeAnimalProp = true;
                if (usingUserProps.length() > 0) {
                    for (int i = 0; i < usingUserProps.length(); i++) {
                        JSONObject jo = usingUserProps.getJSONObject(i);
                        if (!"animal".equals(jo.getString("type"))) {
                            continue;
                        } else {
                            canConsumeAnimalProp = false;
                        }
                        JSONObject extInfo = new JSONObject(jo.getString("extInfo"));
                        int energy = extInfo.optInt("energy", 0);
                        if (energy > 0 && !extInfo.optBoolean("isCollected")) {
                            String propId = jo.getString("propSeq");
                            String propType = jo.getString("propType");
                            String shortDay = extInfo.getString("shortDay");
                            jo = new JSONObject(AntForestRpcCall.collectAnimalRobEnergy(propId, propType, shortDay));
                            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                Log.forest("动物能量🦩[" + energy + "g]");
                            } else {
                                Log.record("收取动物能量失败:" + jo.getString("resultDesc"));
                                Log.i(jo.toString());
                            }
                            try {
                                Thread.sleep(500);
                            } catch (Exception e) {
                                Log.printStackTrace(e);
                            }
                            break;
                        }
                    }
                }
                if (userPatrol.getValue()) {
                    queryUserPatrol();
                }
                if (combineAnimalPiece.getValue()) {
                    queryAnimalAndPiece();
                }
                if (consumeAnimalProp.getValue()) {
                    if (!canConsumeAnimalProp) {
                        Log.record("已经有动物伙伴在巡护森林");
                    } else {
                        queryAnimalPropList();
                    }
                }
                popupTask();
                if (energyRain.getValue()) {
                    energyRain();
                }
                if (receiveForestTaskAward.getValue()) {
                    receiveTaskAward();
                }
                if (ecoLifeTick.getValue() || photoGuangPan.getValue()) {
                    ecoLife();
                }
                Map<String, Integer> friendMap = waterFriendList.getValue();
                for (Map.Entry<String, Integer> friendEntry : friendMap.entrySet()) {
                    String uid = friendEntry.getKey();
                    if (selfId.equals(uid)) {
                        continue;
                    }
                    Integer waterCount = friendEntry.getValue();
                    if (waterCount == null || waterCount <= 0) {
                        continue;
                    }
                    if (waterCount > 3)
                        waterCount = 3;
                    if (Status.canWaterFriendToday(uid, waterCount)) {
                        try {
                            String s = AntForestRpcCall.queryFriendHomePage(uid);
                            TimeUtil.sleep(100);
                            JSONObject jo = new JSONObject(s);
                            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                String bizNo = jo.getString("bizNo");
                                KVNode<Integer, Boolean> waterCountKVNode = returnFriendWater(uid, bizNo, waterCount, waterFriendCount.getValue());
                                waterCount = waterCountKVNode.getKey();
                                if (waterCount > 0) {
                                    Status.waterFriendToday(uid, waterCount);
                                }
                                if (!waterCountKVNode.getValue()) {
                                    break;
                                }
                            } else {
                                Log.record(jo.getString("resultDesc"));
                                Log.i(s);
                            }
                        } catch (Throwable t) {
                            Log.i(TAG, "waterFriendEnergy err:");
                            Log.printStackTrace(TAG, t);
                        }
                    }
                }
                Set<String> set = whoYouWantToGiveTo.getValue();
                if (!set.isEmpty()) {
                    for (String userId : set) {
                        if (!Objects.equals(selfId, userId)) {
                            giveProp(userId);
                            break;
                        }
                    }
                }
                if (exchangeEnergyDoubleClick.getValue() && Status.canExchangeDoubleCardToday()) {
                    int exchangeCount = exchangeEnergyDoubleClickCount.getValue();
                    exchangeEnergyDoubleClick(exchangeCount);
                }
                if (exchangeEnergyDoubleClickLongTime.getValue() && Status.canExchangeDoubleCardTodayLongTime()) {
                    int exchangeCount = exchangeEnergyDoubleClickCountLongTime.getValue();
                    exchangeEnergyDoubleClickLongTime(exchangeCount);
                }
                // 兑换 能量保护罩
                if (exchangeEnergyShield.getValue() && Status.canExchangeEnergyShield()) {
                    exchangeEnergyShield();
                }
                // 兑换 神奇物种抽历史卡机会
                if (exchangeCollectHistoryAnimal7Days.getValue() && Status.canExchangeCollectHistoryAnimal7Days()) {
                    exchangeCollectHistoryAnimal7Days();
                }
                // 兑换 神奇物种抽好友卡机会
                if (exchangeCollectToFriendTimes7Days.getValue() && Status.canExchangeCollectToFriendTimes7Days()) {
                    exchangeCollectToFriendTimes7Days();
                }
                /* 森林集市 */
                if (sendEnergyByAction.getValue()) {
                    sendEnergyByAction("GREEN_LIFE");
                    sendEnergyByAction("ANTFOREST");
                }

                if (medicalHealthFeeds.getValue()) {
                    medicalHealthFeeds();
                }
            }
        } catch (Throwable t) {
            Log.i(TAG, "AntForestV2.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            try {
                synchronized (AntForestV2.this) {
                    int count = taskCount.get();
                    if (count > 0) {
                        AntForestV2.this.wait(TimeUnit.MINUTES.toMillis(30));
                        count = taskCount.get();
                    }
                    if (count > 0) {
                        Log.record("执行超时-蚂蚁森林");
                    } else if (count == 0) {
                        Log.record("执行结束-蚂蚁森林");
                    } else {
                        Log.record("执行完成-蚂蚁森林");
                    }
                }
            } catch (InterruptedException ie) {
                Log.i(TAG, "执行中断-蚂蚁森林");
            }
            Statistics.save();
            FriendWatch.save();
            NotificationUtil.updateLastExecText("收:" + totalCollected + " 帮:" + totalHelpCollected);
        }
    }

    private void notifyMain() {
        if (taskCount.decrementAndGet() < 1) {
            synchronized (AntForestV2.this) {
                AntForestV2.this.notifyAll();
            }
        }
    }

    private JSONObject querySelfHome() {
        JSONObject userHomeObject = null;
        try {
            long start = System.currentTimeMillis();
            userHomeObject = new JSONObject(AntForestRpcCall.queryHomePage());
            long end = System.currentTimeMillis();
            long serverTime = userHomeObject.getLong("now");
            int offsetTime = offsetTimeMath.nextInteger((int) ((start + end) / 2 - serverTime));
            Log.i("服务器时间：" + serverTime + "，本地与服务器时间差：" + offsetTime);
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return userHomeObject;
    }

    private JSONObject queryFriendHome(String userId) {
        JSONObject userHomeObject = null;
        try {
            long start = System.currentTimeMillis();
            userHomeObject = new JSONObject(AntForestRpcCall.queryFriendHomePage(userId));
            long end = System.currentTimeMillis();
            long serverTime = userHomeObject.getLong("now");
            int offsetTime = offsetTimeMath.nextInteger((int) ((start + end) / 2 - serverTime));
            Log.i("服务器时间：" + serverTime + "，本地与服务器时间差：" + offsetTime);
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return userHomeObject;
    }

    private JSONObject collectSelfEnergy() {
        try {
            JSONObject selfHomeObject = querySelfHome();
            if (selfHomeObject != null) {
                if (closeWhackMole.getValue()) {
                    JSONObject propertiesObject = selfHomeObject.optJSONObject("properties");
                    if (propertiesObject != null) {
                        if (Objects.equals("Y", propertiesObject.optString("whackMole"))) {
                            if (closeWhackMole()) {
                                Log.record("6秒拼手速关闭成功");
                            } else {
                                Log.record("6秒拼手速关闭失败");
                            }
                        }
                    }
                }
                String nextAction = selfHomeObject.optString("nextAction");
                if ("WhackMole".equalsIgnoreCase(nextAction)) {
                    Log.record("检测到6秒拼手速强制弹窗，先执行拼手速");
                    whackMole();
                }
                return collectUserEnergy(UserIdMap.getCurrentUid(), selfHomeObject);
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return null;
    }

    private JSONObject collectFriendEnergy(String userId) {
        try {
            JSONObject userHomeObject = queryFriendHome(userId);
            if (userHomeObject != null) {
                return collectUserEnergy(userId, userHomeObject);
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return null;
    }

    private JSONObject collectUserEnergy(String userId, JSONObject userHomeObject) {
        try {
            if (!"SUCCESS".equals(userHomeObject.getString("resultCode"))) {
                Log.record(userHomeObject.getString("resultDesc"));
                return userHomeObject;
            }
            long serverTime = userHomeObject.getLong("now");
            boolean isSelf = Objects.equals(userId, selfId);
            String userName = UserIdMap.getMaskName(userId);
            Log.record("进入[" + userName + "]的蚂蚁森林");

            boolean isCollectEnergy = collectEnergy.getValue() && !dontCollectMap.contains(userId);

            if (isSelf) {
                updateDoubleTime(userHomeObject);
            } else {
                if (isCollectEnergy) {
                    JSONArray jaProps = userHomeObject.optJSONArray("usingUserProps");
                    if (jaProps != null) {
                        for (int i = 0; i < jaProps.length(); i++) {
                            JSONObject joProps = jaProps.getJSONObject(i);
                            if ("energyShield".equals(joProps.getString("type"))) {
                                if (joProps.getLong("endTime") > serverTime) {
                                    Log.record("[" + userName + "]被能量罩保护着哟");
                                    isCollectEnergy = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (isCollectEnergy) {
                JSONArray jaBubbles = userHomeObject.getJSONArray("bubbles");
                List<Long> bubbleIdList = new ArrayList<>();
                for (int i = 0; i < jaBubbles.length(); i++) {
                    JSONObject bubble = jaBubbles.getJSONObject(i);
                    long bubbleId = bubble.getLong("id");
                    switch (CollectStatus.valueOf(bubble.getString("collectStatus"))) {
                        case AVAILABLE:
                            bubbleIdList.add(bubbleId);
                            break;
                        case WAITING:
                            long produceTime = bubble.getLong("produceTime");
                            if (checkIntervalInt + checkIntervalInt / 2 > produceTime - serverTime) {
                                if (hasChildTask(AntForestV2.getBubbleTimerTid(userId, bubbleId))) {
                                    break;
                                }
                                addChildTask(new BubbleTimerTask(userId, bubbleId, produceTime));
                                Log.record("添加蹲点收取🪂[" + userName + "]在[" + TimeUtil.getCommonDate(produceTime) + "]执行");
                            } else {
                                Log.i("用户[" + UserIdMap.getMaskName(userId) + "]能量成熟时间: " + TimeUtil.getCommonDate(produceTime));
                            }
                            break;
                    }
                }
                if (batchRobEnergy.getValue()) {
                    Iterator<Long> iterator = bubbleIdList.iterator();
                    List<Long> batchBubbleIdList = new ArrayList<>();
                    while (iterator.hasNext()) {
                        batchBubbleIdList.add(iterator.next());
                        if (batchBubbleIdList.size() >= 6) {
                            collectEnergy(new CollectEnergyEntity(userId, userHomeObject, AntForestRpcCall.getCollectBatchEnergyRpcEntity(userId, batchBubbleIdList)));
                            batchBubbleIdList = new ArrayList<>();
                        }
                    }
                    int size = batchBubbleIdList.size();
                    if (size > 0) {
                        if (size == 1) {
                            collectEnergy(new CollectEnergyEntity(userId, userHomeObject, AntForestRpcCall.getCollectEnergyRpcEntity(null, userId, batchBubbleIdList.get(0))));
                        } else {
                            collectEnergy(new CollectEnergyEntity(userId, userHomeObject, AntForestRpcCall.getCollectBatchEnergyRpcEntity(userId, batchBubbleIdList)));
                        }
                    }
                } else {
                    for (Long bubbleId : bubbleIdList) {
                        collectEnergy(new CollectEnergyEntity(userId, userHomeObject, AntForestRpcCall.getCollectEnergyRpcEntity(null, userId, bubbleId)));
                    }
                }
            }
            return userHomeObject;
        } catch (Throwable t) {
            Log.i(TAG, "collectUserEnergy err:");
            Log.printStackTrace(TAG, t);
        }
        return null;
    }

    private void collectFriendsEnergy(List<String> idList) {
        try {
            collectFriendsEnergy(new JSONObject(AntForestRpcCall.fillUserRobFlag(new JSONArray(idList).toString())));
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void collectFriendsEnergy(JSONObject friendsObject) {
        try {
            JSONArray jaFriendRanking = friendsObject.optJSONArray("friendRanking");
            if (jaFriendRanking == null) {
                return;
            }
            for (int i = 0, len = jaFriendRanking.length(); i < len; i++) {
                try {
                    JSONObject friendObject = jaFriendRanking.getJSONObject(i);
                    String userId = friendObject.getString("userId");
                    if (Objects.equals(userId, selfId)) {
                        continue;
                    }
                    JSONObject userHomeObject = null;
                    if (collectEnergy.getValue() && !dontCollectMap.contains(userId)) {
                        boolean collectEnergy = true;
                        if (!friendObject.optBoolean("canCollectEnergy")) {
                            long canCollectLaterTime = friendObject.getLong("canCollectLaterTime");
                            if (canCollectLaterTime <= 0 || (canCollectLaterTime - System.currentTimeMillis() > checkIntervalInt)) {
                                collectEnergy = false;
                            }
                        }
                        if (collectEnergy) {
                            userHomeObject = collectFriendEnergy(userId);
                        }/* else {
                            Log.i("不收取[" + UserIdMap.getNameById(userId) + "], userId=" + userId);
                        }*/
                    }
                    if (helpFriendCollect.getValue() && friendObject.optBoolean("canProtectBubble") && Status.canProtectBubbleToday(selfId)) {
                        boolean isHelpCollect = helpFriendCollectList.getValue().contains(userId);
                        if (helpFriendCollectType.getValue() == HelpFriendCollectType.DONT_HELP) {
                            isHelpCollect = !isHelpCollect;
                        }
                        if (isHelpCollect) {
                            if (userHomeObject == null) {
                                userHomeObject = queryFriendHome(userId);
                            }
                            if (userHomeObject != null) {
                                protectFriendEnergy(userHomeObject);
                            }
                        }
                    }
                    if (collectGiftBox.getValue() && friendObject.getBoolean("canCollectGiftBox")) {
                        if (userHomeObject == null) {
                            userHomeObject = queryFriendHome(userId);
                        }
                        if (userHomeObject != null) {
                            collectGiftBox(userHomeObject);
                        }
                    }
                } catch (Exception t) {
                    Log.i(TAG, "collectFriendEnergy err:");
                    Log.printStackTrace(TAG, t);
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void collectGiftBox(JSONObject userHomeObject) {
        try {
            JSONObject giftBoxInfo = userHomeObject.optJSONObject("giftBoxInfo");
            JSONObject userEnergy = userHomeObject.optJSONObject("userEnergy");
            String userId = userEnergy == null ? UserIdMap.getCurrentUid() : userEnergy.optString("userId");
            if (giftBoxInfo != null) {
                JSONArray giftBoxList = giftBoxInfo.optJSONArray("giftBoxList");
                if (giftBoxList != null && giftBoxList.length() > 0) {
                    for (int ii = 0; ii < giftBoxList.length(); ii++) {
                        try {
                            JSONObject giftBox = giftBoxList.getJSONObject(ii);
                            String giftBoxId = giftBox.getString("giftBoxId");
                            String title = giftBox.getString("title");
                            JSONObject giftBoxResult = new JSONObject(AntForestRpcCall.collectFriendGiftBox(giftBoxId, userId));
                            if (!"SUCCESS".equals(giftBoxResult.getString("resultCode"))) {
                                Log.record(giftBoxResult.getString("resultDesc"));
                                Log.i(giftBoxResult.toString());
                                continue;
                            }
                            int energy = giftBoxResult.optInt("energy", 0);
                            Log.forest("礼盒能量🎁[" + UserIdMap.getMaskName(userId) + "-" + title + "]#" + energy + "g");
                            Statistics.addData(Statistics.DataType.COLLECTED, energy);
                        } catch (Throwable t) {
                            Log.printStackTrace(t);
                            break;
                        } finally {
                            TimeUtil.sleep(500);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void protectFriendEnergy(JSONObject userHomeObject) {
        try {
            JSONArray wateringBubbles = userHomeObject.optJSONArray("wateringBubbles");
            JSONObject userEnergy = userHomeObject.optJSONObject("userEnergy");
            String userId = userEnergy == null ? UserIdMap.getCurrentUid() : userEnergy.optString("userId");
            if (wateringBubbles != null && wateringBubbles.length() > 0) {
                for (int j = 0; j < wateringBubbles.length(); j++) {
                    try {
                        JSONObject wateringBubble = wateringBubbles.getJSONObject(j);
                        if (!"fuhuo".equals(wateringBubble.getString("bizType"))) {
                            continue;
                        }
                        if (wateringBubble.getJSONObject("extInfo").optInt("restTimes", 0) == 0) {
                            Status.protectBubbleToday(selfId);
                        }
                        if (!wateringBubble.getBoolean("canProtect")) {
                            continue;
                        }
                        JSONObject joProtect = new JSONObject(AntForestRpcCall.protectBubble(userId));
                        if (!"SUCCESS".equals(joProtect.getString("resultCode"))) {
                            Log.record(joProtect.getString("resultDesc"));
                            Log.i(joProtect.toString());
                            continue;
                        }
                        int vitalityAmount = joProtect.optInt("vitalityAmount", 0);
                        int fullEnergy = wateringBubble.optInt("fullEnergy", 0);
                        String str = "复活能量🚑[" + UserIdMap.getMaskName(userId) + "-" + fullEnergy + "g]" + (vitalityAmount > 0 ? "#活力值+" + vitalityAmount : "");
                        Log.forest(str);
                        totalHelpCollected += fullEnergy;
                        Statistics.addData(Statistics.DataType.HELPED, fullEnergy);
                        break;
                    } catch (Throwable t) {
                        Log.printStackTrace(t);
                        break;
                    } finally {
                        TimeUtil.sleep(500);
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void collectEnergy(CollectEnergyEntity collectEnergyEntity) {
        collectEnergy(collectEnergyEntity, false);
    }

    private void collectEnergy(CollectEnergyEntity collectEnergyEntity, Boolean joinThread) {
        Runnable runnable = () -> {
            try {
                String userId = collectEnergyEntity.getUserId();
                usePropBeforeCollectEnergy(userId);
                RpcEntity rpcEntity = collectEnergyEntity.getRpcEntity();
                boolean needDouble = collectEnergyEntity.getNeedDouble();
                boolean needRetry = collectEnergyEntity.getNeedRetry();
                int tryCount = collectEnergyEntity.addTryCount();
                int collected = 0;
                long startTime;
                synchronized (collectEnergyLockLimit) {
                    long sleep;
                    if (needDouble) {
                        collectEnergyEntity.unsetNeedDouble();
                        sleep = doubleCollectIntervalEntity.getInterval() - System.currentTimeMillis() + collectEnergyLockLimit.get();
                    } else if (needRetry) {
                        collectEnergyEntity.unsetNeedRetry();
                        sleep = retryIntervalInt - System.currentTimeMillis() + collectEnergyLockLimit.get();
                    } else {
                        sleep = collectIntervalEntity.getInterval() - System.currentTimeMillis() + collectEnergyLockLimit.get();
                    }
                    if (sleep > 0) {
                        TimeUtil.sleep(sleep);
                    }
                    startTime = System.currentTimeMillis();
                    collectEnergyLockLimit.setForce(startTime);
                }
                ApplicationHook.requestObject(rpcEntity, 0, 0);
                long spendTime = System.currentTimeMillis() - startTime;
                if (balanceNetworkDelay.getValue()) {
                    delayTimeMath.nextInteger((int) (spendTime / 3));
                }
                if (rpcEntity.getHasError()) {
                    String errorCode = (String) XposedHelpers.callMethod(rpcEntity.getResponseObject(), "getString", "error");
                    if ("1004".equals(errorCode)) {
                        if (BaseModel.getWaitWhenException().getValue() > 0) {
                            long waitTime = System.currentTimeMillis() + BaseModel.getWaitWhenException().getValue();
                            RuntimeInfo.getInstance().put(RuntimeInfo.RuntimeInfoKey.ForestPauseTime, waitTime);
                            NotificationUtil.updateStatusText("异常");
                            Log.record("触发异常,等待至" + TimeUtil.getCommonDate(waitTime));
                            return;
                        }
                        TimeUtil.sleep(600 + RandomUtil.delay());
                    }
                    if (tryCount < tryCountInt) {
                        collectEnergyEntity.setNeedRetry();
                        collectEnergy(collectEnergyEntity);
                    }
                    return;
                }
                JSONObject jo = new JSONObject(rpcEntity.getResponseString());
                String resultCode = jo.getString("resultCode");
                if (!"SUCCESS".equalsIgnoreCase(resultCode)) {
                    if ("PARAM_ILLEGAL2".equals(resultCode)) {
                        Log.record("[" + UserIdMap.getMaskName(userId) + "]" + "能量已被收取,取消重试 错误:" + jo.getString("resultDesc"));
                        return;
                    }
                    Log.record("[" + UserIdMap.getMaskName(userId) + "]" + jo.getString("resultDesc"));
                    if (tryCount < tryCountInt) {
                        collectEnergyEntity.setNeedRetry();
                        collectEnergy(collectEnergyEntity);
                    }
                    return;
                }
                JSONArray jaBubbles = jo.getJSONArray("bubbles");
                int jaBubbleLength = jaBubbles.length();
                if (jaBubbleLength > 1) {
                    List<Long> newBubbleIdList = new ArrayList<>();
                    for (int i = 0; i < jaBubbleLength; i++) {
                        JSONObject bubble = jaBubbles.getJSONObject(i);
                        if (bubble.getBoolean("canBeRobbedAgain")) {
                            newBubbleIdList.add(bubble.getLong("id"));
                        }
                        collected += bubble.getInt("collectedEnergy");
                    }
                    if (collected > 0) {
                        FriendWatch.friendWatch(userId, collected);
                        String str = "一键收取🪂[" + UserIdMap.getMaskName(userId) + "]#" + collected + "g";
                        if (needDouble) {
                            Log.forest(str + "耗时[" + spendTime + "]ms[双击]");
                            Toast.show(str + "[双击]");
                        } else {
                            Log.forest(str + "耗时[" + spendTime + "]ms");
                            Toast.show(str);
                        }
                        totalCollected += collected;
                        Statistics.addData(Statistics.DataType.COLLECTED, collected);
                    } else {
                        Log.record("一键收取[" + UserIdMap.getMaskName(userId) + "]的能量失败" + " " + "，UserID：" + userId + "，BubbleId：" + newBubbleIdList);
                    }
                    if (!newBubbleIdList.isEmpty()) {
                        collectEnergyEntity.setRpcEntity(AntForestRpcCall.getCollectBatchEnergyRpcEntity(userId, newBubbleIdList));
                        collectEnergyEntity.setNeedDouble();
                        collectEnergyEntity.resetTryCount();
                        collectEnergy(collectEnergyEntity);
                        return;
                    }
                } else if (jaBubbleLength == 1) {
                    JSONObject bubble = jaBubbles.getJSONObject(0);
                    collected += bubble.getInt("collectedEnergy");
                    FriendWatch.friendWatch(userId, collected);
                    if (collected > 0) {
                        String str = "收取能量🪂[" + UserIdMap.getMaskName(userId) + "]#" + collected + "g";
                        if (needDouble) {
                            Log.forest(str + "耗时[" + spendTime + "]ms[双击]");
                            Toast.show(str + "[双击]");
                        } else {
                            Log.forest(str + "耗时[" + spendTime + "]ms");
                            Toast.show(str);
                        }
                        totalCollected += collected;
                        Statistics.addData(Statistics.DataType.COLLECTED, collected);
                    } else {
                        Log.record("收取[" + UserIdMap.getMaskName(userId) + "]的能量失败");
                        Log.i("，UserID：" + userId + "，BubbleId：" + bubble.getLong("id"));
                    }
                    if (bubble.getBoolean("canBeRobbedAgain")) {
                        collectEnergyEntity.setNeedDouble();
                        collectEnergyEntity.resetTryCount();
                        collectEnergy(collectEnergyEntity);
                        return;
                    }
                    JSONObject userHome = collectEnergyEntity.getUserHome();
                    if (userHome == null) {
                        return;
                    }
                    String bizNo = userHome.optString("bizNo");
                    if (bizNo.isEmpty()) {
                        return;
                    }
                    int returnCount = 0;
                    if (returnWater33.getValue() > 0 && collected >= returnWater33.getValue()) {
                        returnCount = 33;
                    } else if (returnWater18.getValue() > 0 && collected >= returnWater18.getValue()) {
                        returnCount = 18;
                    } else if (returnWater10.getValue() > 0 && collected >= returnWater10.getValue()) {
                        returnCount = 10;
                    }
                    if (returnCount > 0) {
                        returnFriendWater(userId, bizNo, 1, returnCount);
                    }
                }
            } catch (Exception e) {
                Log.i("collectEnergy err:");
                Log.printStackTrace(e);
            } finally {
                Statistics.save();
                NotificationUtil.updateLastExecText("收:" + totalCollected + " 帮:" + totalHelpCollected);
                notifyMain();
            }
        };
        taskCount.incrementAndGet();
        if (joinThread) {
            runnable.run();
        } else {
            addChildTask(new ChildModelTask("CE|" + collectEnergyEntity.getUserId() + "|" + runnable.hashCode(), "CE", runnable));
        }
    }

    private void updateDoubleTime() throws JSONException {
        String s = AntForestRpcCall.queryHomePage();
        TimeUtil.sleep(100);
        JSONObject joHomePage = new JSONObject(s);
        updateDoubleTime(joHomePage);
    }

    private void updateDoubleTime(JSONObject joHomePage) {
        try {
            JSONArray usingUserPropsNew = joHomePage.getJSONArray("loginUserUsingPropNew");
            if (usingUserPropsNew.length() == 0) {
                usingUserPropsNew = joHomePage.getJSONArray("usingUserPropsNew");
            }
            for (int i = 0; i < usingUserPropsNew.length(); i++) {
                JSONObject userUsingProp = usingUserPropsNew.getJSONObject(i);
                String propGroup = userUsingProp.getString("propGroup");
                if ("doubleClick".equals(propGroup)) {
                    doubleEndTime = userUsingProp.getLong("endTime");
                    // Log.forest("双倍卡剩余时间⏰" + (doubleEndTime - System.currentTimeMillis()) / 1000);
                } else if ("robExpandCard".equals(propGroup)) {
                    String extInfo = userUsingProp.optString("extInfo");
                    if (!extInfo.isEmpty()) {
                        JSONObject extInfoObj = new JSONObject(extInfo);
                        double leftEnergy = Double.parseDouble(extInfoObj.optString("leftEnergy", "0"));
                        if (leftEnergy > 330 || ("true".equals(extInfoObj.optString("overLimitToday", "false")) && leftEnergy >= 1)) {
                            String propId = userUsingProp.getString("propId");
                            String propType = userUsingProp.getString("propType");
                            JSONObject jo = new JSONObject(AntForestRpcCall.collectRobExpandEnergy(propId, propType));
                            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                int collectEnergy = jo.optInt("collectEnergy");
                                Log.forest("额外能量🎄收取[" + collectEnergy + "g]");
                            }
                        }
                    }
                } else if ("stealthCard".equals(propGroup)) {
                    stealthEndTime = userUsingProp.getLong("endTime");
                }
            }
        } catch (Throwable th) {
            Log.i(TAG, "updateDoubleTime err:");
            Log.printStackTrace(TAG, th);
        }
    }

    /* 健康医疗 16g*6能量 */
    private void medicalHealthFeeds() {
        try {
            String s = AntForestRpcCall.query_forest_energy();
            JSONObject jo = new JSONObject(s);
            int countj = 0;
            if (jo.optBoolean("success")) {
                JSONObject response = jo.getJSONObject("data").getJSONObject("response");
                JSONArray energyGeneratedList = response.optJSONArray("energyGeneratedList");
                if (energyGeneratedList != null && energyGeneratedList.length() > 0) {
                    harvestForestEnergy(energyGeneratedList);
                }
                int remainBubble = response.optInt("remainBubble", 0);
                if (remainBubble > 0) {
                    jo = new JSONObject(AntForestRpcCall.medical_health_feeds_query());
                    TimeUtil.sleep(300);
                    if ("SUCCESS".equals(jo.getString("resultCode"))) {
                        response = jo.getJSONObject("data").getJSONObject("response")
                                .optJSONObject("COMMON_FEEDS_BLOCK_2024041200243259").getJSONObject("data")
                                .getJSONObject("response");
                        JSONArray feeds = response.optJSONArray("feeds");
                        if (feeds != null && feeds.length() > 0) {
                            for (int i = 0; i < feeds.length(); i++) {
                                jo = feeds.optJSONObject(i);
                                if (jo == null) {
                                    continue;
                                }
                                String feedId = jo.optString("feedId", "null");
                                if (!"null".equals(feedId)) {
                                    jo = new JSONObject(AntForestRpcCall.produce_forest_energy(feedId));
                                    TimeUtil.sleep(300);
                                    if (jo.optBoolean("success")) {
                                        response = jo.getJSONObject("data").getJSONObject("response");
                                        int cumulativeEnergy = response.optInt("cumulativeEnergy");
                                        if (cumulativeEnergy > 0) {
                                            Log.forest("健康医疗🚑[完成一次]");
                                            countj++;
                                        }
                                        energyGeneratedList = response.optJSONArray("energyGeneratedList");
                                        if (energyGeneratedList != null && energyGeneratedList.length() > 0) {
                                            harvestForestEnergy(energyGeneratedList);
                                        }
                                    }
                                }
                                if (countj >= remainBubble) {
                                    break;
                                }
                            }
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.i(TAG, "medicalHealthFeeds err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void harvestForestEnergy(JSONArray energyGeneratedList) {
        try {
            for (int i = 0; i < energyGeneratedList.length(); i++) {
                JSONObject jo = energyGeneratedList.getJSONObject(i);
                int energy = jo.optInt("energy");
                String id = jo.getString("id");
                jo = new JSONObject(AntForestRpcCall.harvest_forest_energy(energy, id));
                TimeUtil.sleep(300);
                if (jo.optBoolean("success")) {
                    Log.forest("健康医疗🚑[收取能量]#" + energy + "g");
                }
            }
        } catch (Throwable t) {
            Log.i(TAG, "harvestForestEnergy err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /* 6秒拼手速 打地鼠 */
    private void whackMole() {
        try {
            long start = System.currentTimeMillis();
            JSONObject jo = new JSONObject(AntForestRpcCall.startWhackMole());
            if (jo.optBoolean("success")) {
                JSONArray moleInfo = jo.optJSONArray("moleInfo");
                if (moleInfo != null) {
                    List<String> whackMoleIdList = new ArrayList<>();
                    for (int i = 0; i < moleInfo.length(); i++) {
                        JSONObject mole = moleInfo.getJSONObject(i);
                        long moleId = mole.getLong("id");
                        whackMoleIdList.add(String.valueOf(moleId));
                    }
                    if (!whackMoleIdList.isEmpty()) {
                        String token = jo.getString("token");
                        long end = System.currentTimeMillis();
                        TimeUtil.sleep(6000 - end + start);
                        jo = new JSONObject(AntForestRpcCall.settlementWhackMole(token, whackMoleIdList));
                        if ("SUCCESS".equals(jo.getString("resultCode"))) {
                            int totalEnergy = jo.getInt("totalEnergy");
                            Log.forest("森林能量⚡[获得:6秒拼手速能量" + totalEnergy + "g]");
                        }
                    }
                }
            } else {
                Log.i(TAG, jo.getJSONObject("data").toString());
            }
        } catch (Throwable t) {
            Log.i(TAG, "whackMole err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private Boolean closeWhackMole() {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.closeWhackMole());
            if (jo.optBoolean("success")) {
                return true;
            } else {
                Log.i(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return false;
    }

    /* 森林集市 */
    private void sendEnergyByAction(String sourceType) {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.consultForSendEnergyByAction(sourceType));
            if (jo.optBoolean("success")) {
                JSONObject data = jo.getJSONObject("data");
                if (data.optBoolean("canSendEnergy", false)) {
                    jo = new JSONObject(AntForestRpcCall.sendEnergyByAction(sourceType));
                    if (jo.optBoolean("success")) {
                        data = jo.getJSONObject("data");
                        if (data.optBoolean("canSendEnergy", false)) {
                            int receivedEnergyAmount = data.getInt("receivedEnergyAmount");
                            Log.forest("集市逛街👀[获得:能量" + receivedEnergyAmount + "g]");
                        }
                    }
                }
            } else {
                Log.i(TAG, jo.getJSONObject("data").getString("resultCode"));
            }
        } catch (Throwable t) {
            Log.i(TAG, "sendEnergyByAction err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void popupTask() {
        try {
            JSONObject resData = new JSONObject(AntForestRpcCall.popupTask());
            if ("SUCCESS".equals(resData.getString("resultCode"))) {
                JSONArray forestSignVOList = resData.optJSONArray("forestSignVOList");
                if (forestSignVOList != null) {
                    for (int i = 0; i < forestSignVOList.length(); i++) {
                        JSONObject forestSignVO = forestSignVOList.getJSONObject(i);
                        String signId = forestSignVO.getString("signId");
                        String currentSignKey = forestSignVO.getString("currentSignKey");
                        JSONArray signRecords = forestSignVO.getJSONArray("signRecords");
                        for (int j = 0; j < signRecords.length(); j++) {
                            JSONObject signRecord = signRecords.getJSONObject(j);
                            String signKey = signRecord.getString("signKey");
                            if (signKey.equals(currentSignKey)) {
                                if (!signRecord.getBoolean("signed")) {
                                    JSONObject resData2 = new JSONObject(
                                            AntForestRpcCall.antiepSign(signId, UserIdMap.getCurrentUid()));
                                    if ("100000000".equals(resData2.getString("code"))) {
                                        Log.forest("过期能量💊[" + signRecord.getInt("awardCount") + "g]");
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            } else {
                Log.record(resData.getString("resultDesc"));
                Log.i(resData.toString());
            }
        } catch (Throwable t) {
            Log.i(TAG, "popupTask err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private KVNode<Integer, Boolean> returnFriendWater(String userId, String bizNo, int count, int waterEnergy) {
        if (bizNo == null || bizNo.isEmpty()) {
            return new KVNode<>(0, true);
        }
        int wateredTimes = 0;
        boolean isContinue = true;
        try {
            String s;
            JSONObject jo;
            int energyId = getEnergyId(waterEnergy);
            label:
            for (int waterCount = 1; waterCount <= count; waterCount++) {
                s = AntForestRpcCall.transferEnergy(userId, bizNo, energyId);
                TimeUtil.sleep(1500);
                jo = new JSONObject(s);
                String resultCode = jo.getString("resultCode");
                switch (resultCode) {
                    case "SUCCESS":
                        String currentEnergy = jo.getJSONObject("treeEnergy").getString("currentEnergy");
                        Log.forest("好友浇水🚿[" + UserIdMap.getMaskName(userId) + "]#" + waterEnergy + "g，剩余能量["
                                + currentEnergy + "g]");
                        wateredTimes++;
                        Statistics.addData(Statistics.DataType.WATERED, waterEnergy);
                        break;
                    case "WATERING_TIMES_LIMIT":
                        Log.record("好友浇水🚿今日给[" + UserIdMap.getMaskName(userId) + "]浇水已达上限");
                        wateredTimes = 3;
                        break label;
                    case "ENERGY_INSUFFICIENT":
                        Log.record("好友浇水🚿" + jo.getString("resultDesc"));
                        isContinue = false;
                        break label;
                    default:
                        Log.record("好友浇水🚿" + jo.getString("resultDesc"));
                        Log.i(jo.toString());
                        break;
                }
            }
        } catch (Throwable t) {
            Log.i(TAG, "returnFriendWater err:");
            Log.printStackTrace(TAG, t);
        }
        return new KVNode<>(wateredTimes, isContinue);
    }

    private int getEnergyId(int waterEnergy) {
        if (waterEnergy <= 0)
            return 0;
        if (waterEnergy >= 66)
            return 42;
        if (waterEnergy >= 33)
            return 41;
        if (waterEnergy >= 18)
            return 40;
        return 39;
    }

    private void exchangeEnergyDoubleClick(int count) {
        try {
            JSONObject jo = findPropShop("CR20230516000362", "CR20230516000363");
            while (Status.canExchangeDoubleCardToday()
                    && exchangePropShop(jo, Status.INSTANCE.getExchangeTimes() + 1)) {
                Status.exchangeDoubleCardToday(true);
                Thread.sleep(1000);
            }
        } catch (Throwable t) {
            Log.i(TAG, "exchangeEnergyDoubleClick err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // 兑换永久双击卡
    private void exchangeEnergyDoubleClickLongTime(int count) {
        int exchangedTimes;
        try {
            String s = AntForestRpcCall.itemList("SC_ASSETS");
            JSONObject jo = new JSONObject(s);
            String skuId = null;
            String spuId = null;
            double price = 0d;
            if (jo.optBoolean("success")) {
                JSONArray itemInfoVOList = jo.optJSONArray("itemInfoVOList");
                if (itemInfoVOList != null && itemInfoVOList.length() > 0) {
                    for (int i = 0; i < itemInfoVOList.length(); i++) {
                        jo = itemInfoVOList.getJSONObject(i);
                        if ("能量双击卡".equals(jo.getString("spuName"))) {
                            JSONArray skuModelList = jo.getJSONArray("skuModelList");
                            for (int j = 0; j < skuModelList.length(); j++) {
                                jo = skuModelList.getJSONObject(j);
                                if ("VITALITY_ENERGY_DOUBLE_CLICK_NO_EXPIRE_2023"
                                        .equals(jo.getString("rightsConfigId"))) {
                                    skuId = jo.getString("skuId");
                                    spuId = jo.getString("spuId");
                                    price = jo.getJSONObject("price").getDouble("amount");
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
                if (skuId != null) {
                    for (int exchangeCount = 1; exchangeCount <= count; exchangeCount++) {
                        if (Status.canExchangeDoubleCardTodayLongTime()) {
                            jo = new JSONObject(AntForestRpcCall.queryVitalityStoreIndex());
                            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                int totalVitalityAmount = jo.getJSONObject("userVitalityInfoVO")
                                        .getInt("totalVitalityAmount");
                                if (totalVitalityAmount > price) {
                                    jo = new JSONObject(AntForestRpcCall.exchangeBenefit(spuId, skuId));
                                    Thread.sleep(1000);
                                    if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                        Status.exchangeDoubleCardTodayLongTime(true);
                                        exchangedTimes = Status.INSTANCE.getExchangeTimesLongTime();
                                        Log.forest("活力兑换🎐[永久双击卡]#第" + exchangedTimes + "次");
                                    } else {
                                        Log.record(jo.getString("resultDesc"));
                                        Log.i(jo.toString());
                                        Status.exchangeDoubleCardTodayLongTime(false);
                                        break;
                                    }
                                } else {
                                    Log.record("活力值不足，停止兑换！");
                                    break;
                                }
                            }
                        } else {
                            Log.record("兑换次数已到上限！");
                            break;
                        }
                    }
                }
            } else {
                Log.record(jo.getString("desc"));
                Log.i(s);
            }
        } catch (Throwable t) {
            Log.i(TAG, "exchangeEnergyDoubleClickLongTime err:");
            Log.printStackTrace(TAG, t);
        }
    }
    // 兑换 能量保护罩
    private void exchangeEnergyShield() {
        if (exchangePropShop(findPropShop("CR20230517000497", "CR20230516000371"), 1)) {
            Status.exchangeEnergyShield();
        }
    }

    // 兑换 神奇物种抽历史卡机会
    private void exchangeCollectHistoryAnimal7Days() {
        if (exchangePropShop(findPropShop("SP20230518000022", "SK20230518000062"), 1)) {
            Status.exchangeCollectHistoryAnimal7Days();
        }
    }

    // 兑换 神奇物种抽好友卡机会
    private void exchangeCollectToFriendTimes7Days() {
        if (exchangePropShop(findPropShop("SP20230518000021", "SK20230518000061"), 1)) {
            Status.exchangeCollectToFriendTimes7Days();
        }
    }

    private void receiveTaskAward() {
        try {
            do {
                boolean doubleCheck = false;
                String s = AntForestRpcCall.queryTaskList();
                JSONObject jo = new JSONObject(s);
                if ("SUCCESS".equals(jo.getString("resultCode"))) {
                    JSONArray forestSignVOList = jo.getJSONArray("forestSignVOList");
                    JSONObject forestSignVO = forestSignVOList.getJSONObject(0);
                    String currentSignKey = forestSignVO.getString("currentSignKey");
                    JSONArray signRecords = forestSignVO.getJSONArray("signRecords");
                    for (int i = 0; i < signRecords.length(); i++) {
                        JSONObject signRecord = signRecords.getJSONObject(i);
                        String signKey = signRecord.getString("signKey");
                        if (signKey.equals(currentSignKey)) {
                            if (!signRecord.getBoolean("signed")) {
                                JSONObject joSign = new JSONObject(AntForestRpcCall.vitalitySign());
                                TimeUtil.sleep(300);
                                if ("SUCCESS".equals(joSign.getString("resultCode")))
                                    Log.forest("森林签到📆");
                            }
                            break;
                        }
                    }
                    JSONArray forestTasksNew = jo.optJSONArray("forestTasksNew");
                    if (forestTasksNew == null)
                        return;
                    for (int i = 0; i < forestTasksNew.length(); i++) {
                        JSONObject forestTask = forestTasksNew.getJSONObject(i);
                        JSONArray taskInfoList = forestTask.getJSONArray("taskInfoList");
                        for (int j = 0; j < taskInfoList.length(); j++) {
                            JSONObject taskInfo = taskInfoList.getJSONObject(j);
                            JSONObject taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo");
                            JSONObject bizInfo = new JSONObject(taskBaseInfo.getString("bizInfo"));
                            String taskType = taskBaseInfo.getString("taskType");
                            String taskTitle = bizInfo.optString("taskTitle", taskType);
                            String awardCount = bizInfo.optString("awardCount", "1");
                            String sceneCode = taskBaseInfo.getString("sceneCode");
                            String taskStatus = taskBaseInfo.getString("taskStatus");
                            if (TaskStatus.FINISHED.name().equals(taskStatus)) {
                                JSONObject joAward = new JSONObject(AntForestRpcCall.receiveTaskAward(sceneCode, taskType));
                                TimeUtil.sleep(500);
                                if (joAward.optBoolean("success")) {
                                    Log.forest("任务奖励🎖️[" + taskTitle + "]#" + awardCount + "个");
                                    doubleCheck = true;
                                } else {
                                    Log.record("领取失败，" + s);
                                    Log.i(joAward.toString());
                                }
                            } else if (TaskStatus.TODO.name().equals(taskStatus)) {
                                if (bizInfo.optBoolean("autoCompleteTask", false)
                                        || AntForestTaskTypeSet.contains(taskType) || taskType.endsWith("_JIASUQI")
                                        || taskType.endsWith("_BAOHUDI") || taskType.startsWith("GYG")) {
                                    JSONObject joFinishTask = new JSONObject(
                                            AntForestRpcCall.finishTask(sceneCode, taskType));
                                    TimeUtil.sleep(500);
                                    if (joFinishTask.optBoolean("success")) {
                                        Log.forest("森林任务🧾️[" + taskTitle + "]");
                                        doubleCheck = true;
                                    } else {
                                        Log.record("完成任务失败，" + taskTitle);
                                    }
                                } else if ("DAKA_GROUP".equals(taskType)) {
                                    JSONArray childTaskTypeList = taskInfo.optJSONArray("childTaskTypeList");
                                    if (childTaskTypeList != null && childTaskTypeList.length() > 0) {
                                        doChildTask(childTaskTypeList, taskTitle);
                                    }
                                } else if ("TEST_LEAF_TASK".equals(taskType)) {
                                    JSONArray childTaskTypeList = taskInfo.optJSONArray("childTaskTypeList");
                                    if (childTaskTypeList != null && childTaskTypeList.length() > 0) {
                                        doChildTask(childTaskTypeList, taskTitle);
                                        doubleCheck = true;
                                    }
                                }
                            }
                        }
                    }
                    if (doubleCheck)
                        continue;
                } else {
                    Log.record(jo.getString("resultDesc"));
                    Log.i(s);
                }
                break;
            } while (true);
        } catch (Throwable t) {
            Log.i(TAG, "receiveTaskAward err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void doChildTask(JSONArray childTaskTypeList, String title) {
        try {
            for (int i = 0; i < childTaskTypeList.length(); i++) {
                JSONObject taskInfo = childTaskTypeList.getJSONObject(i);
                JSONObject taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo");
                JSONObject bizInfo = new JSONObject(taskBaseInfo.getString("bizInfo"));
                String taskType = taskBaseInfo.getString("taskType");
                String taskTitle = bizInfo.optString("taskTitle", title);
                String sceneCode = taskBaseInfo.getString("sceneCode");
                String taskStatus = taskBaseInfo.getString("taskStatus");
                if (TaskStatus.TODO.name().equals(taskStatus)) {
                    if (bizInfo.optBoolean("autoCompleteTask")) {
                        JSONObject joFinishTask = new JSONObject(
                                AntForestRpcCall.finishTask(sceneCode, taskType));
                        TimeUtil.sleep(500);
                        if (joFinishTask.optBoolean("success")) {
                            Log.forest("完成任务🧾️[" + taskTitle + "]");
                        } else {
                            Log.record("完成任务" + taskTitle + "失败,");
                            Log.i(joFinishTask.toString());
                        }
                    }
                }
            }
        } catch (Throwable th) {
            Log.i(TAG, "doChildTask err:");
            Log.printStackTrace(TAG, th);
        }
    }

    private void startEnergyRain() {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.startEnergyRain());
            TimeUtil.sleep(500);
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                String token = jo.getString("token");
                JSONArray bubbleEnergyList = jo.getJSONObject("difficultyInfo")
                        .getJSONArray("bubbleEnergyList");
                int sum = 0;
                for (int i = 0; i < bubbleEnergyList.length(); i++) {
                    sum += bubbleEnergyList.getInt(i);
                }
                Thread.sleep(5000L);
                if ("SUCCESS".equals(
                        new JSONObject(AntForestRpcCall.energyRainSettlement(sum, token)).getString("resultCode"))) {
                    Toast.show("获得了[" + sum + "g]能量[能量雨]");
                    Log.forest("收能量雨🌧️[" + sum + "g]");
                }
                TimeUtil.sleep(500);
            }
        } catch (Throwable th) {
            Log.i(TAG, "startEnergyRain err:");
            Log.printStackTrace(TAG, th);
        }
    }

    private void energyRain() {
        try {
            JSONObject joEnergyRainHome = new JSONObject(AntForestRpcCall.queryEnergyRainHome());
            TimeUtil.sleep(500);
            if ("SUCCESS".equals(joEnergyRainHome.getString("resultCode"))) {
                if (joEnergyRainHome.getBoolean("canPlayToday")) {
                    startEnergyRain();
                }
                if (joEnergyRainHome.getBoolean("canGrantStatus")) {
                    Log.record("有送能量雨的机会");
                    JSONObject joEnergyRainCanGrantList = new JSONObject(
                            AntForestRpcCall.queryEnergyRainCanGrantList());
                    TimeUtil.sleep(500);
                    JSONArray grantInfos = joEnergyRainCanGrantList.getJSONArray("grantInfos");
                    Set<String> set = giveEnergyRainList.getValue();
                    String userId;
                    boolean granted = false;
                    for (int j = 0; j < grantInfos.length(); j++) {
                        JSONObject grantInfo = grantInfos.getJSONObject(j);
                        if (grantInfo.getBoolean("canGrantedStatus")) {
                            userId = grantInfo.getString("userId");
                            if (set.contains(userId)) {
                                JSONObject joEnergyRainChance = new JSONObject(
                                        AntForestRpcCall.grantEnergyRainChance(userId));
                                TimeUtil.sleep(500);
                                Log.record("尝试送能量雨给【" + UserIdMap.getMaskName(userId) + "】");
                                granted = true;
                                // 20230724能量雨调整为列表中没有可赠送的好友则不赠送
                                if ("SUCCESS".equals(joEnergyRainChance.getString("resultCode"))) {
                                    Log.forest("送能量雨🌧️[" + UserIdMap.getMaskName(userId) + "]#"
                                            + UserIdMap.getMaskName(UserIdMap.getCurrentUid()));
                                    startEnergyRain();
                                } else {
                                    Log.record("送能量雨失败");
                                    Log.i(joEnergyRainChance.toString());
                                }
                                break;
                            }
                        }
                    }
                    if (!granted) {
                        Log.record("没有可以送的用户");
                    }
                    // if (userId != null) {
                    // JSONObject joEnergyRainChance = new
                    // JSONObject(AntForestRpcCall.grantEnergyRainChance(userId));
                    // if ("SUCCESS".equals(joEnergyRainChance.getString("resultCode"))) {
                    // Log.forest("送能量雨🌧️[[" + FriendIdMap.getNameById(userId) + "]#" +
                    // FriendIdMap.getNameById(FriendIdMap.getCurrentUid()));
                    // startEnergyRain();
                    // }
                    // }
                }
            }
            joEnergyRainHome = new JSONObject(AntForestRpcCall.queryEnergyRainHome());
            TimeUtil.sleep(500);
            if ("SUCCESS".equals(joEnergyRainHome.getString("resultCode"))
                    && joEnergyRainHome.getBoolean("canPlayToday")) {
                startEnergyRain();
            }
        } catch (Throwable th) {
            Log.i(TAG, "energyRain err:");
            Log.printStackTrace(TAG, th);
        }
    }

    private void usePropBeforeCollectEnergy(String userId) {
        try {
            if (Objects.equals(selfId, userId)) {
                return;
            }
            boolean needDouble = doubleCard.getValue() && doubleEndTime < System.currentTimeMillis();
            boolean needStealth = stealthCard.getValue() && stealthEndTime < System.currentTimeMillis();
            if (needDouble || needStealth) {
                synchronized (doubleCardLockObj) {
                    JSONObject bagObject = null;
                    if (needDouble) {
                        bagObject = getBag();
                        useDoubleCard(bagObject);
                    }
                    if (needStealth) {
                        if (bagObject == null) {
                            bagObject = getBag();
                        }
                        useStealthCard(bagObject);
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void useDoubleCard(JSONObject bagObject) {
        try {
            if (hasDoubleCardTime() && Status.canDoubleToday()) {
                // 背包查找 限时能量双击卡
                JSONObject jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_DOUBLE_CLICK");
                // 没有限时能量双击卡 且 开启了限时双击永动机
                if (jo == null && doubleCardConstant.getValue()) {
                    // 商店兑换 限时能量双击卡
                    if (exchangePropShop(findPropShop("CR20230516000362", "CR20230516000363"), Status.INSTANCE.getExchangeTimes() + 1)) {
                        Status.exchangeDoubleCardToday(true);
                        jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_DOUBLE_CLICK");
                    }
                }
                if (jo == null) {
                    // 背包查找 能量双击卡
                    jo = findPropBag(bagObject, "ENERGY_DOUBLE_CLICK");
                }
                // 使用能量双击卡
                if (jo != null && usePropBag(jo)) {
                    doubleEndTime = System.currentTimeMillis() + 1000 * 60 * 5;
                    Status.DoubleToday();
                } else {
                    updateDoubleTime();
                }
            }
        } catch (Throwable th) {
            Log.i(TAG, "useDoubleCard err:");
            Log.printStackTrace(TAG, th);
        }
    }

    private void useStealthCard(JSONObject bagObject) {
        try {
            // 背包查找 限时隐身卡
            JSONObject jo = findPropBag(bagObject, "LIMIT_TIME_STEALTH_CARD");
            // 没有限时隐身卡 且 开启了限时隐身永动机
            if (jo == null && stealthCardConstant.getValue()) {
                // 商店兑换 限时隐身卡
                if (exchangePropShop(findPropShop("SP20230521000082", "SK20230521000206"), 1)) {
                    jo = findPropBag(bagObject, "LIMIT_TIME_STEALTH_CARD");
                }
            }
            if (jo == null) {
                jo = findPropBag(bagObject, "STEALTH_CARD");
            }
            // 使用 隐身卡
            if (jo != null && usePropBag(jo)) {
                stealthEndTime = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
            } else {
                updateDoubleTime();
            }
        } catch (Throwable th) {
            Log.i(TAG, "useStealthCard err:");
            Log.printStackTrace(TAG, th);
        }
    }
    private boolean hasDoubleCardTime() {
        long currentTimeMillis = System.currentTimeMillis();
        return TimeUtil.checkInTimeRange(currentTimeMillis, doubleCardTime.getValue());
    }

    /* 赠送道具 */
    private void giveProp(String targetUserId) {
        try {
            do {
                try {
                    JSONObject jo = new JSONObject(AntForestRpcCall.queryPropList(true));
                    if ("SUCCESS".equals(jo.getString("resultCode"))) {
                        JSONArray forestPropVOList = jo.optJSONArray("forestPropVOList");
                        if (forestPropVOList != null && forestPropVOList.length() > 0) {
                            jo = forestPropVOList.getJSONObject(0);
                            String giveConfigId = jo.getJSONObject("giveConfigVO").getString("giveConfigId");
                            int holdsNum = jo.optInt("holdsNum", 0);
                            String propName = jo.getJSONObject("propConfigVO").getString("propName");
                            String propId = jo.getJSONArray("propIdList").getString(0);
                            jo = new JSONObject(AntForestRpcCall.giveProp(giveConfigId, propId, targetUserId));
                            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                Log.forest("赠送道具🎭[" + UserIdMap.getMaskName(targetUserId) + "]#" + propName);
                            } else {
                                Log.record(jo.getString("resultDesc"));
                                Log.i(jo.toString());
                            }
                            if (holdsNum > 1 || forestPropVOList.length() > 1) {
                                continue;
                            }
                        }
                    } else {
                        Log.record(jo.getString("resultDesc"));
                        Log.i(jo.toString());
                    }
                } finally {
                    TimeUtil.sleep(1500);
                }
                break;
            } while (true);
        } catch (Throwable th) {
            Log.i(TAG, "giveProp err:");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 绿色行动
     */
    private void ecoLife() {
        try {
            JSONObject jsonObject = new JSONObject(EcoLifeRpcCall.queryHomePage());
            if (!jsonObject.optBoolean("success")) {
                Log.i(TAG + ".ecoLife.queryHomePage", jsonObject.optString("resultDesc"));
                return;
            }
            JSONObject data = jsonObject.getJSONObject("data");
            if (!data.getBoolean("openStatus") && !ecoLifeOpen.getValue()) {
                Log.forest("绿色任务☘未开通");
                return;
            } else if (!data.getBoolean("openStatus")) {
                jsonObject = new JSONObject(EcoLifeRpcCall.openEcolife());
                if (!jsonObject.optBoolean("success")) {
                    Log.i(TAG + ".ecoLife.openEcolife", jsonObject.optString("resultDesc"));
                    return;
                }
                if (!String.valueOf(true).equals(JsonUtil.getValueByPath(jsonObject, "data.opResult"))) {
                    return;
                }
                Log.forest("绿色任务🍀报告大人，开通成功(～￣▽￣)～可以愉快的玩耍了");
                jsonObject = new JSONObject(EcoLifeRpcCall.queryHomePage());
                data = jsonObject.getJSONObject("data");
            }
            String dayPoint = data.getString("dayPoint");
            JSONArray actionListVO = data.getJSONArray("actionListVO");
            if (ecoLifeTick.getValue()) {
                ecoLifeTick(actionListVO, dayPoint);
            }
            if (photoGuangPan.getValue()) {
                photoGuangPan(dayPoint);
            }
        } catch (Throwable th) {
            Log.i(TAG, "ecoLife err:");
            Log.printStackTrace(TAG, th);
        }
    }

    /* 绿色行动打卡 */

    private void ecoLifeTick(JSONArray actionListVO, String dayPoint) {
        try {
            String source = "source";
            for (int i = 0; i < actionListVO.length(); i++) {
                JSONObject actionVO = actionListVO.getJSONObject(i);
                JSONArray actionItemList = actionVO.getJSONArray("actionItemList");
                for (int j = 0; j < actionItemList.length(); j++) {
                    JSONObject actionItem = actionItemList.getJSONObject(j);
                    if (!actionItem.has("actionId")) {
                        continue;
                    }
                    if (actionItem.getBoolean("actionStatus")) {
                        continue;
                    }
                    String actionId = actionItem.getString("actionId");
                    String actionName = actionItem.getString("actionName");
                    if ("photoguangpan".equals(actionId)) {
                        continue;
                    }
                    JSONObject jo = new JSONObject(EcoLifeRpcCall.tick(actionId, dayPoint, source));
                    if ("SUCCESS".equals(jo.getString("resultCode"))) {
                        Log.forest("绿色打卡🍀[" + actionName + "]");
                    } else {
                        Log.record(jo.getString("resultDesc"));
                        Log.i(jo.toString());
                    }
                    Thread.sleep(500);
                }
            }
        } catch (Throwable th) {
            Log.i(TAG, "ecoLifeTick err:");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 光盘行动
     */
    private void photoGuangPan(String dayPoint) {
        try {
            String source = "renwuGD";
            //检查今日任务状态
            String str = EcoLifeRpcCall.queryDish(source, dayPoint);
            JSONObject jsonObject = new JSONObject(str);
            if (!jsonObject.optBoolean("success")) {
                Log.i(TAG + ".photoGuangPan.ecolifeQueryDish", jsonObject.optString("resultDesc"));
                return;
            }
            boolean isDone = false;
            String photoGuangPanBeforeStr = photoGuangPanBefore.getValue();
            String photoGuangPanAfterStr = photoGuangPanAfter.getValue();
            if (StringUtil.isEmpty(photoGuangPanBeforeStr) || StringUtil.isEmpty(photoGuangPanAfterStr) || Objects.equals(photoGuangPanBeforeStr, photoGuangPanAfterStr)) {
                JSONObject data = jsonObject.optJSONObject("data");
                if (data != null) {
                    String beforeMealsImageUrl = data.optString("beforeMealsImageUrl");
                    String afterMealsImageUrl = data.optString("afterMealsImageUrl");
                    if (!StringUtil.isEmpty(beforeMealsImageUrl) && !StringUtil.isEmpty(afterMealsImageUrl)) {
                        Pattern pattern = Pattern.compile("img/(.*)/original");
                        Matcher beforeMatcher = pattern.matcher(beforeMealsImageUrl);
                        if (beforeMatcher.find()) {
                            photoGuangPanBeforeStr = beforeMatcher.group(1);
                            photoGuangPanBefore.setValue(photoGuangPanBeforeStr);
                        }
                        Matcher afterMatcher = pattern.matcher(afterMealsImageUrl);
                        if (afterMatcher.find()) {
                            photoGuangPanAfterStr = afterMatcher.group(1);
                            photoGuangPanAfter.setValue(photoGuangPanAfterStr);
                        }
                        ConfigV2.save(UserIdMap.getCurrentUid(), false);
                        isDone = true;
                    }
                }
            } else {
                isDone = true;
            }
            if ("SUCCESS".equals(JsonUtil.getValueByPath(jsonObject, "data.status"))) {
                //Log.forest("光盘行动💿今日已完成");
                return;
            }
            if (!isDone) {
                Log.forest("光盘行动💿请先完成一次光盘打卡");
                return;
            }
            //上传餐前照片
            str = EcoLifeRpcCall.uploadDishImage("BEFORE_MEALS",
                    photoGuangPanBeforeStr, 0.16571736, 0.07448776, 0.7597949, dayPoint);
            jsonObject = new JSONObject(str);
            if (!jsonObject.optBoolean("success")) {
                Log.i(TAG + ".photoGuangPan.uploadDishImage", jsonObject.optString("resultDesc"));
                return;
            }
            //上传餐后照片
            str = EcoLifeRpcCall.uploadDishImage("AFTER_MEALS",
                    photoGuangPanAfterStr, 0.00040030346, 0.99891376, 0.0006858421, dayPoint);
            jsonObject = new JSONObject(str);
            if (!jsonObject.optBoolean("success")) {
                Log.i(TAG + ".photoGuangPan.uploadDishImage", jsonObject.optString("resultDesc"));
                return;
            }
            //提交
            str = EcoLifeRpcCall.tick("photoguangpan", dayPoint, source);
            jsonObject = new JSONObject(str);
            if (!jsonObject.optBoolean("success")) {
                Log.i(TAG + ".photoGuangPan.tick", jsonObject.optString("resultDesc"));
                return;
            }
            Log.forest("光盘行动💿任务完成");
        } catch (Throwable t) {
            Log.i(TAG, "photoGuangPan err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void queryUserPatrol() {
        try {
            th:do {
                JSONObject jo = new JSONObject(AntForestRpcCall.queryUserPatrol());
                TimeUtil.sleep(500);
                if ("SUCCESS".equals(jo.getString("resultCode"))) {
                    JSONObject resData = new JSONObject(AntForestRpcCall.queryMyPatrolRecord());
                    TimeUtil.sleep(500);
                    if (resData.optBoolean("canSwitch")) {
                        JSONArray records = resData.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject record = records.getJSONObject(i);
                            JSONObject userPatrol = record.getJSONObject("userPatrol");
                            if (userPatrol.getInt("unreachedNodeCount") > 0) {
                                if ("silent".equals(userPatrol.getString("mode"))) {
                                    JSONObject patrolConfig = record.getJSONObject("patrolConfig");
                                    String patrolId = patrolConfig.getString("patrolId");
                                    resData = new JSONObject(AntForestRpcCall.switchUserPatrol(patrolId));
                                    TimeUtil.sleep(500);
                                    if ("SUCCESS".equals(resData.getString("resultCode"))) {
                                        Log.forest("巡护⚖️-切换地图至" + patrolId);
                                    }
                                    continue th;
                                }
                                break;
                            }
                        }
                    }

                    JSONObject userPatrol = jo.getJSONObject("userPatrol");
                    int currentNode = userPatrol.getInt("currentNode");
                    String currentStatus = userPatrol.getString("currentStatus");
                    int patrolId = userPatrol.getInt("patrolId");
                    JSONObject chance = userPatrol.getJSONObject("chance");
                    int leftChance = chance.getInt("leftChance");
                    int leftStep = chance.getInt("leftStep");
                    int usedStep = chance.getInt("usedStep");
                    if ("STANDING".equals(currentStatus)) {
                        if (leftChance > 0) {
                            jo = new JSONObject(AntForestRpcCall.patrolGo(currentNode, patrolId));
                            TimeUtil.sleep(500);
                            patrolKeepGoing(jo.toString(), currentNode, patrolId);
                            continue;
                        } else if (leftStep >= 2000 && usedStep < 10000) {
                            jo = new JSONObject(AntForestRpcCall.exchangePatrolChance(leftStep));
                            TimeUtil.sleep(300);
                            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                int addedChance = jo.optInt("addedChance", 0);
                                Log.forest("步数兑换⚖️[巡护次数*" + addedChance + "]");
                                continue;
                            } else {
                                Log.i(TAG, jo.getString("resultDesc"));
                            }
                        }
                    } else if ("GOING".equals(currentStatus)) {
                        patrolKeepGoing(null, currentNode, patrolId);
                    }
                } else {
                    Log.i(TAG, jo.getString("resultDesc"));
                }
                break;
            } while (true);
        } catch (Throwable t) {
            Log.i(TAG, "queryUserPatrol err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void patrolKeepGoing(String s, int nodeIndex, int patrolId) {
        try {
            do {
                if (s == null) {
                    s = AntForestRpcCall.patrolKeepGoing(nodeIndex, patrolId, "image");
                }
                JSONObject jo = new JSONObject(s);
                if ("SUCCESS".equals(jo.getString("resultCode"))) {
                    JSONArray jaEvents = jo.optJSONArray("events");
                    if (jaEvents == null || jaEvents.length() == 0) {
                        return;
                    }
                    JSONObject userPatrol = jo.getJSONObject("userPatrol");
                    int currentNode = userPatrol.getInt("currentNode");
                    JSONObject events = jo.getJSONArray("events").getJSONObject(0);
                    JSONObject rewardInfo = events.optJSONObject("rewardInfo");
                    if (rewardInfo != null) {
                        JSONObject animalProp = rewardInfo.optJSONObject("animalProp");
                        if (animalProp != null) {
                            JSONObject animal = animalProp.optJSONObject("animal");
                            if (animal != null) {
                                Log.forest("巡护森林🏇🏻[" + animal.getString("name") + "碎片]");
                            }
                        }
                    }
                    if (!"GOING".equals(jo.getString("currentStatus"))) {
                        return;
                    }
                    JSONObject materialInfo = events.getJSONObject("materialInfo");
                    String materialType = materialInfo.optString("materialType", "image");
                    s = AntForestRpcCall.patrolKeepGoing(currentNode, patrolId, materialType);
                    TimeUtil.sleep(100);
                    continue;

                } else {
                    Log.i(TAG, jo.getString("resultDesc"));
                }
                break;
            } while (true);
        } catch (Throwable t) {
            Log.i(TAG, "patrolKeepGoing err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // 查询可派遣伙伴
    private void queryAnimalPropList() {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.queryAnimalPropList());
            if (!"SUCCESS".equals(jo.getString("resultCode"))) {
                Log.i(TAG, jo.getString("resultDesc"));
                return;
            }
            JSONArray animalProps = jo.getJSONArray("animalProps");
            JSONObject animalProp = null;
            for (int i = 0; i < animalProps.length(); i++) {
                jo = animalProps.getJSONObject(i);
                if (animalProp == null
                        || jo.getJSONObject("main").getInt("holdsNum") > animalProp.getJSONObject("main")
                        .getInt("holdsNum")) {
                    animalProp = jo;
                }
            }
            consumeAnimalProp(animalProp);
        } catch (Throwable t) {
            Log.i(TAG, "queryAnimalPropList err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // 派遣伙伴
    private void consumeAnimalProp(JSONObject animalProp) {
        if (animalProp == null) {
            return;
        }
        try {
            String propGroup = animalProp.getJSONObject("main").getString("propGroup");
            String propType = animalProp.getJSONObject("main").getString("propType");
            String name = animalProp.getJSONObject("partner").getString("name");
            JSONObject jo = new JSONObject(AntForestRpcCall.consumeProp(propGroup, propType, false));
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                Log.forest("巡护派遣🐆[" + name + "]");
            } else {
                Log.i(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.i(TAG, "consumeAnimalProp err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void queryAnimalAndPiece() {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.queryAnimalAndPiece(0));
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                JSONArray animalProps = jo.getJSONArray("animalProps");
                for (int i = 0; i < animalProps.length(); i++) {
                    boolean canCombineAnimalPiece = true;
                    jo = animalProps.getJSONObject(i);
                    JSONArray pieces = jo.getJSONArray("pieces");
                    int id = jo.getJSONObject("animal").getInt("id");
                    for (int j = 0; j < pieces.length(); j++) {
                        jo = pieces.optJSONObject(j);
                        if (jo == null || jo.optInt("holdsNum", 0) <= 0) {
                            canCombineAnimalPiece = false;
                            break;
                        }
                    }
                    if (canCombineAnimalPiece) {
                        combineAnimalPiece(id);
                    }
                }
            } else {
                Log.i(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.i(TAG, "queryAnimalAndPiece err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // 旧版 派遣动物
    private boolean AnimalConsumeProp(int animalId) {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.queryAnimalAndPiece(animalId));
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                JSONArray animalProps = jo.getJSONArray("animalProps");
                jo = animalProps.getJSONObject(0);
                String name = jo.getJSONObject("animal").getString("name");
                JSONObject main = jo.getJSONObject("main");
                String propGroup = main.getString("propGroup");
                String propType = main.getString("propType");
                String propId = main.getJSONArray("propIdList").getString(0);
                jo = new JSONObject(AntForestRpcCall.AnimalConsumeProp(propGroup, propId, propType));
                if ("SUCCESS".equals(jo.getString("resultCode"))) {
                    Log.forest("巡护派遣🐆[" + name + "]");
                    return true;
                } else {
                    Log.i(TAG, jo.getString("resultDesc"));
                }
            } else {
                Log.i(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.i(TAG, "queryAnimalAndPiece err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    private void combineAnimalPiece(int animalId) {
        try {
            do {
                JSONObject jo = new JSONObject(AntForestRpcCall.queryAnimalAndPiece(animalId));
                if ("SUCCESS".equals(jo.getString("resultCode"))) {
                    JSONArray animalProps = jo.getJSONArray("animalProps");
                    jo = animalProps.getJSONObject(0);
                    JSONObject animal = jo.getJSONObject("animal");
                    int id = animal.getInt("id");
                    String name = animal.getString("name");
                    JSONArray pieces = jo.getJSONArray("pieces");
                    boolean canCombineAnimalPiece = true;
                    JSONArray piecePropIds = new JSONArray();
                    for (int j = 0; j < pieces.length(); j++) {
                        jo = pieces.optJSONObject(j);
                        if (jo == null || jo.optInt("holdsNum", 0) <= 0) {
                            canCombineAnimalPiece = false;
                            break;
                        } else {
                            piecePropIds.put(jo.getJSONArray("propIdList").getString(0));
                        }
                    }
                    if (canCombineAnimalPiece) {
                        jo = new JSONObject(AntForestRpcCall.combineAnimalPiece(id, piecePropIds.toString()));
                        if ("SUCCESS".equals(jo.getString("resultCode"))) {
                            Log.forest("合成动物💡[" + name + "]");
                            animalId = id;
                            TimeUtil.sleep(100);
                            continue;
                        } else {
                            Log.i(TAG, jo.getString("resultDesc"));
                        }
                    }
                } else {
                    Log.i(TAG, jo.getString("resultDesc"));
                }
                break;
            } while (true);
        } catch (Throwable t) {
            Log.i(TAG, "combineAnimalPiece err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private int forFriendCollectEnergy(String targetUserId, long bubbleId) {
        int helped = 0;
        try {
            String s = AntForestRpcCall.forFriendCollectEnergy(targetUserId, bubbleId);
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                JSONArray jaBubbles = jo.getJSONArray("bubbles");
                for (int i = 0; i < jaBubbles.length(); i++) {
                    jo = jaBubbles.getJSONObject(i);
                    helped += jo.getInt("collectedEnergy");
                }
                if (helped > 0) {
                    Log.forest("帮收能量🧺[" + UserIdMap.getMaskName(targetUserId) + "]#" + helped + "g");
                    totalHelpCollected += helped;
                    Statistics.addData(Statistics.DataType.HELPED, helped);
                } else {
                    Log.record("帮[" + UserIdMap.getMaskName(targetUserId) + "]收取失败");
                    Log.i("，UserID：" + targetUserId + "，BubbleId" + bubbleId);
                }
            } else {
                Log.record("[" + UserIdMap.getMaskName(targetUserId) + "]" + jo.getString("resultDesc"));
                Log.i(s);
            }
        } catch (Throwable t) {
            Log.i(TAG, "forFriendCollectEnergy err:");
            Log.printStackTrace(TAG, t);
        }
        return helped;
    }

    private JSONObject getBag() {
        try {
            // 获取背包信息
            JSONObject bagObject = new JSONObject(AntForestRpcCall.queryPropList(false));
            if (!"SUCCESS".equals(bagObject.getString("resultCode"))) {
                Log.record(bagObject.getString("resultDesc"));
                Log.i(bagObject.toString());
                return null;
            }
            return bagObject;
        } catch (Throwable th) {
            Log.i(TAG, "findPropBag err:");
            Log.printStackTrace(TAG, th);
        }
        return null;
    }

    /*
     * 查找背包道具
     * prop
     * propGroup, propType, holdsNum, propIdList[], propConfigVO[propName]
     */
    private JSONObject findPropBag(JSONObject bagObject, String propType) {
        JSONObject prop = null;
        try {
            // 遍历背包查找道具
            JSONArray forestPropVOList = bagObject.getJSONArray("forestPropVOList");
            for (int i = 0; i < forestPropVOList.length(); i++) {
                JSONObject forestPropVO = forestPropVOList.getJSONObject(i);
                if (forestPropVO.getString("propType").equals(propType)) {
                    prop = forestPropVO;
                    break;
                }
            }
        } catch (Throwable th) {
            Log.i(TAG, "findPropBag err:");
            Log.printStackTrace(TAG, th);
        }
        return prop;
    }

    /*
     * 使用背包道具
     * prop
     * propGroup, propType, holdsNum, propIdList[], propConfigVO[propName]
     */
    private boolean usePropBag(JSONObject prop) {
        if (prop == null) {
            Log.record("要使用的道具不存在！");
            return false;
        }
        try {
            // 使用道具
            JSONObject jo = new JSONObject(
                    AntForestRpcCall.consumeProp(
                            prop.getJSONArray("propIdList").getString(0), prop.getString("propType")));
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                Log.forest("使用道具🎭[" + prop.getJSONObject("propConfigVO").getString("propName") + "]");
                return true;
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.i(jo.toString());
                return false;
            }
        } catch (Throwable th) {
            Log.i(TAG, "usePropBag err:");
            Log.printStackTrace(TAG, th);
            return false;
        }
    }

    /*
     * 查找商店道具
     * sku
     * spuId, skuId, skuName, exchangedCount, price[amount]
     */
    private JSONObject findPropShop(String spuId, String skuId) {
        JSONObject sku = null;
        try {
            // 获取商店信息
            JSONObject jo = new JSONObject(AntForestRpcCall.itemList("SC_ASSETS"));
            if (!jo.optBoolean("success")) {
                Log.record(jo.getString("desc"));
                Log.i(jo.toString());
                return sku;
            }
            // 遍历商店查找道具
            JSONArray itemInfoVOList = jo.optJSONArray("itemInfoVOList");
            if (itemInfoVOList == null) {
                return sku;
            }
            int length = itemInfoVOList.length();
            for (int i = 0; i < length; i++) {
                jo = itemInfoVOList.getJSONObject(i);
                if (jo.getString("spuId").equals(spuId)) {
                    JSONArray skuModelList = jo.getJSONArray("skuModelList");
                    for (int j = 0; j < skuModelList.length(); j++) {
                        jo = skuModelList.getJSONObject(j);
                        if (jo.getString("skuId").equals(skuId)) {
                            sku = jo;
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (Throwable th) {
            Log.i(TAG, "findPropShop err:");
            Log.printStackTrace(TAG, th);
        }
        return sku;
    }

    /*
     * 兑换商店道具 活力值
     * sku
     * spuId, skuId, skuName, exchangedCount, price[amount]
     * exchangedCount == 0......
     */
    private boolean exchangePropShop(JSONObject sku, int exchangedCount) {
        if (sku == null) {
            Log.record("要兑换的道具不存在！");
            return false;
        }
        try {
            // 获取活力值信息
            JSONObject jo = new JSONObject(AntForestRpcCall.queryVitalityStoreIndex());
            if (!"SUCCESS".equals(jo.getString("resultCode"))) {
                return false;
            }
            // 活力值小于兑换花费，返回
            if (jo.getJSONObject("userVitalityInfoVO").getInt("totalVitalityAmount")
                    < sku.getJSONObject("price").getDouble("amount")) {
                Log.record("活力值不足，停止兑换[" + sku.getString("skuName") + "]！");
                return false;
            }
            // 活力值兑换道具
            jo = new JSONObject(AntForestRpcCall.exchangeBenefit(sku.getString("spuId"), sku.getString("skuId")));
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                Log.forest("活力兑换🎐[" + sku.getString("skuName") + "]#第" + exchangedCount + "次");
                return true;
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.i(jo.toString());
                return false;
            }
        } catch (Throwable th) {
            Log.i(TAG, "exchangePropShop err:");
            Log.printStackTrace(TAG, th);
            return false;
        }
    }

    /**
     * The enum Collect status.
     */
    public enum CollectStatus {
        /**
         * Available collect status.
         */
        AVAILABLE,
        /**
         * Waiting collect status.
         */
        WAITING,
        /**
         * Insufficient collect status.
         */
        INSUFFICIENT,
        /**
         * Robbed collect status.
         */
        ROBBED
    }

    /**
     * The type Bubble timer task.
     */
    private class BubbleTimerTask extends ChildModelTask {

        /**
         * The User id.
         */
        private final String userId;
        /**
         * The Bubble id.
         */
        private final long bubbleId;
        /**
         * The ProduceTime.
         */
        private final long produceTime;

        /**
         * Instantiates a new Bubble timer task.
         */
        BubbleTimerTask(String ui, long bi, long pt) {
            super(AntForestV2.getBubbleTimerTid(ui, bi), pt - advanceTimeInt);
            userId = ui;
            bubbleId = bi;
            produceTime = pt;
        }

        @Override
        public Runnable setRunnable() {
            return () -> {
                String userName = UserIdMap.getMaskName(userId);
                int averageInteger = offsetTimeMath.getAverageInteger();
                long readyTime = produceTime - advanceTimeInt + averageInteger - delayTimeMath.getAverageInteger() - System.currentTimeMillis() + 70;
                if (readyTime > 0) {
                    try {
                        Thread.sleep(readyTime);
                    } catch (InterruptedException e) {
                        Log.i("终止[" + userName + "]蹲点收取任务, 任务ID[" + getId() + "]");
                        return;
                    }
                }
                Log.record("执行蹲点收取[" + userName + "]" + "时差[" + averageInteger + "]ms" + "提前[" + advanceTimeInt + "]ms");
                collectEnergy(new CollectEnergyEntity(userId, null, AntForestRpcCall.getCollectEnergyRpcEntity(null, userId, bubbleId)), true);
            };
        }
    }

    public static String getBubbleTimerTid(String ui, long bi) {
        return "BT|" + ui + "|" + bi;
    }

    public interface HelpFriendCollectType {

        int HELP = 0;
        int DONT_HELP = 1;

        String[] nickNames = {"选中复活", "选中不复活"};

    }
}