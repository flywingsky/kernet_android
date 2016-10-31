package com.kercer.kernet.http.ssl;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;


/**
 * Created by zihong on 2016/10/29.
 */
public final class KCSSLManager
{
    private static SSLSocketFactory mDefaultSslSocketFactory;
    private SSLSocketFactory mSslSocketFactory;

    private synchronized SSLSocketFactory getDefaultSSLSocketFactory()
    {
        if (mDefaultSslSocketFactory == null)
        {
            try
            {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, null);
                mDefaultSslSocketFactory = sslContext.getSocketFactory();
            }
            catch (GeneralSecurityException e)
            {
                throw new AssertionError(); // The system has no TLS. Just give up.
            }
        }
        return mDefaultSslSocketFactory;
    }

    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory)
    {
        this.mSslSocketFactory = sslSocketFactory;
    }

    public static KeyStore getKeyStore(String keyStorePath, String pwd) throws Exception
    {
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream is = new FileInputStream(keyStorePath);
        ks.load(is, pwd.toCharArray());
        is.close();
        return ks;
    }

    public static SSLContext getSSLContext(String keyStorePath, String pwd, String trustStorePath) throws Exception
    {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = getKeyStore(pwd, keyStorePath);
        keyManagerFactory.init(keyStore, pwd.toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore trustStore = getKeyStore(pwd, trustStorePath);
        trustManagerFactory.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }


    public void setSslSocketFactory(InputStream... cerInputStream)
    {
        try
        {
            CertificateFactory certificatefactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            int index = 0;
            for (InputStream is : cerInputStream)
            {
                X509Certificate cert = (X509Certificate) certificatefactory.generateCertificate(is);
                keyStore.setCertificateEntry("alias" + index++, cert);
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");//安全数据层

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());//信任证书管理工厂

            trustManagerFactory.init(keyStore);

            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

            mSslSocketFactory = sslContext.getSocketFactory();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            KCSSLManager.close(cerInputStream);
        }
    }

    /**
     *
     * @param cerPaths cerPaths
     */
    public void setSslSocketFactory(String... cerPaths)
    {
        FileInputStream[] cers = new FileInputStream[cerPaths.length];
        for (int i = 0; i < cerPaths.length; i++)
        {
            File file = new File(cerPaths[i]);
            if (file.exists())
            {
                try
                {
                    cers[i] = new FileInputStream(file);
                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
        }
        setSslSocketFactory(cers);
    }

    /**
     *
     * @param cerValues cerValues
     */
    public void setSslSocketFactoryAsString(String... cerValues)
    {
        ByteArrayInputStream[] cers = new ByteArrayInputStream[cerValues.length];
        for (int i = 0; i < cerValues.length; i++)
        {
            cers[i] = new ByteArrayInputStream(cerValues[i].getBytes());
        }
        setSslSocketFactory(cers);
    }

    public SSLSocketFactory getSslSocketFactory()
    {
        return mSslSocketFactory == null ? getDefaultSSLSocketFactory() : mSslSocketFactory;
    }


    public static void close(Closeable... closeables)
    {
        for (Closeable cb : closeables)
        {
            try
            {
                if (null == cb)
                {
                    continue;
                }
                cb.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}

