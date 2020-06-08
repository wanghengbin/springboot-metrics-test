package com.wanghb.test.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * @author wanghb 2020年6月2日
 */
public class RequestMetrics {

    private static RequestMetrics _this = null;

    private Logger logger = LoggerFactory.getLogger("requestMetrics");

    private String appName = "";

    private int mswitch = 1;

    private Map<String, CtInfo> cmap1;

    private Map<String, CtInfo> cmap2;

    private BlockingQueue<Map<String, Object>> bqueue;

    // @Value("${requestCounter.queue.size:300}")
    private Integer queueSize;

    // @Value("${requestCounter.appName.prefix}")
    private String appNamePrefix;

    // @Value("${requestCounter.uriPatterns}")
    private List<Map<String, Object>> uriPatterns;

    // @Value("${requestCounter.influxDB.url}")
    private String influxDBUrl;

    private static String jvm = System.getProperty("instanceKey", "jvm");

    private static MyHttpClientUtils myHttpClientService = new MyHttpClient(10,
                    10,
                    3000,
                    30000,
                    30000,
                    false)
                    .build();

    private static final String[] falconVtypes = new String[] { "pv", "avgtime", "maxtime", "mintime", "ltime50",
            "ltimeb100", "ltimeb1000", "ltime3000", "ltime6000", "gtime6000", "error" };

    private static final ThreadPoolExecutor mainThreadPool = new ThreadPoolExecutor(
            3,
            20,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(10)
    );

    private RequestMetrics(String appName, String influxDB_url) {
        bqueue = new ArrayBlockingQueue<Map<String, Object>>(queueSize == null ? 300 : queueSize);
        if (StringUtils.isNotBlank(appName)) {
            if (StringUtils.isNotBlank(appNamePrefix)) {
                this.appName = appNamePrefix + "_" + appName;
            } else {
                this.appName = appName;
            }
        }
        if (StringUtils.isNotBlank(influxDB_url)) {
            influxDBUrl = influxDB_url;
        }
        if (StringUtils.isBlank(influxDBUrl)) {
            return;
        }
        cmap1 = new HashMap<>();
        cmap2 = new HashMap<>();
        Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Map<String, Object> bitem = bqueue.take();
                        reqStatistics(String.valueOf(bitem.get("measurements")),
                                String.valueOf(bitem.get("uri")),
                                Long.parseLong(String.valueOf(bitem.get("runTime"))),
                                Boolean.parseBoolean(String.valueOf(bitem.get("resStatus"))),
                                mswitch == 1 ? cmap1 : cmap2);
                    } catch (Exception e) {
                        logger.error("bqueue take:", e);
                    }
                }
            }
        };
        mainThreadPool.execute(mRunnable);

        Runnable scheduleRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    switch (mswitch) {
                        case 1:
                            mswitch = 2;
                            try{
                                Thread.sleep(100);
                                Thread.yield();
                            } catch (Exception e){
                            }
                            rsyncData(cmap1);
                            break;
                        case 2:
                            mswitch = 1;
                            try{
                                Thread.sleep(100);
                                Thread.yield();
                            } catch (Exception e){
                            }
                            rsyncData(cmap2);
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    logger.error("rsyncData", e);
                }
            }
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
        // 一分钟同步一次记录
        service.scheduleAtFixedRate(scheduleRunnable, 1, 1, TimeUnit.MINUTES);
    }

    public static RequestMetrics getInstance(){
        if (_this == null) {
            synchronized (RequestMetrics.class) {
                if (_this == null) {
                    _this = new RequestMetrics("", null);
                    _this.setQueueSize(300);
                }
            }
        }
        return _this;
    }

    public static RequestMetrics buildByEnv(Environment env){
        if (_this == null) {
            synchronized (RequestMetrics.class) {
                if (_this == null) {
                    if (env != null) {
                        boolean enabled = Boolean.parseBoolean(env.getProperty("management.metrics.export.influx.enabled"));
                        String influxDBUrl = "";
                        if (enabled) {
                            String db = env.getProperty("management.metrics.export.influx.db");
                            String uri = env.getProperty("management.metrics.export.influx.uri");
                            String userName = env.getProperty("management.metrics.export.influx.user-name");
                            String password = env.getProperty("management.metrics.export.influx.password");
                            if (StringUtils.isNotBlank(uri)
                                    && StringUtils.isNotBlank(db)
                                    && StringUtils.isNotBlank(userName)
                                    && StringUtils.isNotBlank(password)) {
                                influxDBUrl = uri + "/write?db=" + db + "&u=" + userName + "&p=" + password;
                            }
                        }
                        String appnName = env.getProperty("spring.application.name");
                        if (StringUtils.isNotBlank(influxDBUrl)) {
                            _this = new RequestMetrics(appnName, influxDBUrl);
                            _this.setQueueSize(300);
                        }
                    }
                    if (_this == null) {
                        _this = new RequestMetrics("", null);
                        _this.setQueueSize(300);
                    }
                }
            }
        }
        return _this;
    }

    public void doCount(String measurements, String uri, long runTime, boolean resStatus) {
        if (StringUtils.isNotBlank(uri)
                && StringUtils.isNotBlank(this.appName)) {
            try {
                Map<String, Object> bitem = new HashMap<>();
                bitem.put("measurements", measurements);
                bitem.put("uri", uri);
                bitem.put("runTime", runTime);
                bitem.put("resStatus", resStatus);
                bqueue.add(bitem);
            } catch (Exception e) {
                logger.error("doCount error:", e);
            }
        }
    }

    public void destroy() {
        rsyncData(cmap1);
        rsyncData(cmap2);
    }

    private void reqStatistics(String measurements, String uri, long runTime, boolean resStatus,
            Map<String, CtInfo> cmap) {
        runTime = runTime < 1 ? 1 : runTime;
        runTime = !resStatus ? runTime * -1 : runTime;
        uri = getPatternUri(uri);
        String key = measurements + "_" + uri;
        CtInfo ctinfo = cmap.get(key);
        if (ctinfo == null) {
            ctinfo = cmap.get(key);
            if (ctinfo == null) {
                ctinfo = new CtInfo(measurements, uri, runTime);
                cmap.put(key, ctinfo);
            } else {
                ctinfo.addRecord(runTime);
            }
        } else {
            ctinfo.addRecord(runTime);
        }
    }

    private void rsyncData(Map<String, CtInfo> map) {
        Iterator<String> keys = map.keySet().iterator();
        int index = 0;
        long timestamp = System.currentTimeMillis() / 1000;
        StringBuffer bodySb = new StringBuffer();
        String host = NetUtils.getLocalAddress().getHostAddress();
        String str = "%s,appName=%s,host=%s,jvm=%s,uri=%s count=%d,avgtime=%d,"
                + "maxtime=%d,mintime=%d,tc50=%d,tc100=%d,tc500=%d,tc1000=%d,tc3000=%d,tc6000=%d,tc-1=%d,tc-500=%d\n";
        while (keys.hasNext()) {
            String mkey = keys.next();
            CtInfo ctinfo = map.get(mkey);
            if (ctinfo != null) {
                if (index >= 50) {
                    writeToInfluxDB(bodySb.toString());
                    index = 0;
                    bodySb = new StringBuffer();
                }
                index++;
                if (ctinfo.getTimeList() != null && ctinfo.getTimeList().size() > 0) {
                    ctinfo.compute();
                    String uri = ctinfo.getUri();
                    int count = ctinfo.getTimeList().size();
                    long avgtime = ctinfo.getAvgTime();
                    long maxtime = ctinfo.getMaxTime();
                    long mintime = ctinfo.getMinTime();
                    bodySb.append(String.format(str, ctinfo.getMeasurements(), this.appName, host,
                            jvm, uri, count, avgtime, maxtime, mintime,
                            ctinfo.getTimeCountMap().get(50) == null ? 0 : ctinfo.getTimeCountMap().get(50),
                            ctinfo.getTimeCountMap().get(100) == null ? 0 : ctinfo.getTimeCountMap().get(100),
                            ctinfo.getTimeCountMap().get(500) == null ? 0 : ctinfo.getTimeCountMap().get(500),
                            ctinfo.getTimeCountMap().get(1000) == null ? 0 : ctinfo.getTimeCountMap().get(1000),
                            ctinfo.getTimeCountMap().get(3000) == null ? 0 : ctinfo.getTimeCountMap().get(3000),
                            ctinfo.getTimeCountMap().get(6000) == null ? 0 : ctinfo.getTimeCountMap().get(6000),
                            ctinfo.getTimeCountMap().get(-1) == null ? 0 : ctinfo.getTimeCountMap().get(-1),
                            ctinfo.getTimeCountMap().get(-500) == null ? 0 : ctinfo.getTimeCountMap().get(-500)));

//                    // falcion数据装载
//                    for (String vtype : falconVtypes) {
//                        Map<String, Object> reqMap = new HashMap<String, Object>(30);
//                        reqMap.put("endpoint", "jishu2api");
//                        reqMap.put("metric", this.appName + "." + host.replaceAll("\\.", "_") + "."
//                                        + jvm + "." + vtype);
//                        reqMap.put("timestamp", timestamp);
//                        reqMap.put("step", 60);
//                        reqMap.put("counterType", "GAUGE");
//                        if (vtype.equals("pv")) {
//                            reqMap.put("value", count);
//                        } else if (vtype.equals("avgtime")) {
//                            reqMap.put("value", ctinfo.getAvgTime());
//                        } else if (vtype.equals("maxtime")) {
//                            reqMap.put("value", ctinfo.getMaxTime());
//                        } else if (vtype.equals("mintime")) {
//                            reqMap.put("value", ctinfo.getMinTime());
//                        } else if (vtype.equals("ltime50")) {
//                            reqMap.put("value", ctinfo.getTimeCountMap().get(50));
//                        } else if (vtype.equals("ltimeb100")) {
//                            reqMap.put("value", ctinfo.getTimeCountMap().get(100));
//                        } else if (vtype.equals("ltimeb1000")) {
//                            reqMap.put("value", ctinfo.getTimeCountMap().get(1000));
//                        } else if (vtype.equals("ltime3000")) {
//                            reqMap.put("value", ctinfo.getTimeCountMap().get(3000));
//                        } else if (vtype.equals("ltime6000")) {
//                            reqMap.put("value", ctinfo.getTimeCountMap().get(6000));
//                        } else if (vtype.equals("gtime6000")) {
//                            reqMap.put("value", ctinfo.getTimeCountMap().get(-1));
//                        } else if (vtype.equals("error")) {
//                            reqMap.put("value", ctinfo.getTimeCountMap().get(-500));
//                        }
//                        StringBuffer tagsStr = new StringBuffer();
//                        // tagsStr.append(",host="+host);
//                        tagsStr.append("uri=" + ctinfo.getUri());
//                        reqMap.put("tags", tagsStr.toString());
//                    }
                }

                ctinfo.clean();
            }
        }
        bodySb.append(String.format(str, "app_heartbeat", this.appName, host, jvm, "/", 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0));
        writeToInfluxDB(bodySb.toString());
    }

    private void writeToInfluxDB(final String body) {
        mainThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (StringUtils.isNotBlank(influxDBUrl) && StringUtils.isNotBlank(appName)) {
                    String uuid = UUID.randomUUID().toString();
                    try {
                        String url = influxDBUrl + "&wtime=" + uuid;
                        HttpResult getRes = myHttpClientService.doPost(url, null, body, null);
                        if (getRes.getCode() != 204) {
                            logger.error("writeToInfluxDB", uuid + "--" + getRes.getCode() + "--" + body);
                        }
                    } catch (Exception e) {
                        logger.error("writeToInfluxDB", uuid + "--" + body, e);
                    }
                }
            }
        });
    }

    private String getPatternUri(String uri) {
        // 30秒钟取一次
        List<Map<String, Object>> temp = null;
        if (System.currentTimeMillis() % 30000 == 0) {
            temp = uriPatterns;
        }
        if (temp != null) {
            for (int i = 0; i < temp.size(); i++) {
                Map<String, Object> mm = temp.get(i);
                if (mm.containsKey("uri") && mm.containsKey("pattern")) {
                    boolean isMatch = Pattern.matches(String.valueOf(mm.get("pattern")), uri);
                    if (isMatch) {
                        return String.valueOf(mm.get("uri"));
                    }
                }
            }
        }
        return uri;
    }

    static class CtInfo {

        private String measurements;

        private String uri;

        private List<Long> timeList;

        private Map<Integer, Integer> timeCountMap;

        private long maxTime = 0;

        private long minTime = -1;

        private long avgTime = 0;

        CtInfo(String measurements, String uri, long time) {
            timeList = new LinkedList<>();
            timeCountMap = new HashMap<>();
            timeList.add(time);
            this.maxTime = time;
            this.uri = uri;
            this.measurements = measurements;
        }

        private void timeCountMapIncrement(Integer time) {
            Integer c = timeCountMap.get(time);
            timeCountMap.put(time, c == null ? 1 : c + 1);
        }

        public void compute() {
            if (timeList != null) {
                int size = timeList.size();
                long totalTime = 0L;

                for (Long t : timeList) {
                    if (t == null) {
                        continue;
                    }
                    if (t < 0) {
                        timeCountMapIncrement(-500);
                    }
                    t = Math.abs(t);
                    totalTime += t;
                    if (t > maxTime) {
                        maxTime = t;
                    }
                    if (minTime == -1 || minTime > t) {
                        minTime = t;
                    }
                    if (t < 50) {
                        timeCountMapIncrement(50);
                    }
                    if (t >= 50 && t < 100) {
                        timeCountMapIncrement(100);
                    }
                    if (t >= 100 && t < 500) {
                        timeCountMapIncrement(500);
                    }
                    if (t >= 500 && t < 1000) {
                        timeCountMapIncrement(1000);
                    }
                    if (t >= 1000 && t < 3000) {
                        timeCountMapIncrement(3000);
                    }
                    if (t >= 3000 && t < 6000) {
                        timeCountMapIncrement(6000);
                    }
                    if (t >= 6000) {
                        timeCountMapIncrement(-1);
                    }
                }
                avgTime = (int) (totalTime / size);
            }
        }

        public void addRecord(Long time) {
            timeList.add(time);
        }

        public void clean() {
            timeList.clear();
            timeList = new LinkedList<>();
            timeCountMap.clear();
            timeCountMap = new HashMap<>();
            maxTime = 0;
            minTime = -1;
            avgTime = 0;
        }

        /**
         * @param measurements the measurements to set
         */
        public void setMeasurements(String measurements) {
            this.measurements = measurements;
        }

        /**
         * @return the measurements
         */
        public String getMeasurements() {
            return measurements;
        }

        public List<Long> getTimeList() {
            return timeList;
        }

        public String getUri() {
            return uri;
        }

        public Long getMinTime() {
            return minTime;
        }

        public long getMaxTime() {
            return maxTime;
        }

        public long getAvgTime() {
            return avgTime;
        }

        public Map<Integer, Integer> getTimeCountMap() {
            return timeCountMap;
        }

    }

    /**-----------------------------------getter and setter--------------------------------------*/

    /**
     * @return the queueSize
     */
    public Integer getQueueSize() {
        return queueSize;
    }

    /**
     * @param queueSize the queueSize to set
     */
    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }

    /**
     * @return the appNamePrefix
     */
    public String getAppNamePrefix() {
        return appNamePrefix;
    }

    /**
     * @param appNamePrefix the appNamePrefix to set
     */
    public void setAppNamePrefix(String appNamePrefix) {
        this.appNamePrefix = appNamePrefix;
    }

    /**
     * @return the influxDBUrl
     */
    public String getInfluxDBUrl() {
        return influxDBUrl;
    }

    /**
     * @param influxDBUrl the influxDBUrl to set
     */
    public void setInfluxDBUrl(String influxDBUrl) {
        this.influxDBUrl = influxDBUrl;
    }
}
