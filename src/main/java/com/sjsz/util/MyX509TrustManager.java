package com.sjsz.util;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * https是对链接加了安全证书SSL的，如果服务器中没有相关链接的SSL证书，它就不能够信任那个链接，也就不会访问到了。
 * 所以我们第一步是自定义一个信任管理器。自要实现自带的X509TrustManager接口就可以了。
 *
 * 可以看到里面的方法都是空的，当方法为空是默认为所有的链接都为安全，
 * 也就是所有的链接都能够访问到。当然这样有一定的安全风险，可以根据实际需要写入内容。
 *
 * Created by Administrator on 2018/1/30.
 */
public class MyX509TrustManager implements X509TrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
