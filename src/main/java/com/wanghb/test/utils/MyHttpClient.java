package com.wanghb.test.utils;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.util.concurrent.TimeUnit;

public class MyHttpClient {

    /**
     * 最大连接数
     */
    private Integer maxTotal = 100;

    /**
     * 最大并发数
     */
    private Integer defaultMaxPerRoute = 20;

    /**
     * 设置连接超时时间，单位毫秒
     */
    private Integer connectTimeout;

    /**
     * 设置从connect Manager获取Connection 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
     */
    private Integer connectionRequestTimeout;

    /**
     * 请求获取数据的超时时间，单位毫秒
     */
    private Integer socketTimeout;

    private boolean staleConnectionCheckEnabled;

    private CloseableHttpClient httpClient;

    public MyHttpClient() {
        this(100, 20, 1000, 5000, 30000, false);
    }

    public MyHttpClient(Integer maxTotal, Integer defaultMaxPerRoute, Integer connectTimeout,
                        Integer connectionRequestTimeout, Integer socketTimeout, boolean staleConnectionCheckEnabled) {
        this.maxTotal = maxTotal;
        this.defaultMaxPerRoute = defaultMaxPerRoute;
        this.connectTimeout = connectTimeout;
        this.connectionRequestTimeout = connectionRequestTimeout;
        this.socketTimeout = socketTimeout;
        this.staleConnectionCheckEnabled = staleConnectionCheckEnabled;
        PoolingHttpClientConnectionManager connMgr = this.getHttpClientConnectionManager();
        httpClient = getCloseableHttpClient(connMgr);
        IdleConnectionMonitorThread monitor = new IdleConnectionMonitorThread(connMgr);
        monitor.start();
    }

    public MyHttpClientUtils build(RequestConfig config) {
        return new MyHttpClientUtils(httpClient, config);
    }

    public MyHttpClientUtils build() {
        return new MyHttpClientUtils(httpClient, null);
    }

    private ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
        ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                HeaderElementIterator it = new BasicHeaderElementIterator(
                        response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        return Long.parseLong(value) * 1000;
                    }
                }
                return 60 * 1000;// 如果没有约定，则默认定义时长为60s
            }
        };
        return myStrategy;
    }

    private PoolingHttpClientConnectionManager getHttpClientConnectionManager() {
        PoolingHttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager();
        // 最大连接数
        httpClientConnectionManager.setMaxTotal(maxTotal);
        // 并发数
        httpClientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        return httpClientConnectionManager;
    }

    private RequestConfig getRequestConfig() {
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectionRequestTimeout)
                .setSocketTimeout(socketTimeout).setStaleConnectionCheckEnabled(staleConnectionCheckEnabled);
        return builder.build();
    }

    private CloseableHttpClient getCloseableHttpClient(PoolingHttpClientConnectionManager connMgr) {
        HttpClientBuilder builder = HttpClients.custom().setConnectionManager(getHttpClientConnectionManager())
                .setKeepAliveStrategy(connectionKeepAliveStrategy()).setDefaultRequestConfig(getRequestConfig());
        return builder.build();
    }

    private static class IdleConnectionMonitorThread extends Thread {

        private final HttpClientConnectionManager connMgr;
        private volatile boolean shutdown;

        public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(5000);
                        // Close expired connections
                        connMgr.closeExpiredConnections();
                        // Optionally, close connections
                        // that have been idle longer than 30 sec
                        connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
    public static void main(String[] args) {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "ERROR");

        MyHttpClient client = new MyHttpClient();
        try {
            MyHttpClientService service = client.build();
            while (true) {
                long stTime = System.currentTimeMillis();
                String res = service.doGet(
                        "http://test.qiuyuehui.com/hnlive-test/hylive/onlineuser/getuserlist?reqId=1&pageNo=1&pageSize=10");
                System.out.println("#########" + (System.currentTimeMillis() - stTime));
                System.out.println(res);
                Tools.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     */
}