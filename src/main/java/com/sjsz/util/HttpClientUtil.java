package com.sjsz.util;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.SecureRandom;

/**
 * HTTPS 工具类
 * https的链接一定要进行SSL的验证或者过滤之后才能够访问。
 *
 * Created by Administrator on 2018/1/30.
 */
public class HttpClientUtil {

    /**
     * 处理https GET/POST请求
     *
     * @param requestUrl 请求地址
     * @param requestMethod 请求方法
     * @param outputStr 参数
     * @return
     */
    public static String httpsRequest(String requestUrl, String requestMethod, String outputStr) {
        StringBuffer buffer = null;
        try {
            //创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManager[] tm = {new MyX509TrustManager()};
            //初始化
            sslContext.init(null, tm, new SecureRandom());
            //获取SSLSocketFactory对象
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            URL url = new URL(requestUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod(requestMethod);
            //设置当前实例使用的SSLSocketFactory
            connection.setSSLSocketFactory(ssf);
            connection.connect();
            //往服务器端写内容
            if (null != outputStr) {
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(outputStr.getBytes("utf-8"));
                outputStream.close();
            }
            //读取服务器端返回的内容
            InputStream is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            buffer = new StringBuffer();
            String line = null;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }
}
