package com.sjsz.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sjsz.util.GlobalVar;
import com.sjsz.util.HttpClientHelper;
import com.sjsz.util.ResponseHelper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/1/30.
 */
@Controller
@RequestMapping("/net")
public class NetController {

    private static int port = 32102;

    private static String userName = "base_user";

    private static String pwd = "Change_4si";

    private static String ip = "10.1.2.243";

    private static String charset = "utf-8";

    private HttpClientHelper httpClientHelper = new HttpClientHelper();


    @RequestMapping(value = "/test1")
    public void test1(HttpServletResponse response) {
        JSONObject jsonObject = new JSONObject();
        //set the URL
        String openidURL = "/rest/openapi/sm/session";
        //set parameters
        List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("userid", userName));
        parameters.add(new BasicNameValuePair("value", pwd));
        parameters.add(new BasicNameValuePair("ipaddr", "10.1.2.243"));

        //create a connection manager
        X509TrustManager tm = new X509TrustManager() {

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        //create a SSL connection
        SSLContext sslcontext = null;
        try {
            sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[] { tm }, null);
            SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("https", port, socketFactory));
            schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));//http 80 端口
            schemeRegistry.register(new Scheme("https", 443, socketFactory));//https 443端口

            //create a HttpClient to connect to the target host
            HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager(schemeRegistry));

            //set the URL
            String url = "https://" + ip + ":" + port + openidURL;

            //set the method
            HttpPut httpPut = new HttpPut(url);
            httpPut.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));

            //send the request
            HttpResponse httpResponse = httpClient.execute(httpPut);
           /* String result = EntityUtils.toString(httpResponse.getEntity(), Charset.forName("utf-8"));*/
            Map<String, String> retMap = parseResponse(getResult(httpResponse));
            String openID = "";
            if (retMap.get("code").equals("0")) {
                openID =  retMap.get("data");
            }
            System.out.println(openID);
            jsonObject.put("result", openID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ResponseHelper.write(response, jsonObject);
    }

    /**
     * 查询服务器设备列表
     *
     * @param response HttpServletResponse对象
     */
    @RequestMapping(value = "/device/list")
    public void getDeviceList(HttpServletResponse response, String servertype) {
        JSONObject jsonObject = new JSONObject();
        try {
            final String openID = getOpenID();
            if (null == openID || openID.isEmpty()) {
                System.out.println("failed to login.");
                return;
            }
            final String result = queryDeviceList(openID, servertype);
            System.out.println(result);
            jsonObject.put("result",result);
            jsonObject.put("state", true);
        } catch (Exception e) {
            e.printStackTrace();
            jsonObject.put("state", false);
        }
        ResponseHelper.write(response, jsonObject);
    }



    /**
     * 查询指定服务器的部件详细信息
     *
     * @param response HttpServletResponse对象
     * @param dn
     */
    @RequestMapping(value = "/device/detail")
    public void getDeviceDetail(HttpServletResponse response, String dn) {
        JSONObject jsonObject = new JSONObject();
        try {
            final String openID = getOpenID();
            if (null == openID || openID.isEmpty()) {
                System.out.println("failed to login.");
                return;
            }
            System.out.println(openID);
            if(StringUtils.isEmpty(dn)) {
                dn = "NE=34603447";
            } else {
                dn = "NE="+ dn;
            }
            final String result = queryDevice(openID, dn);
            System.out.println(result);
            jsonObject.put("result",result);
            jsonObject.put("state", true);
        } catch (Exception e) {
            jsonObject.put("state", false);
        }
        ResponseHelper.write(response, jsonObject);
    }

    /**
     * 根据指定参数获取存储设备列表
     *
     * @param response
     * @param dn 可选(设备标识，设备标识格式为“NE=xxx”，作为URL参数时需要做转义处理)
     */
    @RequestMapping(value = "/storage/device")
    public void getStorageDevice(HttpServletResponse response, String dn, String deviceSeries) {
        JSONObject jsonObject = new JSONObject();
        try {
            final String openID = getOpenID();
            if (null == openID || openID.isEmpty()) {
                System.out.println("failed to login.");
                return;
            }
            System.out.println(openID);
            /*String deviceSeries = "fcswitch"; //交换机*/
            final String result = queryStorageDevice(openID, deviceSeries, dn);
            System.out.println(result);
            jsonObject.put("result",result);
            jsonObject.put("state", true);
        } catch (Exception e) {
            jsonObject.put("state", false);
        }
        ResponseHelper.write(response, jsonObject);
    }

    private String queryStorageDevice(String openID, String deviceSeries, String dn) throws Exception {
        //set the URL and method
        final String queryURL = "/rest/openapi/storage/device";
        //set headers and parameters
        final List<BasicNameValuePair> headers = new ArrayList<BasicNameValuePair>();
        headers.add(new BasicNameValuePair("openid", openID));
        //set parameters
        final List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("deviceSeries", deviceSeries));
        if (!StringUtils.isEmpty(dn)) {
            parameters.add(new BasicNameValuePair("dn", dn));
        }

        //create a connection manager
        final X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        //create a SSL connection
        final SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { tm }, null);
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", port, socketFactory));

        //create a HttpClient to connect to the target host
        final HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager(schemeRegistry));

        //set the URL
        String url = "https://" + ip + ":" + port + queryURL;

        //set parameters
        if (null != parameters) {
            url += "?";
            boolean init = false;
            for (final BasicNameValuePair e : parameters) {
                if (!init) {
                    url += URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode(e.getValue(), "UTF-8");
                    init = true;
                } else {
                    url += "&" + URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode (e.getValue(), "UTF-8");
                }
            }
        }
        System.out.println(url);
        final HttpGet httpGet = new HttpGet(url);
        //set headers
        if (null != headers) {
            for (final BasicNameValuePair header : headers) {
                httpGet.setHeader(header.getName(), header.getValue());
            }
        }

        //send the request
        final HttpResponse response = httpClient.execute(httpGet);
        final String ret = getResult(response);
        System.out.println(ret);
        if (null == ret || ret.isEmpty()) {
            return "";
        }
        //get the result
        final Map<String, String> retMap = parseResponse(ret);
        if (retMap.get("code").equals("0")) {
            System.out.println(retMap.get("data"));
            return retMap.get("data");
        }
        return "";

    }

    /**
     * 获取openID
     *
     * @return
     * @throws Exception
     */
    @RequestMapping("/getOpenID")
    private static String getOpenID() throws Exception {
        //set the URL
        final String openidURL = "/rest/openapi/sm/session";
        //set parameters
        final List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("userid", userName));
        parameters.add(new BasicNameValuePair("value", pwd));
        parameters.add(new BasicNameValuePair("ipaddr", ip));
        //create a connection manager
        final X509TrustManager tm = new X509TrustManager()
        {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

        };

        //create a SSL connection
        final SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] {tm}, null);
        final SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", port, socketFactory));

        //create a HttpClient to connect to the target host
        final HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager (schemeRegistry));

        //set the URL
        final String url = "https://" + ip + ":" + port + openidURL;
        //set the method
        final HttpPut httpPut = new HttpPut(url);
        httpPut.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
        //send the request
        final HttpResponse response = httpClient.execute(httpPut);
        final Map<String, String> retMap = parseResponse(getResult(response));
        System.out.println(retMap);
        if (retMap.get("code").equals("0")) {
            return retMap.get("data");
        }
        return "";
    }

    /**
     * 查询服务器列表
     *
     * @param openID
     * @param serverType
     * @return
     * @throws Exception
     */
    private static String queryDeviceList(final String openID, String serverType) throws Exception {
        //set the URL and method
        final String query_URL = "/rest/openapi/server/device";
        //set headers and parameters
        final List<BasicNameValuePair> headers = new ArrayList<BasicNameValuePair>();
        headers.add(new BasicNameValuePair("openid", openID));
        //set parameters
        final List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("servertype", serverType));

        //create a connection manager
        final X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        //create a SSL connection
        final SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { tm }, null);
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", port, socketFactory));

        //create a HttpClient to connect to the target host
        final HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager(schemeRegistry));

        //set the URL
        String url = "https://" + ip + ":" + port + query_URL;

        //set parameters
        if (null != parameters) {
            url += "?";
            boolean init = false;
            for (final BasicNameValuePair e : parameters) {
                if (!init) {
                    url += URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode(e.getValue(), "UTF-8");
                    init = true;
                } else {
                    url += "&" + URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode (e.getValue(), "UTF-8");
                }
            }
        }
        System.out.println(url);
        final HttpGet httpGet = new HttpGet(url);
        //set headers
        if (null != headers) {
            for (final BasicNameValuePair header : headers) {
                httpGet.setHeader(header.getName(), header.getValue());
            }
        }

        //send the request
        final HttpResponse response = httpClient.execute(httpGet);
        final String ret = getResult(response);
        System.out.println(ret);
        if (null == ret || ret.isEmpty()) {
            return "";
        }
        //get the result
        final Map<String, String> retMap = parseResponse(ret);
        if (retMap.get("code").equals("0")) {
            System.out.println(retMap.get("data"));
            return retMap.get("data");
        }
        return "";
    }

    /**
     * 获取服务器信息
     *
     * @param openID
     * @return
     * @throws Exception
     */
    private static String queryDevice(final String openID, final String dn) throws Exception {
        System.out.println(openID);
        //set the URL and method
        final String queryURL = "/rest/openapi/server/device/detail";
        //set headers and parameters
        final List<BasicNameValuePair> headers = new ArrayList<BasicNameValuePair>();
        headers.add(new BasicNameValuePair("openid", openID));
        //set parameters
        final List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("dn", dn));

        //create a connection manager
        final X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        //create a SSL connection
        final SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { tm }, null);
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", port, socketFactory));

        //create a HttpClient to connect to the target host
        final HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager(schemeRegistry));

        //set the URL
        String url = "https://" + ip + ":" + port + queryURL;

        //set parameters
        if (null != parameters) {
            url += "?";
            boolean init = false;
            for (final BasicNameValuePair e : parameters) {
                if (!init) {
                    url += URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode(e.getValue(), "UTF-8");
                    init = true;
                } else {
                    url += "&" + URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode (e.getValue(), "UTF-8");
                }
            }
        }
        System.out.println(url);
        final HttpGet httpGet = new HttpGet(url);
        //set headers
        if (null != headers) {
            for (final BasicNameValuePair header : headers) {
                httpGet.setHeader(header.getName(), header.getValue());
            }
        }

        //send the request
        final HttpResponse response = httpClient.execute(httpGet);
        final String ret = getResult(response);
        System.out.println(ret);
        if (null == ret || ret.isEmpty()) {
            return "";
        }
        //get the result
        final Map<String, String> retMap = parseResponse(ret);
        if (retMap.get("code").equals("0")) {
            System.out.println(retMap.get("data"));
            return retMap.get("data");
        }
        return "";
    }


    /**
     * 查询所有的设备类别信息列表
     * @throws Exception
     */
    @RequestMapping("/queryNeCategoryTest")
    public static void queryNeCategoryTest(HttpServletResponse httpServletResponse) throws Exception {
        JSONObject jsonObject = new JSONObject();
        String openID = getOpenID();
        //set the URL and method
        final String openidURL = "/rest/openapi/necategory";
        String method = "GET";

        //set headers and parameters
        final List<BasicNameValuePair> headers = new ArrayList<BasicNameValuePair>();
        headers.add(new BasicNameValuePair("openid", openID));
        //set parameters
        final List<BasicNameValuePair> parameters = null;

        //create a connection manager
        final X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        //create a SSL connection
        final SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { tm }, null);
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", port, socketFactory));

        //create a HttpClient to connect to the target host
        final HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager(schemeRegistry));

        //set the URL
        String url = "https://" + ip + ":" + port + openidURL;

        //set parameters
        if (null != parameters) {
            url += "?";
            boolean init = false;
            for (final BasicNameValuePair e : parameters) {
                if (!init) {
                    url += URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode(e.getValue(), "UTF-8");
                    init = true;
                } else {
                    url += "&" + URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode (e.getValue(), "UTF-8");
                }
            }
        }
        System.out.println(url);
        final HttpGet httpGet = new HttpGet(url);
        //set headers
        if (null != headers) {
            for (final BasicNameValuePair header : headers) {
                httpGet.setHeader(header.getName(), header.getValue());
            }
        }

        //send the request
        final HttpResponse response = httpClient.execute(httpGet);
        final String ret = getResult(response);
        System.out.println(ret);
        jsonObject.put("result", ret);
        ResponseHelper.write(httpServletResponse, jsonObject);
    }

    /**
     * 查询预订条件的设备类型信息列表
     * @param httpServletResponse
     * @throws Exception
     */
    @RequestMapping("/queryNeTypeTest")
    public static void queryNeTypeTest(HttpServletResponse httpServletResponse) throws Exception {
        JSONObject jsonObject = new JSONObject();
        String openID = getOpenID();
        //set the URL and method
        final String openidURL = "/rest/openapi/netype";
        String method = "GET";
        //set headers and parameters
        final List<BasicNameValuePair> headers = new ArrayList<BasicNameValuePair>();
        headers.add(new BasicNameValuePair("openid", openID));
        //set parameters
        List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("category", ""));
        parameters.add(new BasicNameValuePair("start", ""));
        parameters.add(new BasicNameValuePair("size", ""));
        parameters.add(new BasicNameValuePair("orderby", ""));
        parameters.add(new BasicNameValuePair("desc", ""));

        //create a connection manager
        final X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        //create a SSL connection
        final SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { tm }, null);
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", port, socketFactory));

        //create a HttpClient to connect to the target host
        final HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager(schemeRegistry));

        //set the URL
        String url = "https://" + ip + ":" + port + openidURL;

        //set parameters
        if (null != parameters) {
            url += "?";
            boolean init = false;
            for (final BasicNameValuePair e : parameters) {
                if (!init) {
                    url += URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode(e.getValue(), "UTF-8");
                    init = true;
                } else {
                    url += "&" + URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode (e.getValue(), "UTF-8");
                }
            }
        }
        System.out.println(url);
        final HttpGet httpGet = new HttpGet(url);
        //set headers
        if (null != headers) {
            for (final BasicNameValuePair header : headers) {
                httpGet.setHeader(header.getName(), header.getValue());
            }
        }

        //send the request
        final HttpResponse response = httpClient.execute(httpGet);
        final String ret = getResult(response);
        System.out.println(ret);
        jsonObject.put("result", ret);
        ResponseHelper.write(httpServletResponse, jsonObject);
    }


    @RequestMapping("/queryNeDeviceTest")
    public static void queryNeDeviceTest(HttpServletResponse httpServletResponse) throws Exception {
        JSONObject jsonObject = new JSONObject();
        String openID = getOpenID();
        //set the URL and method
        final String openidURL = "/rest/openapi/nedevice";
        String method = "GET";
        //set headers and parameters
        final List<BasicNameValuePair> headers = new ArrayList<BasicNameValuePair>();
        headers.add(new BasicNameValuePair("openid", openID));
        //set parameters
        List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("necategory", ""));
        parameters.add(new BasicNameValuePair("netype", ""));
        parameters.add(new BasicNameValuePair("neip", ""));
        parameters.add(new BasicNameValuePair("nestate", ""));
        parameters.add(new BasicNameValuePair("start", ""));
        parameters.add(new BasicNameValuePair("size", "300"));
        parameters.add(new BasicNameValuePair("orderby", ""));
        parameters.add(new BasicNameValuePair("desc", ""));

        //create a connection manager
        final X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        //create a SSL connection
        final SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { tm }, null);
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", port, socketFactory));

        //create a HttpClient to connect to the target host
        final HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager(schemeRegistry));

        //set the URL
        String url = "https://" + ip + ":" + port + openidURL;

        //set parameters
        if (null != parameters) {
            url += "?";
            boolean init = false;
            for (final BasicNameValuePair e : parameters) {
                if (!init) {
                    url += URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode(e.getValue(), "UTF-8");
                    init = true;
                } else {
                    url += "&" + URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode (e.getValue(), "UTF-8");
                }
            }
        }
        System.out.println(url);
        final HttpGet httpGet = new HttpGet(url);
        //set headers
        if (null != headers) {
            for (final BasicNameValuePair header : headers) {
                httpGet.setHeader(header.getName(), header.getValue());
            }
        }

        //send the request
        final HttpResponse response = httpClient.execute(httpGet);
        final String ret = getResult(response);
        System.out.println(ret);
        jsonObject.put("result", ret);
        ResponseHelper.write(httpServletResponse, jsonObject);
    }

    /**
     * 查询预订条件的端口信息
     *
     * @param httpServletResponse
     * @throws Exception
     */
    @RequestMapping("/queryPortResTest")
    public static void queryPortResTest(HttpServletResponse httpServletResponse) throws Exception {
        JSONObject jsonObject = new JSONObject();
        String openID = getOpenID();
        //set the URL and method
        final String openidURL = "/rest/openapi/network/port";
        String method = "GET";
        //set headers and parameters
        final List<BasicNameValuePair> headers = new ArrayList<BasicNameValuePair>();
        headers.add(new BasicNameValuePair("openid", openID));
        //set parameters
        List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("necategory", ""));
        parameters.add(new BasicNameValuePair("netype", ""));
        parameters.add(new BasicNameValuePair("neip", ""));
        parameters.add(new BasicNameValuePair("nestate", ""));
        parameters.add(new BasicNameValuePair("start", ""));
        parameters.add(new BasicNameValuePair("size", "3000"));
        parameters.add(new BasicNameValuePair("orderby", ""));
        parameters.add(new BasicNameValuePair("desc", ""));

        //create a connection manager
        final X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        //create a SSL connection
        final SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { tm }, null);
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", port, socketFactory));

        //create a HttpClient to connect to the target host
        final HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager(schemeRegistry));

        //set the URL
        String url = "https://" + ip + ":" + port + openidURL;

        //set parameters
        if (null != parameters) {
            url += "?";
            boolean init = false;
            for (final BasicNameValuePair e : parameters) {
                if (!init) {
                    url += URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode(e.getValue(), "UTF-8");
                    init = true;
                } else {
                    url += "&" + URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode (e.getValue(), "UTF-8");
                }
            }
        }
        System.out.println(url);
        final HttpGet httpGet = new HttpGet(url);
        //set headers
        if (null != headers) {
            for (final BasicNameValuePair header : headers) {
                httpGet.setHeader(header.getName(), header.getValue());
            }
        }

        //send the request
        final HttpResponse response = httpClient.execute(httpGet);
        final String ret = getResult(response);
        System.out.println(ret);
        jsonObject.put("result", ret);
        ResponseHelper.write(httpServletResponse, jsonObject);
    }


    /**
     * get the result
     */
    private static String getResult(final HttpResponse response) throws IllegalStateException, IOException {
        if (200 != response.getStatusLine().getStatusCode()) {
            return "";
        }

        final InputStream is = response.getEntity().getContent();
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String ret = "";
        String line = "";
        while ((line = br.readLine()) != null) {
            if (!ret.isEmpty()) {
                ret += "\n";
            }
            ret += line;
        }
        return ret;
    }


    private static Map<String, String> parseResponse(final String input) {
        final Map<String, String> retMap = new HashMap<String, String>();
        final JSONObject jObject = JSONObject.parseObject(input);

        if (null == jObject) {
            return retMap;
        }

        if (null != jObject.get("code")) {
            final String i = jObject.get("code").toString();
            retMap.put("code", i);
        }
        if (null != jObject.get("data")) {
            final String data = jObject.get("data").toString();
            retMap.put("data", data);
        }
        if (null != jObject.get("description")) {
            final String des = jObject.get("description").toString();
            retMap.put("description", des);
        }
        if (null != jObject.get("result")) {
            final String res = jObject.get("result").toString();
            retMap.put("result", res);
        }
        return retMap;
    }

    @RequestMapping(value = "/getServerInfo")
    public String getServerInfo(String dn, HttpServletRequest request) throws Exception {
        return "redirect:/net/device/detail?dn=" + dn; //重定向到华为网管系统
    }

    @RequestMapping("/getDataGrid")
    public void  getDataGrid(HttpServletResponse response, String deviceName) throws Exception {
        JSONObject jsonObject = new JSONObject();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String s = "";
        CloseableHttpResponse httpResponse = null;
        String url = "http://10.1.2.243:31943/net/queryNeDeviceTest";
        Map<String, String> param = new HashMap<>();
        param.put("deviceName", deviceName);
        URIBuilder builder = new URIBuilder(url);
        if (param != null) {
            for (String key: param.keySet()) {
                builder.addParameter(key, param.get(key));
            }
        }
        URI uri = builder.build();

        HttpGet httpGet = new HttpGet(uri);
        httpResponse = httpClient.execute(httpGet);
        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            s = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
        }
        jsonObject = JSONObject.parseObject(s);
        ResponseHelper.write(response, jsonObject);
    }

    /**
     * 查询预定条件的设备ONU信息列表
     *
     * @param openId
     * @return
     */
    public static String queryOnu(final String openId) throws Exception {
        //set the URL
        final String queryNeURL = "/rest/openapi/enterprise/ponmgr/onu";
        //set headers and parameters
        final List<BasicNameValuePair> headers = new ArrayList<BasicNameValuePair>();
        headers.add(new BasicNameValuePair("openid", openId));
        final List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
 //       parameters.add(new BasicNameValuePair("oltname", "xxx"));

        //create a connection manager
        final X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        //create a SSL connection
        final SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] {tm}, null);
        final SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", port, socketFactory));

        //create a HttpClient to connect to the target host
        final HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager (schemeRegistry));

        //set the URL
        String url = "https://" + ip + ":" + port + queryNeURL;
        //set parameters
        if (null != parameters) {
            url += "?";
            boolean init = false;
            for (final BasicNameValuePair e : parameters) {
                if (!init) {
                    url += URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode(e.getValue(), "UTF-8");init = true;
                } else {
                    url += "&" + URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode (e.getValue(), "UTF-8");
                }
            }
        }
        final HttpGet httpGet = new HttpGet(url);
        //set headers
        if (null != headers) {
            for (final BasicNameValuePair header : headers) {
                httpGet.setHeader(header.getName(), header.getValue());
            }
        }

        //send the request
        final HttpResponse response = httpClient.execute(httpGet); final String ret = getResult(response);
        if (null == ret || ret.isEmpty()) {
            return "";
        }

        //get the result
        final Map<String, String> retMap = parseResponse(ret);
        if (retMap.get("code").equals("0")) {
            return retMap.get("data");
        }
        return "";
    }

    /**
     * 查询预定条件的设备ONU信息列表
     * @param response
     */
    @RequestMapping("/getOnu")
    public void getOnu(HttpServletResponse response)  {
        JSONObject jsonObject = new JSONObject();
        try {
            final String openID = getOpenID();
            if (null == openID || openID.isEmpty()) {
                jsonObject.put("state", false);
                ResponseHelper.write(response, jsonObject);
            }
            final String onus = queryOnu(openID);
            JSONArray jsonArray = JSONArray.parseArray(onus);
            jsonObject.put("result", jsonArray);
            jsonObject.put("state", true);
        } catch (Exception e) {
            jsonObject.put("state", false);
        }
        ResponseHelper.write(response, jsonObject);
    }


    public static String postGetAlarmTest(final String openId) throws Exception {
        //set the URL and method
        String queryNeURL = "/rest/openapi/alarm";
        String method = "GET";

        //set headers
        final List<BasicNameValuePair> headers = new ArrayList<BasicNameValuePair>();
        headers.add(new BasicNameValuePair("openid", openId));
        //set parameters
        final List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("severity", ""));
        parameters.add(new BasicNameValuePair("clearStatus", ""));
        parameters.add(new BasicNameValuePair("ackStatus", ""));
        parameters.add(new BasicNameValuePair("startTime", ""));
        parameters.add(new BasicNameValuePair("endTime", ""));
        parameters.add(new BasicNameValuePair("alarmName", ""));
        parameters.add(new BasicNameValuePair("alarmSource", ""));
        parameters.add(new BasicNameValuePair("location", ""));
        parameters.add(new BasicNameValuePair("pageSize", ""));
        parameters.add(new BasicNameValuePair("pageNo", ""));

        //create a connection manager
        final X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        //create a SSL connection
        final SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] {tm}, null);
        final SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", port, socketFactory));

        //create a HttpClient to connect to the target host
        final HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager (schemeRegistry));

        //set the URL
        String url = "https://" + ip + ":" + port + queryNeURL;
        //set parameters
        if (null != parameters) {
            url += "?";
            boolean init = false;
            for (final BasicNameValuePair e : parameters) {
                if (!init) {
                    url += URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode(e.getValue(), "UTF-8");init = true;
                } else {
                    url += "&" + URLEncoder.encode(e.getName(), "UTF-8") + "=" + URLEncoder.encode (e.getValue(), "UTF-8");
                }
            }
        }
        final HttpGet httpGet = new HttpGet(url);
        //set headers
        if (null != headers) {
            for (final BasicNameValuePair header : headers) {
                httpGet.setHeader(header.getName(), header.getValue());
            }
        }

        //send the request
        final HttpResponse response = httpClient.execute(httpGet);
        final String ret = getResult(response);
        if (null == ret || ret.isEmpty()) {
            return "";
        }

        //get the result
        final Map<String, String> retMap = parseResponse(ret);
        if (retMap.get("code").equals("0")) {
            return retMap.get("data");
        }
        return "";
    }

    /**
     * 查询预定条件的告警信息列表
     * @param response
     */
    @RequestMapping("/queryAlarm")
    public void queryAlarm(HttpServletResponse response) {
        JSONObject jsonObject = new JSONObject();
        try {
            final String openID = getOpenID();
            String alarmTest = postGetAlarmTest(openID);
            JSONArray jsonArray = JSONArray.parseArray(alarmTest);
            jsonObject.put("result", jsonArray);
            jsonObject.put("state", true);
        } catch (Exception e) {
            jsonObject.put("state", false);
        }
        ResponseHelper.write(response, jsonObject);
    }

}
