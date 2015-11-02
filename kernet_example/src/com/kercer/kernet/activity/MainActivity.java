package com.kercer.kernet.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.kercer.kernet.http.KCHttpListener;
import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.KCHttpResponse;
import com.kercer.kernet.http.KCHttpResult;
import com.kercer.kernet.http.KCRequestQueue;
import com.kercer.kernet.http.KerNet;
import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCLog;
import com.kercer.kernet.http.error.KCNetError;
import com.kercer.kernet.http.request.KCStringRequest;
import com.kercer.kernet_example.R;

public class MainActivity extends Activity
{
	private KCRequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRequestQueue = KerNet.newRequestQueue(this);

        KCHttpRequest<?> request1 = new KCHttpRequest<Object>(KCHttpRequest.Method.GET, "http://www.baidu.com", new KCHttpListener()
        {
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
//        KerNet.newRequestRunner(this).startAsyn(request1);


        KCStringRequest request = new KCStringRequest("http://gdown.baidu.com/data/wisegame/4f9b25fb0e093ac6/QQ_220.apk", new KCHttpResult.KCHttpResultListener<String>()
		{
			@Override
			public void onHttpResult(String response)
			{
				// TODO Auto-generated method stub
				Log.i("kernet", response);
			}
		} , new KCHttpListener()
		{
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

        request.setOnProgressListener(new KCHttpListener.KCProgressListener()
		{

            @Override
            public void onResponseHeaders(KCHeaderGroup aHeaderGroup) {

            }

            @Override
			public void onProgress(long aCurrent, long aTotal)
			{
				KCLog.v(String.format("%d, %d", aCurrent, aTotal));
			}
		});


//        String url = "http://gdown.baidu.com/data/wisegame/4f9b25fb0e093ac6/QQ_220.apk";
//        String path = Environment.getExternalStorageDirectory().getPath()+"/0downloadtest.apk";
//        KCDownloadRequest downloadRequest = new KCDownloadRequest(url, path, new KCHttpListener()
//        {
//
//			@Override
//			public void onHttpError(KCNetError error)
//			{
//				// TODO Auto-generated method stub
//				KCLog.e("");
//			}
//
//			@Override
//			public void onHttpComplete(KCHttpRequest<?> request, KCHttpResponse response)
//			{
//				// TODO Auto-generated method stub
//				KCLog.e("");
//			}
//
//        });
//        downloadRequest.setOnProgressListener(new KCHttpListener.KCProgressListener()
//		{
//			@Override
//			public void onProgress(long aTransferredBytes, long aTotalSize)
//			{
//				// TODO Auto-generated method stub
//				KCLog.e("");
//			}
//		});
//
//        mRequestQueue.add(downloadRequest);
        mRequestQueue.add(request1);



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
