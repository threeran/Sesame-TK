package tkaxv7s.xposed.sesame.model.task.antForest;

import de.robv.android.xposed.XposedHelpers;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import tkaxv7s.xposed.sesame.data.ConfigV2;
import tkaxv7s.xposed.sesame.data.ModelFields;
import tkaxv7s.xposed.sesame.data.RuntimeInfo;
import tkaxv7s.xposed.sesame.data.modelFieldExt.*;
import tkaxv7s.xposed.sesame.data.task.ModelTask;
import tkaxv7s.xposed.sesame.entity.*;
import tkaxv7s.xposed.sesame.hook.ApplicationHook;
import tkaxv7s.xposed.sesame.hook.Toast;
import tkaxv7s.xposed.sesame.model.base.TaskCommon;
import tkaxv7s.xposed.sesame.model.normal.base.BaseModel;
import tkaxv7s.xposed.sesame.model.task.antFarm.AntFarm.TaskStatus;
import tkaxv7s.xposed.sesame.ui.ObjReference;
import tkaxv7s.xposed.sesame.util.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 蚂蚁森林V2
 */
public class AntForestV2 extends ModelTask {
    private static final String TAG = AntForestV2.class.getSimpleName();

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

    private int totalCollected = 0;
    private int totalHelpCollected = 0;

    private final AtomicLong offsetTime = new AtomicLong(0);

    private final AtomicInteger taskCount = new AtomicInteger(0);

    private String selfId;

    private Integer tryCountInt;

    private Integer retryIntervalInt;

    private FixedOrRangeIntervalEntity queryIntervalEntity;

    private FixedOrRangeIntervalEntity collectIntervalEntity;

    private FixedOrRangeIntervalEntity doubleCollectIntervalEntity;

    private volatile long doubleEndTime = 0;

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
    private BooleanModelField collectProp;
    private StringModelField queryInterval;
    private StringModelField collectInterval;
    private StringModelField doubleCollectInterval;
    private BooleanModelField doubleCard;
    private ListModelField.ListJoinCommaToStringModelField doubleCardTime;
    @Getter
    private IntegerModelField doubleCountLimit;
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
    private BooleanModelField userPatrol;
    private BooleanModelField antdodoCollect;
    private BooleanModelField totalCertCount;
    private BooleanModelField collectGiftBox;
    private BooleanModelField medicalHealthFeeds;
    private BooleanModelField sendEnergyByAction;
    private BooleanModelField animalConsumeProp;
    private SelectModelField sendFriendCard;
    private SelectModelField whoYouWantToGiveTo;
    private BooleanModelField ecoLifeTick;
    private BooleanModelField ecoLifeOpen;
    private BooleanModelField photoGuangPan;
    private TextModelField photoGuangPanBefore;
    private TextModelField photoGuangPanAfter;

    @Getter
    private Set<String> dontCollectMap = new HashSet<>();

    @Override
    public String getName() {
        return "森林";
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(collectEnergy = new BooleanModelField("collectEnergy", "收集能量", false));
        modelFields.addField(batchRobEnergy = new BooleanModelField("batchRobEnergy", "一键收取", false));
        modelFields.addField(queryInterval = new StringModelField("queryInterval", "查询间隔(毫秒或毫秒范围)", "500-1000"));
        modelFields.addField(collectInterval = new StringModelField("collectInterval", "收取间隔(毫秒或毫秒范围)", "1000-1500"));
        modelFields.addField(doubleCollectInterval = new StringModelField("doubleCollectInterval", "双击收取间隔(毫秒或毫秒范围)", "50-150"));
        modelFields.addField(balanceNetworkDelay = new BooleanModelField("balanceNetworkDelay", "平衡网络延迟", true));
        modelFields.addField(advanceTime = new IntegerModelField("advanceTime", "提前时间(毫秒)", 0, Integer.MIN_VALUE, 500));
        modelFields.addField(tryCount = new IntegerModelField("tryCount", "尝试收取(次数)", 1, 0, 10));
        modelFields.addField(retryInterval = new IntegerModelField("retryInterval", "重试间隔(毫秒)", 1000, 0, 10000));
        modelFields.addField(dontCollectList = new SelectModelField("dontCollectList", "不收取能量列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(doubleCard = new BooleanModelField("doubleCard", "双击卡 | 使用", false));
        modelFields.addField(doubleCountLimit = new IntegerModelField("doubleCountLimit", "双击卡 | 使用次数", 6));
        modelFields.addField(doubleCardTime = new ListModelField.ListJoinCommaToStringModelField("doubleCardTime", "双击卡 | 使用时间(范围)", ListUtil.newArrayList("0700-0730")));
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
        modelFields.addField(collectProp = new BooleanModelField("collectProp", "收集道具", false));
        modelFields.addField(collectWateringBubble = new BooleanModelField("collectWateringBubble", "收金球", false));
        modelFields.addField(energyRain = new BooleanModelField("energyRain", "能量雨", false));
        modelFields.addField(giveEnergyRainList = new SelectModelField("giveEnergyRainList", "赠送能量雨列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(animalConsumeProp = new BooleanModelField("animalConsumeProp", "派遣动物", false));
        modelFields.addField(userPatrol = new BooleanModelField("userPatrol", "巡护森林", false));
        modelFields.addField(receiveForestTaskAward = new BooleanModelField("receiveForestTaskAward", "森林任务", false));
        modelFields.addField(antdodoCollect = new BooleanModelField("antdodoCollect", "神奇物种开卡", false));
        modelFields.addField(totalCertCount = new BooleanModelField("totalCertCount", "记录证书总数", false));
        modelFields.addField(collectGiftBox = new BooleanModelField("collectGiftBox", "领取礼盒", false));
        modelFields.addField(medicalHealthFeeds = new BooleanModelField("medicalHealthFeeds", "健康医疗", false));
        modelFields.addField(sendEnergyByAction = new BooleanModelField("sendEnergyByAction", "森林集市", false));
        modelFields.addField(sendFriendCard = new SelectModelField("sendFriendCard", "送卡片好友列表(当前图鉴所有卡片)", new LinkedHashSet<>(), AlipayUser::getList));
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
    public void run() {
        try {
            Log.record("执行开始-蚂蚁森林");
            NotificationUtil.setStatusTextExec();

            taskCount.set(0);
            selfId = UserIdMap.getCurrentUid();
            tryCountInt = tryCount.getValue();
            retryIntervalInt = retryInterval.getValue();
            dontCollectMap = dontCollectList.getValue();

            queryIntervalEntity = new FixedOrRangeIntervalEntity(queryInterval.getValue(), 10, 10000);
            collectIntervalEntity = new FixedOrRangeIntervalEntity(collectInterval.getValue(), 50, 10000);
            doubleCollectIntervalEntity = new FixedOrRangeIntervalEntity(doubleCollectInterval.getValue(), 10, 5000);

            if (!balanceNetworkDelay.getValue()) {
                offsetTime.set(0);
            }

            collectSelfEnergy();
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

            if (!TaskCommon.IS_ENERGY_TIME) {
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
                    if (selfId.equals(uid))
                        continue;
                    Integer waterCount = friendEntry.getValue();
                    if (waterCount == null || waterCount <= 0) {
                        continue;
                    }
                    if (waterCount > 3)
                        waterCount = 3;
                    if (Status.canWaterFriendToday(uid, waterCount)) {
                        waterFriendEnergy(uid, waterCount);
                    }
                }
                if (antdodoCollect.getValue()) {
                    antdodoReceiveTaskAward();
                    antdodoPropList();
                    antdodoCollect();
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
        return querySelfHome(queryIntervalEntity.getInterval());
    }

    private JSONObject querySelfHome(Integer interval) {
        JSONObject userHomeObject = null;
        try {
            if (balanceNetworkDelay.getValue()) {
                long start = System.currentTimeMillis();
                userHomeObject = new JSONObject(AntForestRpcCall.queryHomePage());
                long end = System.currentTimeMillis();
                long serverTime = userHomeObject.getLong("now");
                offsetTime.set(Math.max((start + end) / 2 - serverTime, -3000));
                Log.i("服务器时间：" + serverTime + "，本地与服务器时间差：" + offsetTime.get());
            } else {
                userHomeObject = new JSONObject(AntForestRpcCall.queryHomePage());
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        } finally {
            if (interval > 0) {
                TimeUtil.sleep(interval);
            }
        }
        return userHomeObject;
    }

    private JSONObject queryFriendHome(String userId) {
        return queryFriendHome(userId, queryIntervalEntity.getInterval());
    }

    private JSONObject queryFriendHome(String userId, Integer interval) {
        JSONObject userHomeObject = null;
        try {
            if (balanceNetworkDelay.getValue()) {
                long start = System.currentTimeMillis();
                userHomeObject = new JSONObject(AntForestRpcCall.queryFriendHomePage(userId));
                long end = System.currentTimeMillis();
                long serverTime = userHomeObject.getLong("now");
                offsetTime.set(Math.max((start + end) / 2 - serverTime, -3000));
                Log.i("服务器时间：" + serverTime + "，本地与服务器时间差：" + offsetTime.get());
            } else {
                userHomeObject = new JSONObject(AntForestRpcCall.queryFriendHomePage(userId));
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        } finally {
            if (interval > 0) {
                TimeUtil.sleep(interval);
            }
        }
        return userHomeObject;
    }

    private JSONObject collectSelfEnergy() {
        try {
            JSONObject userHomeObject = querySelfHome(0);
            if (userHomeObject != null) {
                return collectUserEnergy(UserIdMap.getCurrentUid(), userHomeObject);
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        } finally {
            Integer interval = queryIntervalEntity.getInterval();
            if (interval > 0) {
                TimeUtil.sleep(interval);
            }
        }
        return null;
    }

    private JSONObject collectFriendEnergy(String userId) {
        try {
            JSONObject userHomeObject = queryFriendHome(userId, 0);
            if (userHomeObject != null) {
                return collectUserEnergy(userId, userHomeObject);
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        } finally {
            Integer interval = queryIntervalEntity.getInterval();
            if (interval > 0) {
                TimeUtil.sleep(interval);
            }
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
                            if (BaseModel.getCheckInterval().getValue() > produceTime - serverTime) {
                                String tid = AntForestV2.getBubbleTimerTid(userId, bubbleId);
                                if (hasChildTask(tid)) {
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

            if (!TaskCommon.IS_ENERGY_TIME) {
                if (isSelf) {
                    String whackMoleStatus = userHomeObject.optString("whackMoleStatus");
                    if ("CAN_PLAY".equals(whackMoleStatus) || "CAN_INITIATIVE_PLAY".equals(whackMoleStatus) || "NEED_MORE_FRIENDS".equals(whackMoleStatus)) {
                        whackMole();
                    }
                    if (totalCertCount.getValue()) {
                        JSONObject userBaseInfo = userHomeObject.getJSONObject("userBaseInfo");
                        int totalCertCount = userBaseInfo.optInt("totalCertCount", 0);
                        FileUtil.setCertCount(selfId, Log.getFormatDate(), totalCertCount);
                    }
                    boolean hasMore = false;
                    do {
                        if (hasMore) {
                            hasMore = false;
                            userHomeObject = querySelfHome();
                        }
                        if (collectWateringBubble.getValue()) {
                            JSONArray wateringBubbles = userHomeObject.has("wateringBubbles")
                                    ? userHomeObject.getJSONArray("wateringBubbles")
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
                            JSONArray givenProps = userHomeObject.has("givenProps")
                                    ? userHomeObject.getJSONArray("givenProps")
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
                    JSONArray usingUserProps = userHomeObject.has("usingUserProps")
                            ? userHomeObject.getJSONArray("usingUserProps")
                            : new JSONArray();
                    boolean canConsumeProp = true;
                    if (usingUserProps.length() > 0) {
                        for (int i = 0; i < usingUserProps.length(); i++) {
                            JSONObject jo = usingUserProps.getJSONObject(i);
                            if (!"animal".equals(jo.getString("type"))) {
                                continue;
                            } else {
                                canConsumeProp = false;
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
                        if (!canConsumeProp) {
                            Log.record("已经有动物在巡护");
                        }
                        queryUserPatrol();
                        queryAnimalAndPiece(canConsumeProp);
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
        } finally {
            TimeUtil.sleep(500);
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
                            if (canCollectLaterTime <= 0 || (canCollectLaterTime - System.currentTimeMillis() > BaseModel.getCheckInterval().getValue())) {
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
                if (doubleEndTime < System.currentTimeMillis() && doubleCard.getValue() && !Objects.equals(selfId, userId)) {
                    useDoubleCard();
                }
                RpcEntity rpcEntity = collectEnergyEntity.getRpcEntity();
                boolean needDouble = collectEnergyEntity.getNeedDouble();
                boolean needRetry = collectEnergyEntity.getNeedRetry();
                int tryCount = collectEnergyEntity.addTryCount();
                int collected = 0;
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
                    long start = System.currentTimeMillis();
                    ApplicationHook.requestObject(rpcEntity, 0, 0);
                    long end = System.currentTimeMillis();
                    collectEnergyLockLimit.setForce((start + end) / 2);
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
                        try {
                            Thread.sleep(600 + RandomUtil.delay());
                        } catch (InterruptedException e) {
                            Log.printStackTrace(e);
                        }
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
                        String str = "一键收取🪂[" + UserIdMap.getMaskName(userId) + "]#" + collected + "g" + (needDouble ? "[双击卡]" : "");
                        Log.forest(str);
                        Toast.show(str);
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
                        String str = "收取能量🪂[" + UserIdMap.getMaskName(userId) + "]#" + collected + "g" + (needDouble ? "[双击卡]" : "");
                        Log.forest(str);
                        Toast.show(str);
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
        if (joinThread) {
            runnable.run();
        } else {
            addChildTask(new ChildModelTask("CE|" + collectEnergyEntity.getUserId() + "|" + runnable.hashCode(), "CE", runnable));
            taskCount.incrementAndGet();
        }
    }

    private void updateDoubleTime() throws JSONException {
        try {
            String s = AntForestRpcCall.queryHomePage();
            JSONObject joHomePage = new JSONObject(s);
            updateDoubleTime(joHomePage);
        } finally {
            TimeUtil.sleep(100);
        }
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
                        if (leftEnergy > 300 || ("true".equals(extInfoObj.optString("overLimitToday", "false")) && leftEnergy >= 1)) {
                            String propId = userUsingProp.getString("propId");
                            String propType = userUsingProp.getString("propType");
                            try {
                                JSONObject jo = new JSONObject(AntForestRpcCall.collectRobExpandEnergy(propId, propType));
                                if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                    int collectEnergy = jo.optInt("collectEnergy");
                                    Log.forest("额外能量🎄收取[" + collectEnergy + "g]");
                                }
                            } finally {
                                TimeUtil.sleep(1000);
                            }
                        }
                    }
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
            TimeUtil.sleep(1000);
            JSONObject jo = new JSONObject(s);
            int countj = 0;
            if (jo.getBoolean("success")) {
                JSONObject response = jo.getJSONObject("data").getJSONObject("response");
                JSONArray energyGeneratedList = response.optJSONArray("energyGeneratedList");
                if (energyGeneratedList != null && energyGeneratedList.length() > 0) {
                    harvestForestEnergy(energyGeneratedList);
                }
                int remainBubble = response.optInt("remainBubble", 0);
                if (remainBubble > 0) {
                    jo = new JSONObject(AntForestRpcCall.medical_health_feeds_query());
                    TimeUtil.sleep(1000);
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
                                    TimeUtil.sleep(1000);
                                    if (jo.getBoolean("success")) {
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
                try {
                    jo = new JSONObject(AntForestRpcCall.harvest_forest_energy(energy, id));
                    if (jo.getBoolean("success")) {
                        Log.forest("健康医疗🚑[收取能量]#" + energy + "g");
                    }
                } finally {
                    TimeUtil.sleep(1000);
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
            if (jo.getBoolean("success")) {
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

    /* 森林集市 */
    private void sendEnergyByAction(String sourceType) {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.consultForSendEnergyByAction(sourceType));
            if (jo.getBoolean("success")) {
                JSONObject data = jo.getJSONObject("data");
                if (data.optBoolean("canSendEnergy", false)) {
                    jo = new JSONObject(AntForestRpcCall.sendEnergyByAction(sourceType));
                    if (jo.getBoolean("success")) {
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

    private void waterFriendEnergy(String userId, int count) {
        try {
            String s = AntForestRpcCall.queryFriendHomePage(userId);
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                String bizNo = jo.getString("bizNo");
                count = returnFriendWater(userId, bizNo, count, waterFriendCount.getValue());
                if (count > 0)
                    Status.waterFriendToday(userId, count);
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.i(s);
            }
        } catch (Throwable t) {
            Log.i(TAG, "waterFriendEnergy err:");
            Log.printStackTrace(TAG, t);
        } finally {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
    }

    private int returnFriendWater(String userId, String bizNo, int count, int waterEnergy) {
        if (bizNo == null || bizNo.isEmpty()) {
            return 0;
        }
        int wateredTimes = 0;
        try {
            String s;
            JSONObject jo;
            int energyId = getEnergyId(waterEnergy);
            for (int waterCount = 1; waterCount <= count; waterCount++) {
                s = AntForestRpcCall.transferEnergy(userId, bizNo, energyId);
                jo = new JSONObject(s);
                if ("SUCCESS".equals(jo.getString("resultCode"))) {
                    String currentEnergy = jo.getJSONObject("treeEnergy").getString("currentEnergy");
                    Log.forest("好友浇水🚿[" + UserIdMap.getMaskName(userId) + "]#" + waterEnergy + "g，剩余能量["
                            + currentEnergy + "g]");
                    wateredTimes++;
                    Statistics.addData(Statistics.DataType.WATERED, waterEnergy);
                } else if ("WATERING_TIMES_LIMIT".equals(jo.getString("resultCode"))) {
                    Log.record("今日给[" + UserIdMap.getMaskName(userId) + "]浇水已达上限");
                    wateredTimes = 3;
                    break;
                } else {
                    Log.record(jo.getString("resultDesc"));
                    Log.i(jo.toString());
                }
                Thread.sleep(1500);
            }
        } catch (Throwable t) {
            Log.i(TAG, "returnFriendWater err:");
            Log.printStackTrace(TAG, t);
        }
        return wateredTimes;
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
        int exchangedTimes;
        try {
            String s = AntForestRpcCall.itemList("SC_ASSETS");
            JSONObject jo = new JSONObject(s);
            String skuId = null;
            String spuId = null;
            double price = 0d;
            if (jo.getBoolean("success")) {
                JSONArray itemInfoVOList = jo.optJSONArray("itemInfoVOList");
                if (itemInfoVOList != null && itemInfoVOList.length() > 0) {
                    for (int i = 0; i < itemInfoVOList.length(); i++) {
                        jo = itemInfoVOList.getJSONObject(i);
                        if ("能量双击卡".equals(jo.getString("spuName"))) {
                            JSONArray skuModelList = jo.getJSONArray("skuModelList");
                            for (int j = 0; j < skuModelList.length(); j++) {
                                jo = skuModelList.getJSONObject(j);
                                if ("LIMIT_TIME_ENERGY_DOUBLE_CLICK_3DAYS_2023"
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
                        if (Status.canExchangeDoubleCardToday()) {
                            jo = new JSONObject(AntForestRpcCall.queryVitalityStoreIndex());
                            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                int totalVitalityAmount = jo.getJSONObject("userVitalityInfoVO")
                                        .getInt("totalVitalityAmount");
                                if (totalVitalityAmount > price) {
                                    jo = new JSONObject(AntForestRpcCall.exchangeBenefit(spuId, skuId));
                                    if ("SUCCESS".equals(jo.getString("resultCode"))) {
                                        Status.exchangeDoubleCardToday(true);
                                        exchangedTimes = Status.INSTANCE.getExchangeTimes();
                                        Log.forest("活力兑换🎐[限时双击卡]#第" + exchangedTimes + "次");
                                    } else {
                                        Log.record(jo.getString("resultDesc"));
                                        Log.i(jo.toString());
                                        Status.exchangeDoubleCardToday(false);
                                        break;
                                    }
                                    Thread.sleep(1000);
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
            if (jo.getBoolean("success")) {
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
                                    Thread.sleep(1000);
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

    private void receiveTaskAward() {
        try {
            do {
                try {
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
                                    if (joAward.getBoolean("success")) {
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
                                        if (joFinishTask.getBoolean("success")) {
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
                } finally {
                    TimeUtil.sleep(1000);
                }
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
                        if (joFinishTask.getBoolean("success")) {
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
            }
        } catch (Throwable th) {
            Log.i(TAG, "startEnergyRain err:");
            Log.printStackTrace(TAG, th);
        }
    }

    private void energyRain() {
        try {
            JSONObject joEnergyRainHome = new JSONObject(AntForestRpcCall.queryEnergyRainHome());
            if ("SUCCESS".equals(joEnergyRainHome.getString("resultCode"))) {
                if (joEnergyRainHome.getBoolean("canPlayToday")) {
                    startEnergyRain();
                }
                if (joEnergyRainHome.getBoolean("canGrantStatus")) {
                    Log.record("有送能量雨的机会");
                    JSONObject joEnergyRainCanGrantList = new JSONObject(
                            AntForestRpcCall.queryEnergyRainCanGrantList());
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
            if ("SUCCESS".equals(joEnergyRainHome.getString("resultCode"))
                    && joEnergyRainHome.getBoolean("canPlayToday")) {
                startEnergyRain();
            }
        } catch (Throwable th) {
            Log.i(TAG, "energyRain err:");
            Log.printStackTrace(TAG, th);
        }
    }

    private void useDoubleCard() {
        synchronized (doubleCardLockObj) {
            try {
                if (doubleCard.getValue() && doubleEndTime < System.currentTimeMillis()) {
                    if (hasDoubleCardTime() && Status.canDoubleToday()) {
                        JSONObject jo = new JSONObject(AntForestRpcCall.queryPropList(false));
                        if ("SUCCESS".equals(jo.getString("resultCode"))) {
                            JSONArray forestPropVOList = jo.getJSONArray("forestPropVOList");
                            String propId = null;
                            String propType = null;
                            String propName = null;
                            for (int i = 0; i < forestPropVOList.length(); i++) {
                                JSONObject forestPropVO = forestPropVOList.getJSONObject(i);
                                String tmpPropType = forestPropVO.getString("propType");
                                if ("LIMIT_TIME_ENERGY_DOUBLE_CLICK".equals(tmpPropType)) {
                                    JSONArray propIdList = forestPropVO.getJSONArray("propIdList");
                                    propId = propIdList.getString(0);
                                    propType = tmpPropType;
                                    propName = "限时双击卡";
                                    break;
                                }
                                if ("ENERGY_DOUBLE_CLICK".equals(tmpPropType)) {
                                    JSONArray propIdList = forestPropVO.getJSONArray("propIdList");
                                    propId = propIdList.getString(0);
                                    propType = tmpPropType;
                                    propName = "双击卡";
                                }
                            }
                            if (!StringUtil.isEmpty(propId)) {
                                jo = new JSONObject(AntForestRpcCall.consumeProp(propId, propType));
                                String resultCode = jo.getString("resultCode");
                                if ("SUCCESS".equals(resultCode)) {
                                    doubleEndTime = System.currentTimeMillis() + 1000 * 60 * 5;
                                    Log.forest("使用道具🎭[" + propName + "]");
                                    Status.DoubleToday();
                                } else if ("DOUBLE_CLICK_IN_USE".equals(resultCode)) {
                                    doubleEndTime = System.currentTimeMillis() + 1000 * 60 * 5;
                                    Log.forest("已使用道具🎭[" + propName + "]");
                                    Status.DoubleToday();
                                } else {
                                    Log.record(jo.getString("resultDesc"));
                                    Log.i(jo.toString());
                                    updateDoubleTime();
                                }
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                Log.i(TAG, "useDoubleCard err:");
                Log.printStackTrace(TAG, th);
            }
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
            if (!jsonObject.getBoolean("success")) {
                Log.i(TAG + ".ecoLife.queryHomePage", jsonObject.optString("resultDesc"));
                return;
            }
            JSONObject data = jsonObject.getJSONObject("data");
            if (!data.getBoolean("openStatus") && !ecoLifeOpen.getValue()) {
                Log.forest("绿色任务☘未开通");
                return;
            } else if (!data.getBoolean("openStatus")) {
                jsonObject = new JSONObject(EcoLifeRpcCall.openEcolife());
                if (!jsonObject.getBoolean("success")) {
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
            if (!jsonObject.getBoolean("success")) {
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
            if (!jsonObject.getBoolean("success")) {
                Log.i(TAG + ".photoGuangPan.uploadDishImage", jsonObject.optString("resultDesc"));
                return;
            }
            //上传餐后照片
            str = EcoLifeRpcCall.uploadDishImage("AFTER_MEALS",
                    photoGuangPanAfterStr, 0.00040030346, 0.99891376, 0.0006858421, dayPoint);
            jsonObject = new JSONObject(str);
            if (!jsonObject.getBoolean("success")) {
                Log.i(TAG + ".photoGuangPan.uploadDishImage", jsonObject.optString("resultDesc"));
                return;
            }
            //提交
            str = EcoLifeRpcCall.tick("photoguangpan", dayPoint, source);
            jsonObject = new JSONObject(str);
            if (!jsonObject.getBoolean("success")) {
                Log.i(TAG + ".photoGuangPan.tick", jsonObject.optString("resultDesc"));
                return;
            }
            Log.forest("光盘行动💿任务完成");
        } catch (Throwable t) {
            Log.i(TAG, "photoGuangPan err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /* 神奇物种 */

    private boolean antdodoLastDay(String endDate) {
        long timeStemp = System.currentTimeMillis();
        long endTimeStemp = Log.timeToStamp(endDate);
        return timeStemp < endTimeStemp && (endTimeStemp - timeStemp) < 86400000L;
    }

    public boolean antdodoIn8Days(String endDate) {
        long timeStemp = System.currentTimeMillis();
        long endTimeStemp = Log.timeToStamp(endDate);
        return timeStemp < endTimeStemp && (endTimeStemp - timeStemp) < 691200000L;
    }

    private void antdodoCollect() {
        try {
            String s = AntForestRpcCall.queryAnimalStatus();
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                JSONObject data = jo.getJSONObject("data");
                if (data.getBoolean("collect")) {
                    Log.record("神奇物种卡片今日收集完成！");
                } else {
                    collectAnimalCard();
                }
            } else {
                Log.i(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.i(TAG, "antdodoCollect err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void collectAnimalCard() {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.antdodoHomePage());
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                JSONObject data = jo.getJSONObject("data");
                JSONObject animalBook = data.getJSONObject("animalBook");
                String bookId = animalBook.getString("bookId");
                String endDate = animalBook.getString("endDate") + " 23:59:59";
                antdodoReceiveTaskAward();
                if (!antdodoIn8Days(endDate) || antdodoLastDay(endDate))
                    antdodoPropList();
                JSONArray ja = data.getJSONArray("limit");
                int index = -1;
                for (int i = 0; i < ja.length(); i++) {
                    jo = ja.getJSONObject(i);
                    if ("DAILY_COLLECT".equals(jo.getString("actionCode"))) {
                        index = i;
                        break;
                    }
                }
                Set<String> set = sendFriendCard.getValue();
                if (index >= 0) {
                    int leftFreeQuota = jo.getInt("leftFreeQuota");
                    for (int j = 0; j < leftFreeQuota; j++) {
                        jo = new JSONObject(AntForestRpcCall.antdodoCollect());
                        if ("SUCCESS".equals(jo.getString("resultCode"))) {
                            data = jo.getJSONObject("data");
                            JSONObject animal = data.getJSONObject("animal");
                            String ecosystem = animal.getString("ecosystem");
                            String name = animal.getString("name");
                            Log.forest("神奇物种🦕[" + ecosystem + "]#" + name);
                            if (!set.isEmpty()) {
                                for (String userId : set) {
                                    if (!UserIdMap.getCurrentUid().equals(userId)) {
                                        int fantasticStarQuantity = animal.optInt("fantasticStarQuantity", 0);
                                        if (fantasticStarQuantity == 3) {
                                            sendCard(animal, userId);
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            Log.i(TAG, jo.getString("resultDesc"));
                        }
                    }
                }
                if (!set.isEmpty()) {
                    for (String userId : set) {
                        if (!UserIdMap.getCurrentUid().equals(userId)) {
                            sendAntdodoCard(bookId, userId);
                            break;
                        }
                    }
                }
            } else {
                Log.i(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.i(TAG, "collect err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void antdodoReceiveTaskAward() {
        try {
            String s = AntForestRpcCall.antdodoTaskList();
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                JSONArray taskGroupInfoList = jo.getJSONObject("data").optJSONArray("taskGroupInfoList");
                if (taskGroupInfoList == null)
                    return;
                for (int i = 0; i < taskGroupInfoList.length(); i++) {
                    JSONObject antdodoTask = taskGroupInfoList.getJSONObject(i);
                    JSONArray taskInfoList = antdodoTask.getJSONArray("taskInfoList");
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
                            JSONObject joAward = new JSONObject(
                                    AntForestRpcCall.antdodoReceiveTaskAward(sceneCode, taskType));
                            if (joAward.getBoolean("success"))
                                Log.forest("任务奖励🎖️[" + taskTitle + "]#" + awardCount + "个");
                            else
                                Log.record("领取失败，" + s);
                            Log.i(joAward.toString());
                        } else if (TaskStatus.TODO.name().equals(taskStatus)) {
                            if ("SEND_FRIEND_CARD".equals(taskType)) {
                                JSONObject joFinishTask = new JSONObject(
                                        AntForestRpcCall.antdodoFinishTask(sceneCode, taskType));
                                if (joFinishTask.getBoolean("success")) {
                                    Log.forest("物种任务🧾️[" + taskTitle + "]");
                                    antdodoReceiveTaskAward();
                                    return;
                                } else {
                                    Log.record("完成任务失败，" + taskTitle);
                                }
                            }
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.i(s);
            }
        } catch (Throwable t) {
            Log.i(TAG, "antdodoReceiveTaskAward err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void antdodoPropList() {
        try {
            th:
            do {
                JSONObject jo = new JSONObject(AntForestRpcCall.antdodoPropList());
                if ("SUCCESS".equals(jo.getString("resultCode"))) {
                    JSONArray propList = jo.getJSONObject("data").optJSONArray("propList");
                    if (propList == null) {
                        return;
                    }
                    for (int i = 0; i < propList.length(); i++) {
                        JSONObject prop = propList.getJSONObject(i);
                        String propType = prop.getString("propType");
                        if ("COLLECT_TIMES_7_DAYS".equals(propType)) {
                            JSONArray propIdList = prop.getJSONArray("propIdList");
                            String propId = propIdList.getString(0);
                            String propName = prop.getJSONObject("propConfig").getString("propName");
                            int holdsNum = prop.optInt("holdsNum", 0);
                            try {
                                jo = new JSONObject(AntForestRpcCall.antdodoConsumeProp(propId, propType));
                                if (!"SUCCESS".equals(jo.getString("resultCode"))) {
                                    Log.record(jo.getString("resultDesc"));
                                    Log.i(jo.toString());
                                    continue;
                                }
                                JSONObject useResult = jo.getJSONObject("data").getJSONObject("useResult");
                                JSONObject animal = useResult.getJSONObject("animal");
                                String ecosystem = animal.getString("ecosystem");
                                String name = animal.getString("name");
                                Log.forest("使用道具🎭[" + propName + "]#" + ecosystem + "-" + name);
                                Set<String> map = sendFriendCard.getValue();
                                for (String userId : map) {
                                    if (!UserIdMap.getCurrentUid().equals(userId)) {
                                        int fantasticStarQuantity = animal.optInt("fantasticStarQuantity", 0);
                                        if (fantasticStarQuantity == 3) {
                                            sendCard(animal, userId);
                                        }
                                        break;
                                    }
                                }
                                if (holdsNum > 1) {
                                    continue th;
                                }
                            } finally {
                                TimeUtil.sleep(1000);
                            }
                        }
                    }
                }
                break;
            } while (true);
        } catch (Throwable th) {
            Log.i(TAG, "antdodoPropList err:");
            Log.printStackTrace(TAG, th);
        }
    }

    private void sendAntdodoCard(String bookId, String targetUser) {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.queryBookInfo(bookId));
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                JSONArray animalForUserList = jo.getJSONObject("data").optJSONArray("animalForUserList");
                for (int i = 0; i < animalForUserList.length(); i++) {
                    JSONObject animalForUser = animalForUserList.getJSONObject(i);
                    int count = animalForUser.getJSONObject("collectDetail").optInt("count");
                    if (count <= 0)
                        continue;
                    JSONObject animal = animalForUser.getJSONObject("animal");
                    for (int j = 0; j < count; j++) {
                        sendCard(animal, targetUser);
                        Thread.sleep(500L);
                    }
                }
            }
        } catch (Throwable th) {
            Log.i(TAG, "sendAntdodoCard err:");
            Log.printStackTrace(TAG, th);
        }
    }

    private void sendCard(JSONObject animal, String targetUser) {
        try {
            String animalId = animal.getString("animalId");
            String ecosystem = animal.getString("ecosystem");
            String name = animal.getString("name");
            JSONObject jo = new JSONObject(AntForestRpcCall.antdodoSocial(animalId, targetUser));
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                Log.forest("赠送卡片🦕[" + UserIdMap.getMaskName(targetUser) + "]#" + ecosystem + "-" + name);
            } else {
                Log.i(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable th) {
            Log.i(TAG, "sendCard err:");
            Log.printStackTrace(TAG, th);
        }
    }

    private void queryUserPatrol() {
        try {
            do {
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
                                    continue;
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
                            TimeUtil.sleep(1000);
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
            if (s == null) {
                s = AntForestRpcCall.patrolKeepGoing(nodeIndex, patrolId, "image");
            }
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                JSONArray jaEvents = jo.optJSONArray("events");
                if (jaEvents == null || jaEvents.length() == 0)
                    return;
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
                if (!"GOING".equals(jo.getString("currentStatus")))
                    return;
                JSONObject materialInfo = events.getJSONObject("materialInfo");
                String materialType = materialInfo.optString("materialType", "image");
                String str = AntForestRpcCall.patrolKeepGoing(currentNode, patrolId, materialType);
                patrolKeepGoing(str, nodeIndex, patrolId);

            } else {
                Log.i(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.i(TAG, "patrolKeepGoing err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void queryAnimalAndPiece(boolean canConsumeProp) {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.queryAnimalAndPiece(0));
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                JSONArray animalProps = jo.getJSONArray("animalProps");
                for (int i = 0; i < animalProps.length(); i++) {
                    jo = animalProps.getJSONObject(i);
                    JSONObject animal = jo.getJSONObject("animal");
                    int id = animal.getInt("id");
                    if (canConsumeProp && animalConsumeProp.getValue()) {
                        JSONObject main = jo.optJSONObject("main");
                        if (main != null && main.optInt("holdsNum", 0) > 0) {
                            canConsumeProp = !AnimalConsumeProp(id);
                        }
                    }
                    JSONArray pieces = jo.getJSONArray("pieces");
                    boolean canCombine = true;
                    for (int j = 0; j < pieces.length(); j++) {
                        jo = pieces.optJSONObject(j);
                        if (jo == null || jo.optInt("holdsNum", 0) <= 0) {
                            canCombine = false;
                            break;
                        }
                    }
                    if (canCombine) {
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
            JSONObject jo = new JSONObject(AntForestRpcCall.queryAnimalAndPiece(animalId));
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                JSONArray animalProps = jo.getJSONArray("animalProps");
                jo = animalProps.getJSONObject(0);
                JSONObject animal = jo.getJSONObject("animal");
                int id = animal.getInt("id");
                String name = animal.getString("name");
                JSONArray pieces = jo.getJSONArray("pieces");
                boolean canCombine = true;
                JSONArray piecePropIds = new JSONArray();
                for (int j = 0; j < pieces.length(); j++) {
                    jo = pieces.optJSONObject(j);
                    if (jo == null || jo.optInt("holdsNum", 0) <= 0) {
                        canCombine = false;
                        break;
                    } else {
                        piecePropIds.put(jo.getJSONArray("propIdList").getString(0));
                    }
                }
                if (canCombine) {
                    jo = new JSONObject(AntForestRpcCall.combineAnimalPiece(id, piecePropIds.toString()));
                    if ("SUCCESS".equals(jo.getString("resultCode"))) {
                        Log.forest("合成动物💡[" + name + "]");
                        combineAnimalPiece(id);
                    } else {
                        Log.i(TAG, jo.getString("resultDesc"));
                    }
                }
            } else {
                Log.i(TAG, jo.getString("resultDesc"));
            }
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
            super(AntForestV2.getBubbleTimerTid(ui, bi), pt - 3000 - advanceTime.getValue());
            userId = ui;
            bubbleId = bi;
            produceTime = pt;
        }

        @Override
        public Runnable setRunnable() {
            return () -> {
                String userName = UserIdMap.getMaskName(userId);
                long readyTime = produceTime - advanceTime.getValue() + offsetTime.get() - System.currentTimeMillis();
                if (readyTime > 0) {
                    try {
                        Thread.sleep(readyTime);
                    } catch (InterruptedException e) {
                        Log.i("终止[" + userName + "]蹲点收取任务, 任务ID[" + getId() + "]");
                        return;
                    }
                }
                Log.record("执行蹲点收取[" + userName + "]");
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
