package com.kercer.kernet.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.kercer.kercore.debug.KCLog;
import com.kercer.kernet.download.KCDownloadEngine;
import com.kercer.kernet.download.KCDownloadListener;
import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.KCHttpResponse;
import com.kercer.kernet.http.KCHttpResult;
import com.kercer.kernet.http.KCRequestQueue;
import com.kercer.kernet.http.KerNet;
import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCStatusLine;
import com.kercer.kernet.http.error.KCNetError;
import com.kercer.kernet.http.listener.KCHttpListener;
import com.kercer.kernet.http.request.KCStringRequest;
import com.kercer.kernet_example.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

public class MainActivity extends Activity
{
	private KCRequestQueue mRequestQueue;
    private KCDownloadEngine mDownloadEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRequestQueue = KerNet.newRequestQueue(this);
        mDownloadEngine = new KCDownloadEngine("Kernet", 3);



        String urlQQAPK = "http://gdown.baidu.com/data/wisegame/4f9b25fb0e093ac6/QQ_220.apk";
        String urlDek = "http://mob.jz-test.doumi.com/dek/html_1128141111.dek";

        download(urlQQAPK);

    }

    private String urlToPath(String aUrl)
    {
        String path = Environment.getExternalStorageDirectory().getPath() + "/kernet";
        if (!new File(path).exists())
        {
            new File(path).mkdir();
        }
        path += aUrl.substring(aUrl.lastIndexOf(File.separator), aUrl.length());
        return path;
    }

    public void download(String aUrl)
    {
        String path = urlToPath(aUrl);
        try {
            mDownloadEngine.startDownload(aUrl, path, new KCDownloadListener() {
                @Override
                public void onPrepare() {
                    KCLog.d("onPrepare");
                }

                @Override
                public void onReceiveFileLength(long downloadedBytes, long fileLength) {
                    KCLog.d("onReceiveFileLength:%s:%s", downloadedBytes, fileLength);
                }

                @Override
                public void onProgressUpdate(long downloadedBytes, long fileLength, int speed) {
                    KCLog.d("onProgressUpdate:%s:%s:%s", downloadedBytes, fileLength, speed);
                }

                @Override
                public void onComplete(long downloadedBytes, long fileLength, int totalTimeInSeconds) {
                    KCLog.d("onComplete:%s:%s:%s", downloadedBytes, fileLength, totalTimeInSeconds);
                }

                @Override
                public void onError(long downloadedBytes, Throwable e) {
                    KCLog.d("onError%s", downloadedBytes);
                    e.printStackTrace();
                }
            }, false, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void requestInQueue(String aUrl)
    {
        KCStringRequest request = new KCStringRequest(aUrl, new KCHttpResult.KCHttpResultListener<String>()
        {
            @Override
            public void onHttpResult(KCHttpResponse aResponse, String aResult) {
                // TODO Auto-generated method stub
                Log.i("kernet", aResult);
            }

        } , new KCHttpListener()
        {
            @Override
            public void onResponseHeaders(KCStatusLine aStatusLine, KCHeaderGroup aHeaderGroup) {

            }

            @Override
            public void onHttpError(KCNetError error)
            {
                // TODO Auto-generated method stub
                Log.i("kernet", "");
            }

            @Override
            public void onHttpComplete(KCHttpRequest<?> request, KCHttpResponse response)
            {
                // TODO Auto-generated method stub

            }
        });
        mRequestQueue.add(request);
    }

    public void requestRunner(String aUrl)
    {
        KCHttpRequest<?> request1 = new KCHttpRequest<Object>(KCHttpRequest.Method.GET, aUrl, new KCHttpListener()
        {
            @Override
            public void onResponseHeaders(KCStatusLine aStatusLine, KCHeaderGroup aHeaderGroup) {

            }

            @Override
            public void onHttpError(KCNetError error)
            {
                KCLog.e("");
            }

            @Override
            public void onHttpComplete(KCHttpRequest<?> request, KCHttpResponse response)
            {
                KCLog.e("");
            }
        }) {
        };
        KerNet.newRequestRunner(null).startAsyn(request1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
