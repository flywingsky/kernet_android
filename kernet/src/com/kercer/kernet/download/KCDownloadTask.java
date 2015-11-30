package com.kercer.kernet.download;

import android.os.SystemClock;

import com.kercer.kercore.debug.KCLog;
import com.kercer.kercore.io.KCUtilIO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author zihong
 *
 * This class implements multithread download
 *
 * == Format of the .cfg file
 *
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | A |         |B|       C       |       D       |       E       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       F       |       G       |       H       |       I       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       J       |      ...      |      ...      |      ...      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * A(2 bytes): stores the total time spent downloading the file
 * B(1 byte): stores the number of threads used to download the file
 * C(8 bytes): stores file length(acquired from HTTP header Content-Length)
 * D(8 bytes): stores the end offset of the last requested chunk
 * E(8 bytes): stores the start offset for thread 1
 * F(8 bytes): stores the end offset for thread 1
 * G(8 bytes): stores the start offset for thread 2
 * H(8 bytes): stores the end offset for thread 2
 * I(8 bytes): stores the start offset for thread 3
 * J(8 bytes): stores the end offset for thread 3
 *
 */
public class KCDownloadTask
{
	private KCDownloadEngine mDownloadEngine;
	private URL mOrigUrl;
	private URL mUrl;
	private KCDownloadListener mNotifier;
	private KCDownloadProgressUpdater mDownloadProgressUpdater;

	private KCDownloadConfig mDownloadConfig = new KCDownloadConfig();

	private String mDestFilePath;
	private File mConfigFile;
	private FileChannel mConfigFileChannel;
	private LongBuffer mConfigHeaderBuffer;
	private ByteBuffer mConfigMetaBuffer; // the first 8 bytes of the config file
	private long mLastChunkEndOffset;

	private long mFileLength = -1;
	private long mDownloadedBytes;


	private List<KCDownloadWorker> mWorkerList = new ArrayList<KCDownloadWorker>();
	private List<Boolean> mTaskStoppedStateList = new ArrayList<Boolean>(1);
	private volatile int mGlobalTaskRunSerialNo = -1;
	private final Object mControlTaskLock = new Object();

	// buffer objects(byte[]) that can be reused respectively by each thread
	private static ThreadLocal<byte[]> mBufferPool = new ThreadLocal<byte[]>();

	private boolean mAborted = false;
	private boolean mPreparingToRun = true;
	private boolean mRunning;
	// set to true if the caller calls stop() before the task is started.
	private boolean mEarlyStopped;

	private long mStartDownloadTimestamp;


	public KCDownloadTask(KCDownloadEngine aEngine, URL aUrl, String aDestFilePath) throws FileNotFoundException
	{
		this(aEngine, aUrl, aDestFilePath, null, null);
	}

	public KCDownloadTask(KCDownloadEngine aDownloadEngine, URL aUrl, String aDestFilePath, KCDownloadListener aNotifier) throws FileNotFoundException
	{
		this(aDownloadEngine, aUrl, aDestFilePath, aNotifier, null);
	}


	public KCDownloadTask(KCDownloadEngine aDownloadEngine, URL aUrl, String aDestFilePath, KCDownloadListener aNotifier, KCDownloadConfig aDownloadConfig) throws FileNotFoundException
	{
		mDownloadEngine = aDownloadEngine;
		mOrigUrl = mUrl = aUrl; // keep the original url
		mDestFilePath = aDestFilePath;
		mNotifier = aNotifier;
		if (aDownloadConfig == null)
			mDownloadConfig = new KCDownloadConfig();
		else
			mDownloadConfig = aDownloadConfig;
	}

	public KCDownloadListener getDownloadNotifier()
	{
		return mNotifier;
	}

	public void runTask(final boolean useResumable, final boolean needProgress)
	{
		// synchrnozized to ensure mGlobalTaskRunSerialNo is accessed atomically
		synchronized (mControlTaskLock)
		{
			if (mEarlyStopped)
			{
				mEarlyStopped = false;
				return;
			}

			KCDownloadWorker worker = null;
			try
			{
				if (mNotifier != null)
					mNotifier.onPrepare();

				// if resumable download is required, a config file should be created to keep the configurations
				if (useResumable)
				{
					initConfigFileBuffer();
					verifyLastChunkEndOffset();
				}

				mTaskStoppedStateList.add(++mGlobalTaskRunSerialNo, Boolean.FALSE);
				worker = new KCDownloadWorker(0, mGlobalTaskRunSerialNo);
				mWorkerList.add(worker);
				worker.initRequest(true, useResumable);

				mStartDownloadTimestamp = System.currentTimeMillis();

				// is a download progress meter needed?
				if (needProgress)
				{
					mDownloadProgressUpdater = new KCDownloadProgressUpdater();
					if (mNotifier != null)
						mDownloadProgressUpdater.start();
				}

				mPreparingToRun = false;
				mRunning = true;
			}
			catch (Exception e)
			{
				if (KCLog.DEBUG)
					e.printStackTrace();
				reportError(worker, e);
			}
		}
	}



	@SuppressWarnings("resource")
	private void initConfigFileBuffer() throws IOException
	{
		mConfigFile = new File(mDestFilePath + ".cfg");
		mConfigFileChannel = new RandomAccessFile(mConfigFile, "rw").getChannel();
		// the length of the config file is fixed
		int configFileSize = (Long.SIZE / 8 * KCDownloadConfig.CONFIG_HEADER_SIZE) + (mDownloadConfig.getThreadCountPerTask() * KCDownloadConfig.CONFIG_BYTE_OFFSET_SLOT_COUNT * (Long.SIZE / 8));
		mConfigHeaderBuffer = mConfigFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, configFileSize).asLongBuffer();
		mConfigMetaBuffer = mConfigFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, Long.SIZE / 8);
	}

	private void verifyLastChunkEndOffset()
	{
		long lastChunkEndOffset = mConfigHeaderBuffer.get(KCDownloadConfig.CONFIG_LAST_CHUNK_OFFSET_INDEX);
		if (lastChunkEndOffset > 0)
		{
			long maxChunkEndOffset = 0;
			int threadCount = (mConfigMetaBuffer.get(KCDownloadConfig.CONFIG_META_THREAD_COUNT_INDEX) & 0xff);
			for (int i = 0; i < threadCount; ++i)
			{
				long chunkEndOffset = getEndOffset(i);
				if (maxChunkEndOffset < chunkEndOffset)
					maxChunkEndOffset = chunkEndOffset;
			}

			if (maxChunkEndOffset != lastChunkEndOffset)
				mConfigHeaderBuffer.put(KCDownloadConfig.CONFIG_LAST_CHUNK_OFFSET_INDEX, maxChunkEndOffset);
		}
	}

	private boolean mDone;

	private synchronized void onComplete(boolean force)
	{
		if (!force && (mDone || mDownloadedBytes != mFileLength))
			return;

		stopDownloadProgressAndSpeedUpdater();

		if (mNotifier != null)
		{
			try
			{
				saveTotalDownloadTime();

				mNotifier.onComplete(mDownloadedBytes, mFileLength, getTotalDownloadTime());
			}
			catch (Exception e)
			{
				if (KCLog.DEBUG)
					e.printStackTrace();
			}
		}

		KCUtilIO.closeSilently(mConfigFileChannel);
		if (mConfigFile != null && mConfigFile.exists())
			mConfigFile.delete();
		mDone = true;
		mRunning = false;
	}

	public void cancel()
	{
		stop();
	}

	//you can delete ".cfg"
	public void stop()
	{
		synchronized (mControlTaskLock)
		{
			if (mPreparingToRun)
			{ // the task is not yet started
				// so we mark it as "early-stopped"
				mEarlyStopped = true;
				return;
			}

			if (!mRunning)
				return;

			saveTotalDownloadTime();

			stopDownloadProgressAndSpeedUpdater();

			mTaskStoppedStateList.set(mGlobalTaskRunSerialNo, Boolean.TRUE);

			mAborted = true;
			// disconnect all HTTP connections for the current task
			for (int i = 0; i < mWorkerList.size(); ++i)
			{
				HttpURLConnection conn = mWorkerList.get(i).mHttpConn;
				if (conn != null)
					try{
						conn.disconnect();
					}catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}

			}

			KCUtilIO.closeSilently(mConfigFileChannel);

			mRunning = false;
		}
	}

	private void saveTotalDownloadTime()
	{
		if (mStartDownloadTimestamp > 0)
		{
			int downloadTimeSpan = (int) ((System.currentTimeMillis() - mStartDownloadTimestamp) / 1000);
			if (downloadTimeSpan > 0)
			{
				setTotalDownloadTime(getTotalDownloadTime() + downloadTimeSpan);
			}
			mStartDownloadTimestamp = 0;
		}
	}

	private void stopDownloadProgressAndSpeedUpdater()
	{
		if (mDownloadProgressUpdater != null)
		{
			mDownloadProgressUpdater.stopLoop();
			mDownloadProgressUpdater.interrupt();
			mDownloadProgressUpdater = null;
		}
	}

	public void resume(final boolean useResumable)
	{
		synchronized (mControlTaskLock)
		{
			if (mRunning)
				return;

			mAborted = false;
			mPreparingToRun = true;

			mDownloadedBytes = 0;
			mWorkerList.clear();
			mUrl = mOrigUrl;

			mDownloadEngine.getExecutorService().execute(new Runnable()
			{
				@Override
				public void run()
				{
					runTask(useResumable, true);
				}
			});
		}
	}

	private void reportError(KCDownloadWorker worker, Throwable e)
	{
		// we only need to stop the task ONCE, so we have to check if the task
		// is already stopped
		if (worker == null || !worker.isStopped())
		{
			if (worker != null)
				stop();

			if (mNotifier != null)
				mNotifier.onError(mDownloadedBytes, e);
		}
	}

	public boolean isStopped()
	{
		synchronized (mControlTaskLock)
		{
			return mTaskStoppedStateList.get(mGlobalTaskRunSerialNo);
		}
	}

	private static byte[] getBuffer()
	{
		byte[] buffer = mBufferPool.get();
		if (buffer == null)
		{
			buffer = new byte[KCDownloadConfig.BUFFER_SIZE];
			mBufferPool.set(buffer);
		}
		return buffer;
	}

	/**
	 * this method requests next chunk of bytes to download, it is synchronized because it will be called concurrently by multiple threads
	 *
	 * @return true if chunks of the file are not yet fully consumed
	 **/
	private synchronized boolean requestNextChunk(int threadIndex)
	{
		if (mLastChunkEndOffset < mFileLength)
		{
			int blockSize = (int) Math.min(mFileLength - mLastChunkEndOffset, KCDownloadConfig.REQUEST_CHUNK_SIZE);

			// index = 3 + threadIndex * 2
			// example:
			// if threadIndex = 1, index = 3 + 1 * 2, then the slot to store the
			// start offset of the chunk is 5, end offset of the chunk is 6
			int index = KCDownloadConfig.CONFIG_HEADER_SIZE + (threadIndex * KCDownloadConfig.CONFIG_BYTE_OFFSET_SLOT_COUNT);
			mConfigHeaderBuffer.put(index, mLastChunkEndOffset); // mutable startOffset, increased as downloaded bytes being increased
			mConfigHeaderBuffer.put(index + 1, mLastChunkEndOffset + blockSize); // endOffset

			// update mLastChunkEndOffset and persist it in the config file
			mLastChunkEndOffset += blockSize;
			mConfigHeaderBuffer.put(KCDownloadConfig.CONFIG_LAST_CHUNK_OFFSET_INDEX, mLastChunkEndOffset);

			return true;
		}
		return false;
	}

	// use 2 bytes to keep the time
	private void setTotalDownloadTime(int timeInSeconds)
	{
		mConfigMetaBuffer.put(KCDownloadConfig.CONFIG_META_DOWNLOAD_TIME_INDEX, (byte) (timeInSeconds >> 8));
		mConfigMetaBuffer.put(KCDownloadConfig.CONFIG_META_DOWNLOAD_TIME_INDEX + 1, (byte) timeInSeconds);
	}

	// use 2 bytes to keep the time
	private int getTotalDownloadTime()
	{
		return ((mConfigMetaBuffer.get(KCDownloadConfig.CONFIG_META_DOWNLOAD_TIME_INDEX) << 8) & 0xff00) | (mConfigMetaBuffer.get(KCDownloadConfig.CONFIG_META_DOWNLOAD_TIME_INDEX + 1) & 0xff);
	}

	private void setThreadCount(int count)
	{
		mConfigMetaBuffer.put(KCDownloadConfig.CONFIG_META_THREAD_COUNT_INDEX, (byte) count);
	}

	public int getThreadCount()
	{
		return mConfigMetaBuffer.get(KCDownloadConfig.CONFIG_META_THREAD_COUNT_INDEX) & 0xff;
	}

	private void setStartOffset(int threadIndex, long offset)
	{
		int index = KCDownloadConfig.CONFIG_HEADER_SIZE + (threadIndex * KCDownloadConfig.CONFIG_BYTE_OFFSET_SLOT_COUNT);
		mConfigHeaderBuffer.put(index, offset);
	}

	private void setEndOffset(int threadIndex, long offset)
	{
		int index = KCDownloadConfig.CONFIG_HEADER_SIZE + (threadIndex * KCDownloadConfig.CONFIG_BYTE_OFFSET_SLOT_COUNT) + 1;
		mConfigHeaderBuffer.put(index, offset);
	}

	private long getStartOffset(int threadIndex)
	{
		int index = KCDownloadConfig.CONFIG_HEADER_SIZE + (threadIndex * KCDownloadConfig.CONFIG_BYTE_OFFSET_SLOT_COUNT);
		return mConfigHeaderBuffer.get(index);
	}

	private long getEndOffset(int threadIndex)
	{
		int index = KCDownloadConfig.CONFIG_HEADER_SIZE + (threadIndex * KCDownloadConfig.CONFIG_BYTE_OFFSET_SLOT_COUNT) + 1;
		return mConfigHeaderBuffer.get(index);
	}

	class KCDownloadWorker
	{


		private int mRetryWaitMilliseconds = KCDownloadConfig.INITIAL_RETRY_WAIT_TIME;
		private int mCurRetryCount;

		private HttpURLConnection mHttpConn;

		private long mStartByteOffset;
		private long mEndByteOffset;
		private int mThreadIndex;

		private boolean mInitConfig;
		private RandomAccessFile mDestRandomAccessFile;

		private int mCurSerialNo;

		private boolean mPartialContentExpected;

		private KCDownloadWorker(int threadIndex, int serialNo) throws Exception
		{
			mThreadIndex = threadIndex;
			mCurSerialNo = serialNo;

			// we once used multithread to download the current task, so we should
			// continue using multithread and expect partial content
			mPartialContentExpected = getThreadCount() > 1;
		}

		public boolean isStopped()
		{
			return mTaskStoppedStateList.get(mCurSerialNo);
		}

		private void initRequest(final boolean init, final boolean useResumable)
		{
			if (isStopped())
				return;

			mDownloadEngine.getExecutorService().execute(new Runnable()
			{
				@Override
				public void run()
				{
					while (true)
					{
						try
						{
							while (true)
							{
								if (KCLog.DEBUG)
									KCLog.d(">>>>DT download retry: " + mCurRetryCount + ", url: " + mUrl.toString());
								if (!executeRequest(useResumable))
								{
									mFileLength = mConfigHeaderBuffer.get(KCDownloadConfig.CONFIG_FILE_LENGTH_INDEX);
									mDownloadedBytes = mFileLength;
									onComplete(true);
									break;
								}
								else
								{
									int statusCode = mHttpConn.getResponseCode();
									if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP || statusCode == HttpURLConnection.HTTP_MOVED_PERM)
									{
										mUrl = new URL(mHttpConn.getHeaderField("Location"));
										if (KCLog.DEBUG)
											KCLog.d(">>>>DT redirecting to: " + mUrl.toString());
									}
									else
									{
										if (KCLog.DEBUG)
											KCLog.d(">>>>DT status: " + statusCode + ", expect_partial_content: " + mPartialContentExpected);
										if (statusCode != HttpURLConnection.HTTP_PARTIAL)
										{
											// we are expecting partial content, while the status code is not 206
											// so we should retry the connection with the original url
											if (mPartialContentExpected)
											{
												mUrl = mOrigUrl;
												throw new KCWrongStatusCodeException();
											}

											// the status code is 200, while the Content-Length is 0, this is not 'scientific'
											// so again, we should retry the connection with the original url if we haven't yet
											// reach MAX_RETRY_COUNT
											if (statusCode == HttpURLConnection.HTTP_OK && !initContentLength())
											{
												// if Content-Length is absent or equal to 0
												// we should retry the original URL
												mUrl = mOrigUrl;
												if (retry())
												{
													continue;
												}
												else
												{ // we reach MAX_RETRY_COUNT
													reportError(KCDownloadWorker.this, new KCZeroContentLengthException());
													return;
												}
											}
										}

										if (KCLog.DEBUG)
											KCLog.d(">>>>DT thread: " + Thread.currentThread().getName() + ", final url: " + mUrl.toString());
										break;
									}
								}
							}

							handleResponse();
							break;
						}
						catch (KCUnexpectedStatusCodeException e)
						{
							reportError(KCDownloadWorker.this, e);
							break;
						}
						catch (Throwable e)
						{
							if (KCLog.DEBUG)
								e.printStackTrace();

							if (e instanceof IOException)
							{
								String msg = e.getMessage();
								if (msg != null)
								{
									msg = msg.toLowerCase(Locale.getDefault());
									// oh shit, no free disk space is left
									if (msg.contains("enospc") || msg.contains("no space"))
									{
										reportError(KCDownloadWorker.this, e);
										break;
									}
								}
							}
							if (e instanceof KCWrongStatusCodeException)
							{
								// WrongStatusCodeException, which means the remote server initially
								// told us that it supports resumable download, while later it returned
								// 200 instead of 206, so we will retry at most MAX_RETRY_COUNT times
								// and see if the remote server will return correct status code
								mPartialContentExpected = true;
							}

							if (!mAborted)
							{ // if the task is NOT manually aborted
								if (!retry())
								{ // if we still do NOT reach MAX_RETRY_COUNT

									if (e instanceof KCWrongStatusCodeException)
									{
										reportError(KCDownloadWorker.this, e);
									}
									else
									{
										Exception reachMaxRetryException = new KCReachMaxRetryException(e);
										reportError(KCDownloadWorker.this, reachMaxRetryException);
									}
									break;
								}
							}
							else
							{
								break;
							}
						}
					}

					if (KCLog.DEBUG)
						KCLog.d(">>>> delete FD: " + (mDestRandomAccessFile != null) + ", " + Thread.currentThread().getName());
					// ensures that the file descriptor is correctly closed.
					KCUtilIO.closeSilently(mDestRandomAccessFile);
				}

				private void handleResponse() throws Exception
				{
					int statusCode = mHttpConn.getResponseCode();
					if (statusCode == HttpURLConnection.HTTP_OK)
					{
						// oh no, cannot use multithread
						readFullContent();
					}
					else if (statusCode == HttpURLConnection.HTTP_PARTIAL)
					{
						// cool, use multithread
						handlePartialContent(init && !mInitConfig);
					}
					else if (statusCode >= 400 && statusCode < 500)
					{
						if (KCLog.DEBUG)
							KCLog.d("unexpected status code, URL: " + mUrl.toString());
						// client error? probably because of bad URL
						throw new KCUnexpectedStatusCodeException();
					}
					else if (statusCode != 416)
					{ // REQUESTED_RANGE_NOT_SATISFIABLE = 416
						// this rarely happens, but according to the RFC, we should assume it would happen
						throw new IOException(statusCode + " " + mHttpConn.getResponseMessage());
					}
				}

				private boolean retry()
				{
					// if the request is not manually aborted by the user, it is most probably
					// because of network error, so we have to retry~
					if (++mCurRetryCount <= KCDownloadConfig.MAX_RETRY_COUNT)
					{
						mRetryWaitMilliseconds = Math.min(KCDownloadConfig.MAX_RETRY_WAIT_TIME, mRetryWaitMilliseconds * 2);
						SystemClock.sleep(mRetryWaitMilliseconds);
						// luohh add for test
						// SystemClock.sleep(2 * 1000);
						return true;
					}

					return false;
				}
			});
		}

		private boolean executeRequest(final boolean useResumable) throws IOException
		{
			if (useResumable)
			{
				mStartByteOffset = getStartOffset(mThreadIndex);
				mEndByteOffset = getEndOffset(mThreadIndex);
				// correct the startByteOffset if it was accidentally set to a number
				// larger than the endByteOffset(currently I have no idea what would cause this)
				if (mStartByteOffset > mEndByteOffset)
				{
					mStartByteOffset = mEndByteOffset - KCDownloadConfig.REQUEST_CHUNK_SIZE;
					if (mStartByteOffset < 0)
						mStartByteOffset = 0;
				}

				if (mEndByteOffset == 0)
				{
					// mEndByteOffset is 0, which means that this is the first request we ever make for the current DownloadTask
					mEndByteOffset = KCDownloadConfig.INITIAL_REQUEST_CHUNK_SIZE;
				}
				else if (mStartByteOffset == mEndByteOffset)
				{
					// if the chunk for the current thread was *just* finished, we will try
					// to request a new chunk, if it does not succeed, it means we already
					// reach the end of the file, and we will keep going and see if the next
					// thread has already finished its last chunk (the 'else' part)
					if (requestNextChunk(mThreadIndex))
					{
						mStartByteOffset = getStartOffset(mThreadIndex);
						mEndByteOffset = getEndOffset(mThreadIndex);
					}
					else
					{
						// the current thread already finishes its share of chunks and we reach the end of the file
						// so we shift to the next thread, see the comment in startWorkers()
						++mThreadIndex;
						if (mThreadIndex >= getThreadCount())
							return false;

						return executeRequest(true);
					}
				}

				if (mDestRandomAccessFile == null)
					mDestRandomAccessFile = new RandomAccessFile(mDestFilePath, "rw");

				mDestRandomAccessFile.seek(mStartByteOffset);

				mHttpConn = (HttpURLConnection) mUrl.openConnection();
				mHttpConn.addRequestProperty("Range", "bytes=" + mStartByteOffset + "-" + (mEndByteOffset - 1));
			}
			else
			{
				mHttpConn = (HttpURLConnection) mUrl.openConnection();
			}
			// we are not expecting gzip. just give the raw bytes.
			mHttpConn.setRequestProperty("Accept-Encoding", "identity");
			mHttpConn.setConnectTimeout(15000);
			mHttpConn.setReadTimeout(15000);

			// luohh add for test
			// mHttpConn.setConnectTimeout(2 * 1000);
			// mHttpConn.setReadTimeout(2 * 1000);

			mHttpConn.setRequestMethod("GET");
			return true;
		}

		private void handlePartialContent(boolean init) throws Exception
		{
			if (init)
			{
				mPartialContentExpected = true;

				readContentLength();

				if (mFileLength > 0)
				{
					int threadCount = initConfig();
					if (mNotifier != null)
						mNotifier.onReceiveFileLength(mDownloadedBytes, mFileLength);
					startWorkers(threadCount);
				}
				else
				{
					// not possible to reach here according to RFC-2616(http://tools.ietf.org/html/rfc2616#page-122)
					// because if the status code is 206(SC_PARTIAL_CONTENT), Content-Range will not be absent,
					// thus mFileLength is not possible to be less than or equal to 0
					readFullContent();
					return;
				}
			}

			if (isStopped())
				return;

			readPartialContent();
		}

		private void readContentLength()
		{
			try
			{
				String contentRange = mHttpConn.getHeaderField("Content-Range");
				mFileLength = Long.parseLong(contentRange.substring(contentRange.lastIndexOf('/') + 1));
			}
			catch (Exception e)
			{
				if (KCLog.DEBUG)
					e.printStackTrace();
			}
		}

		private int initConfig()
		{
			int threadCount = getThreadCount();
			long savedFileLength = mConfigHeaderBuffer.get(KCDownloadConfig.CONFIG_FILE_LENGTH_INDEX);

			if (threadCount == 0 || (savedFileLength != 0 && savedFileLength != mFileLength))
			{
				threadCount = mDownloadConfig.getThreadCountPerTask();
				while (threadCount > 1 && mFileLength / threadCount < KCDownloadConfig.INITIAL_REQUEST_CHUNK_SIZE)
				{
					// if the file is too small, we don't need that many threads
					--threadCount;
				}

				// the preset mEndByteOffset might be bigger than the actual Content-Length(if the file is smaller than INITIAL_REQUEST_CHUNK_SIZE)
				if (mEndByteOffset > mFileLength)
					mEndByteOffset = mFileLength;
				setEndOffset(mThreadIndex, mEndByteOffset);
				// update mLastChunkEndOffset(the currently biggest offset we reached.)
				mLastChunkEndOffset = mEndByteOffset;
				mConfigHeaderBuffer.put(KCDownloadConfig.CONFIG_LAST_CHUNK_OFFSET_INDEX, mLastChunkEndOffset);

				// persis fileLength and threadCount in the config file
				mConfigHeaderBuffer.put(KCDownloadConfig.CONFIG_FILE_LENGTH_INDEX, mFileLength);
				setThreadCount(threadCount);
			}
			else
			{
				// init current download offset
				mLastChunkEndOffset = mConfigHeaderBuffer.get(KCDownloadConfig.CONFIG_LAST_CHUNK_OFFSET_INDEX);
				mDownloadedBytes = mLastChunkEndOffset;
				// the following LOOP ensures that downloadedBytes is accurate!
				for (int i = mThreadIndex; i < threadCount; ++i)
				{
					// why we minus mDownloadedBytes this way? see 'requestNextChunk()'
					mDownloadedBytes -= (getEndOffset(i) - getStartOffset(i));
				}
			}

			mInitConfig = true;
			return threadCount;
		}

		private void startWorkers(int threadCount) throws Exception
		{
			// The reason that we add up 'mThreadIndex' for 'i' is because we almost finish downloading
			// the entire file, the thread represented by 'mThreadIndex' cannot request more chunks to download
			// so we will not need to start a worker for it, and this is why there is a '++mThreadIndex' in executeRequest()

			// The reason 'i' starts from '1' is that the current tbread is already running, which should
			// not be counted in this loop
			for (int i = mThreadIndex + 1; i < threadCount; ++i)
			{
				long startOffset = getStartOffset(i);
				long endOffset = getEndOffset(i);
				if (startOffset < endOffset || (startOffset == endOffset && requestNextChunk(i)))
				{
					KCDownloadWorker worker = new KCDownloadWorker(i, mCurSerialNo);
					mWorkerList.add(worker);
					worker.initRequest(false, true);
				}
				else
				{
					break;
				}
			}
		}

		private void readPartialContent() throws Exception
		{
			mCurRetryCount = 0;
			mRetryWaitMilliseconds = KCDownloadConfig.INITIAL_RETRY_WAIT_TIME;

			InputStream is = null;

			byte[] buffer = getBuffer();
			while (true)
			{
				int blockSize = 0;
				try
				{
					checkStatusCodeForPartialContent();
					is = mHttpConn.getInputStream();

					int len = 1;
					while (len > 0)
					{
						len = is.read(buffer, blockSize, KCDownloadConfig.BUFFER_SIZE - blockSize);
						if (len > 0)
						{
							blockSize += len;
							// ensures the buffer is filled to avoid making IO busy
							if (blockSize < KCDownloadConfig.BUFFER_SIZE)
								continue;
						}

						mDestRandomAccessFile.write(buffer, 0, blockSize);

						syncReadBytes(blockSize);
						// propagate the download progress
						if (mDownloadProgressUpdater != null)
							mDownloadProgressUpdater.onProgressUpdate(blockSize);
						blockSize = 0;
					}
				}
				catch (Exception e)
				{
					if (blockSize > 0)
					{
						mDestRandomAccessFile.write(buffer, 0, blockSize);
						syncReadBytes(blockSize);
						if (mDownloadProgressUpdater != null)
							mDownloadProgressUpdater.onProgressUpdate(blockSize);
					}

					throw e;
				}
				finally
				{
					KCUtilIO.closeSilently(is);
					mHttpConn.disconnect();
				}

				// already downloaded the last chunk or the task was stopped
				boolean downloadedLastChunk = mStartByteOffset >= mEndByteOffset && !requestNextChunk(mThreadIndex);
				if (downloadedLastChunk || isStopped())
				{
					if (downloadedLastChunk)
						onComplete(false);
					break;
				}
				else
				{
					executeRequest(true);
				}
			}
		}

		private void checkStatusCodeForPartialContent() throws IOException
		{
			long ts = System.currentTimeMillis();
			if (KCLog.DEBUG)
				KCLog.d(">>>>DT HTTP REQUEST starts" + Thread.currentThread().getName());

			if (mHttpConn.getResponseCode() != HttpURLConnection.HTTP_PARTIAL)
			{
				if (KCLog.DEBUG)
					KCLog.d(">>>> checkStatusCodeForPartialContent: " + Thread.currentThread().getName() + ", wrong status code: " + mHttpConn.getResponseCode());
				throw new KCWrongStatusCodeException();
			}
			if (KCLog.DEBUG)
				KCLog.d(">>>>DT HTTP REQUEST ends: " + (System.currentTimeMillis() - ts) + ", " + Thread.currentThread().getName());
		}

		// persist the downloaded byte count in the config file
		private void syncReadBytes(int total) throws IOException
		{
			mStartByteOffset += total;
			setStartOffset(mThreadIndex, mStartByteOffset);
		}

		// for single thread download
		private void readFullContent() throws IOException
		{
			mCurRetryCount = 0;
			mRetryWaitMilliseconds = KCDownloadConfig.INITIAL_RETRY_WAIT_TIME;

			if (mNotifier != null)
				mNotifier.onReceiveFileLength(0, mFileLength);
			mDownloadedBytes = 0;

			FileOutputStream fos = new FileOutputStream(mDestFilePath);

			InputStream is = null;

			byte[] buffer = getBuffer();
			int blockSize = 0;
			try
			{
				is = mHttpConn.getInputStream();

				int len = 1;
				while (len > 0)
				{
					len = is.read(buffer, blockSize, KCDownloadConfig.BUFFER_SIZE - blockSize);
					if (len > 0)
					{
						blockSize += len;
						if (blockSize < KCDownloadConfig.BUFFER_SIZE)
							continue;
					}

					fos.write(buffer, 0, blockSize);
					if (mDownloadProgressUpdater != null)
						mDownloadProgressUpdater.onProgressUpdate(blockSize);
					blockSize = 0;
				}

				onComplete(true);
			}
			catch (Exception e)
			{
				if (blockSize > 0)
				{
					fos.write(buffer, 0, blockSize);
					if (mDownloadProgressUpdater != null)
						mDownloadProgressUpdater.onProgressUpdate(blockSize);
				}
			}
			finally
			{
				KCUtilIO.closeSilently(is);
				KCUtilIO.closeSilently(fos);
				mHttpConn.disconnect();
			}
		}

		private boolean initContentLength()
		{
			mFileLength = mHttpConn.getContentLength();

			return mFileLength > 0;
		}
	}

	class KCUnexpectedStatusCodeException extends RuntimeException
	{
		private static final long serialVersionUID = -6537360708511199076L;
	}

	public static class KCReachMaxRetryException extends RuntimeException
	{
		private static final long serialVersionUID = 8493035725274498348L;

		public KCReachMaxRetryException(Throwable e)
		{
			super(e);
		}
	}

	public static class KCWrongStatusCodeException extends RuntimeException
	{
		private static final long serialVersionUID = 1993527299811166227L;
	}

	public static class KCZeroContentLengthException extends RuntimeException
	{
		private static final long serialVersionUID = 178268877309938933L;
	}

	class KCDownloadProgressUpdater extends Thread
	{

		private boolean running = true;
		private long downloadedByteSampleArr[] = new long[getMaxDownloadedByteArrIndex() + 1];
		private int slotIndex;

		public int getMaxDownloadedByteArrIndex()
		{
			return mDownloadConfig.getDownloadSpeedSamplingTimeSpan() / mDownloadConfig.getUpdateProgressInterval() -1;
		}

		void stopLoop()
		{
			running = false;
		}

		@Override
		public void run()
		{
			Thread.currentThread().setPriority(MIN_PRIORITY);

			long startTimestamp = System.currentTimeMillis();
			while (running)
			{
				try
				{
					Thread.sleep(mDownloadConfig.getUpdateProgressInterval());
					// we are not ready to propagate the progress, cause we currently haven't
					// downloaded any bytes, the mDownloadedBytes instance variable is not yet
					// initialized, which is done in DownloadWorker.initConfig()
					if (mDownloadedBytes <= 0)
						continue;

					int speed;
					if (slotIndex == getMaxDownloadedByteArrIndex())
					{
						long totalRead = 0;
						synchronized (this)
						{
							for (int i = 0; i < getMaxDownloadedByteArrIndex(); ++i)
							{
								totalRead += downloadedByteSampleArr[i];
								downloadedByteSampleArr[i] = downloadedByteSampleArr[i + 1];
							}
							totalRead += downloadedByteSampleArr[getMaxDownloadedByteArrIndex()];
							downloadedByteSampleArr[getMaxDownloadedByteArrIndex()] = 0;
						}

						speed = (int) (totalRead * 1000 / mDownloadConfig.getDownloadSpeedSamplingTimeSpan());
					}
					else
					{
						long totalRead = 0;
						for (int i = 0; i <= slotIndex; ++i)
						{
							totalRead += downloadedByteSampleArr[i];
						}

						long tsDelta = System.currentTimeMillis() - startTimestamp;
						speed = (int) (totalRead * 1000 / tsDelta);

						if (tsDelta > mDownloadConfig.getDownloadSpeedSamplingTimeSpan())
						{
							slotIndex = getMaxDownloadedByteArrIndex();
						}
						else
						{
							slotIndex = (int) (tsDelta / mDownloadConfig.getUpdateProgressInterval());
						}
					}

					try
					{
						mNotifier.onProgressUpdate(mDownloadedBytes, mFileLength, speed);
					}
					catch (Exception e)
					{
						if (KCLog.DEBUG)
							e.printStackTrace();
					}
				}
				catch (Exception e)
				{
				}
			}
		}

		synchronized void onProgressUpdate(int bytes)
		{
			downloadedByteSampleArr[slotIndex] += bytes;
			mDownloadedBytes += bytes;
		}
	}
}
