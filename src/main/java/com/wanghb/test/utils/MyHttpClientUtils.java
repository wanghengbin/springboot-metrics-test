package com.wanghb.test.utils;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyHttpClientUtils {

    private CloseableHttpClient httpClient;

    private RequestConfig config;

    public MyHttpClientUtils(CloseableHttpClient httpClient, RequestConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    /**
     * 不带参数的get请求，如果状态码为200，则返回body，如果不为200，则返回null
     * 
     * @param url
     * @return
     * @throws Exception
     */
    public String doGet(String url) throws Exception {
        // 声明 http get 请求
        HttpGet httpGet = new HttpGet(url);

        // 装载配置信息
        if (config != null) {
            httpGet.setConfig(config);
        }

        // 发起请求
        CloseableHttpResponse response = this.httpClient.execute(httpGet);

        // 判断状态码是否为200
        if (response.getStatusLine().getStatusCode() == 200) {
            // 返回响应体的内容
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        }
        return null;
    }

    /**
     * 带参数的get请求，如果状态码为200，则返回body，如果不为200，则返回null
     * 
     * @param url
     * @return
     * @throws Exception
     */
    public String doGet(String url, Map<String, Object> map) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(url);

        if (map != null) {
            // 遍历map,拼接请求参数
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue().toString());
            }
        }

        // 调用不带参数的get请求
        return this.doGet(uriBuilder.build().toString());

    }

    public HttpResult doPut(String url, Map<String, Object> map) throws Exception {
        // 声明httpPost请求
        HttpPut httpPut = new HttpPut(url);
        // 加入配置信息
        httpPut.setConfig(config);

        // 判断map是否为空，不为空则进行遍历，封装from表单对象
        if (map != null) {
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }
            // 构造from表单对象
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(list, "UTF-8");

            // 把表单放到post里
            httpPut.setEntity(urlEncodedFormEntity);
        }

        // 发起请求
        CloseableHttpResponse response = this.httpClient.execute(httpPut);
        return new HttpResult(response.getStatusLine().getStatusCode(),
                EntityUtils.toString(response.getEntity(), "UTF-8"));
    }

    /**
     * 带参数的post请求
     * 
     * @param url
     * @param map
     * @return
     * @throws Exception
     */
    public HttpResult doPost(String url, Map<String, Object> map) throws Exception {
        // 声明httpPost请求
        HttpPost httpPost = new HttpPost(url);
        // 加入配置信息
        httpPost.setConfig(config);

        // 判断map是否为空，不为空则进行遍历，封装from表单对象
        if (map != null) {
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }
            // 构造from表单对象
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(list, "UTF-8");

            // 把表单放到post里
            httpPost.setEntity(urlEncodedFormEntity);
        }

        // 发起请求
        CloseableHttpResponse response = this.httpClient.execute(httpPost);
        return new HttpResult(response.getStatusLine().getStatusCode(),
                EntityUtils.toString(response.getEntity(), "UTF-8"));
    }

    public HttpResult doPostJSON(String url, Map<String, String> urlParams, String jsonStr) throws Exception {
        return doPostJSONWithHeader(url, urlParams, jsonStr, null, ContentType.APPLICATION_JSON);
    }

    public HttpResult doPost(String url, Map<String, String> urlParams, String body, ContentType contentType)
            throws Exception {
        return doPostJSONWithHeader(url, urlParams, body, null, contentType);
    }

    public HttpResult doPostJSONWithHeader(String url, Map<String, String> urlParams, String jsonStr, Map<String,String> headerMap, ContentType contentType) throws Exception {
        // 判断map是否为空，不为空则进行遍历，封装from表单对象
        if (urlParams != null) {
            url=fixUrl(url, urlParams) ;
        }

        // 声明httpPost请求
        HttpPost httpPost = new HttpPost(url);
        // 加入配置信息
        httpPost.setConfig(config);


        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");//表示客户端发送给服务器端的数据格式
        if (headerMap!=null && !headerMap.isEmpty()) {
            for(Map.Entry<String, String> en : headerMap.entrySet()){
                if(!StringUtils.isEmpty(en.getKey()) && en.getValue() != null) {
                    httpPost.setHeader(en.getKey(),en.getValue());
                }
            }
        }
        StringEntity entity = new StringEntity(jsonStr, contentType);
        httpPost.setEntity(entity);

        // 发起请求
        CloseableHttpResponse response = this.httpClient.execute(httpPost);
        String resBody="";
        if(response.getEntity()!=null){
            resBody= EntityUtils.toString(
                response.getEntity(), "UTF-8");
        }
        return new HttpResult(response.getStatusLine().getStatusCode(), resBody);
    }

    /**
     * 不带参数post请求
     * 
     * @param url
     * @return
     * @throws Exception
     */
    public HttpResult doPost(String url) throws Exception {
        return this.doPost(url, null);
    }

    private String fixUrl(String url, Map<String, String> par) {
        String parstr = getPar(par);
        if (!parstr.equals("")) {
            int npos = url.indexOf('?');
            if (npos > 0) {
                if (!url.endsWith("?")) {
                    url += "&";
                }
            } else {
                url += "?";
            }
            url += parstr;
        }
        return url;
    }

    private String getPar(Map<String, String> m) {
        if (m != null && m.size() > 0) {
            StringBuilder par = new StringBuilder();

            for (Map.Entry<String, String> en : m.entrySet()) {
                if (!StringUtils.isEmpty(en.getKey()) && en.getValue() != null) {
                    String value = en.getValue();
                    try {
                        value = java.net.URLEncoder.encode(en.getValue(), "utf-8");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    par.append("&").append(en.getKey()).append("=").append(value);
                }
            }
            if (par.length() > 0) {
                return par.substring(1);
            }
        }
        return "";
    }

}