/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kercer.kernet.http;

import com.kercer.kercore.io.KCByteArrayPool;
import com.kercer.kercore.io.KCUtilIO;
import com.kercer.kernet.http.KCHttpRequest.Method;
import com.kercer.kernet.http.base.KCHeader;
import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCHttpContent;
import com.kercer.kernet.http.base.KCHttpDefine;
import com.kercer.kernet.http.base.KCHttpStatus;
import com.kercer.kernet.http.base.KCProtocolVersion;
import com.kercer.kernet.http.base.KCStatusLine;
import com.kercer.kernet.http.cookie.KCCookieManager;
import com.kercer.kernet.http.error.KCAuthFailureError;
import com.kercer.kernet.http.listener.KCHttpProgressListener;
import com.kercer.kernet.http.request.KCMultiPartRequest;
import com.kercer.kernet.http.request.KCMultiPartRequest.KCMultiPartParam;
import com.kercer.kernet.uri.KCURI;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import static com.kercer.kernet.http.request.KCMultipartUtils.BINARY;
import static com.kercer.kernet.http.request.KCMultipartUtils.BOUNDARY_PREFIX;
import static com.kercer.kernet.http.request.KCMultipartUtils.COLON_SPACE;
import static com.kercer.kernet.http.request.KCMultipartUtils.CONTENT_TYPE_MULTIPART;
import static com.kercer.kernet.http.request.KCMultipartUtils.CONTENT_TYPE_OCTET_STREAM;
import static com.kercer.kernet.http.request.KCMultipartUtils.CRLF;
import static com.kercer.kernet.http.request.KCMultipartUtils.FILENAME;
import static com.kercer.kernet.http.request.KCMultipartUtils.FORM_DATA;
import static com.kercer.kernet.http.request.KCMultipartUtils.HEADER_CONTENT_DISPOSITION;
import static com.kercer.kernet.http.request.KCMultipartUtils.HEADER_CONTENT_TRANSFER_ENCODING;
import static com.kercer.kernet.http.request.KCMultipartUtils.HEADER_CONTENT_TYPE;
import static com.kercer.kernet.http.request.KCMultipartUtils.SEMICOLON_SPACE;
import static com.kercer.kernet.http.request.KCMultipartUtils.getContentLengthForMultipartRequest;

/**
 * An {@link KCHttpStack} based on {@link HttpURLConnection}.
 */
public class KCHttpStackDefault implements KCHttpStack
{
	/**
	 * An interface for transforming URLs before use.
	 */
	public interface KCUrlRewriter
	{
		/**
		 * Returns a URL to use instead of the provided one, or null to indicate this URL should not be used at all.
		 */
		public String rewriteUrl(String originalUrl);
	}

	private final KCUrlRewriter mUrlRewriter;
	private final SSLSocketFactory mSslSocketFactory;
	protected final KCByteArrayPool mPool;

	private static int DEFAULT_POOL_SIZE = 4096;

	private static KCCookieManager mCookieManager = new KCCookieManager();

	public KCHttpStackDefault()
	{
		this(null);
	}

	/**
	 * @param urlRewriter
	 *            Rewriter to use for request URLs
	 */
	public KCHttpStackDefault(KCUrlRewriter urlRewriter)
	{
		this(urlRewriter, null, new KCByteArrayPool(DEFAULT_POOL_SIZE));
	}

	/**
	 * @param urlRewriter
	 *            Rewriter to use for request URLs
	 * @param sslSocketFactory
	 *            SSL factory to use for HTTPS connections
	 */
	public KCHttpStackDefault(KCUrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory, KCByteArrayPool pool)
	{
		mUrlRewriter = urlRewriter;
		mSslSocketFactory = sslSocketFactory;
		mPool = pool;

	}

	@Override
	public KCHttpResponse performRequest(KCHttpRequest<?> request, KCHeaderGroup additionalHeaders, KCDeliveryResponse aDelivery) throws IOException, KCAuthFailureError
	{
		String url = request.getUrl();

		if (mUrlRewriter != null)
		{
			String rewritten = mUrlRewriter.rewriteUrl(url);
			if (rewritten == null)
			{
				throw new IOException("URL blocked by rewriter: " + url);
			}
			url = rewritten;
		}
		URL parsedUrl = new URL(url);
		HttpURLConnection connection = openConnection(parsedUrl, request);

		//process request cookies
		mCookieManager.processRequest(request);

		// add headers
		KCHeader[] requestHeaders = request.getHeaders().getAllHeaders();
		for (KCHeader header : requestHeaders)
		{
			connection.addRequestProperty(header.getName(), header.getValue());
		}
		KCHeader[] addHeaders = additionalHeaders.getAllHeaders();
		for (KCHeader additionalHeader : addHeaders)
		{
			connection.addRequestProperty(additionalHeader.getName(), additionalHeader.getValue());
		}

		if (request instanceof KCMultiPartRequest)
		{
			setConnectionParametersForMultipartRequest(connection, request);
		}
		else
		{
			setConnectionParametersForRequest(connection, request);
		}

		// setConnectionParametersForRequest(connection, request);
		// Initialize HttpResponse with data from the HttpURLConnection.
		KCProtocolVersion protocolVersion = new KCProtocolVersion("HTTP", 1, 1);
		if (connection.getResponseCode() == -1)
		{
			// -1 is returned by getResponseCode() if the response code could not be retrieved.
			// Signal to the caller that something was wrong with the connection.
			throw new IOException("Could not retrieve response code from HttpUrlConnection.");
		}
		KCStatusLine responseStatus = new KCStatusLine(protocolVersion, connection.getResponseCode(), connection.getResponseMessage());
		KCHttpResponse response = new KCHttpResponse(responseStatus);

		//set headers
		for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet())
		{
			if (header.getKey() != null)
			{
				List<String> values = header.getValue();
				for (String value : values)
				{
					KCHeader h = new KCHeader(header.getKey(), value);
					response.addHeader(h);
				}
			}
		}

		aDelivery.postHeaders(request, responseStatus, response.getHeaderGroup());

		try
		{
			mCookieManager.processResponse(response.getHeaderGroup(), KCURI.parse(url));
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}

		if (hasResponseBody(request.getMethod(), responseStatus.getStatusCode()))
		{
			KCHttpContent content = contentFromConnection(connection, request);
			response.setContent(content);
		}

		return response;
	}

	/**
	 * Perform a multipart request on a connection
	 *
	 * @param connection
	 *            The Connection to perform the multi part request
	 * @param request
	 *            The params to add to the Multi Part request The files to upload
	 * @throws ProtocolException
	 */
	private static void setConnectionParametersForMultipartRequest(HttpURLConnection connection, KCHttpRequest<?> request) throws IOException, ProtocolException
	{

		final String charset = ((KCMultiPartRequest<?>) request).getProtocolCharset();
		final int curTime = (int) (System.currentTimeMillis() / 1000);
		final String boundary = BOUNDARY_PREFIX + curTime;
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setRequestProperty(HEADER_CONTENT_TYPE, String.format(CONTENT_TYPE_MULTIPART, charset, curTime));

		Map<String, KCMultiPartParam> multipartParams = ((KCMultiPartRequest<?>) request).getMultipartParams();
		Map<String, String> filesToUpload = ((KCMultiPartRequest<?>) request).getFilesToUpload();

		if (((KCMultiPartRequest<?>) request).isFixedStreamingMode())
		{
			int contentLength = getContentLengthForMultipartRequest(boundary, multipartParams, filesToUpload);

			connection.setFixedLengthStreamingMode(contentLength);
		}
		else
		{
			connection.setChunkedStreamingMode(0);
		}
		// Modified end

		PrintWriter writer = null;
		try
		{
			OutputStream out = connection.getOutputStream();
			writer = new PrintWriter(new OutputStreamWriter(out, charset), true);

			for (String key : multipartParams.keySet())
			{
				KCMultiPartParam param = multipartParams.get(key);

				writer.append(boundary).append(CRLF).append(String.format(HEADER_CONTENT_DISPOSITION + COLON_SPACE + FORM_DATA, key)).append(CRLF)
						.append(HEADER_CONTENT_TYPE + COLON_SPACE + param.contentType).append(CRLF).append(CRLF).append(param.value).append(CRLF).flush();
			}

			for (String key : filesToUpload.keySet())
			{

				File file = new File(filesToUpload.get(key));

				if (!file.exists())
				{
					throw new IOException(String.format("File not found: %s", file.getAbsolutePath()));
				}

				if (file.isDirectory())
				{
					throw new IOException(String.format("File is a directory: %s", file.getAbsolutePath()));
				}

				writer.append(boundary).append(CRLF).append(String.format(HEADER_CONTENT_DISPOSITION + COLON_SPACE + FORM_DATA + SEMICOLON_SPACE + FILENAME, key, file.getName()))
						.append(CRLF).append(HEADER_CONTENT_TYPE + COLON_SPACE + CONTENT_TYPE_OCTET_STREAM).append(CRLF)
						.append(HEADER_CONTENT_TRANSFER_ENCODING + COLON_SPACE + BINARY).append(CRLF).append(CRLF).flush();

				BufferedInputStream input = null;
				try
				{
					FileInputStream fis = new FileInputStream(file);
					int transferredBytes = 0;
					int totalSize = (int) file.length();
					input = new BufferedInputStream(fis);
					int bufferLength = 0;

					byte[] buffer = new byte[1024];
					while ((bufferLength = input.read(buffer)) > 0)
					{
						out.write(buffer, 0, bufferLength);
						transferredBytes += bufferLength;
						request.notifyProgress(transferredBytes, totalSize);
					}
					out.flush(); // Important! Output cannot be closed. Close of
									// writer will close
									// output as well.
				}
				finally
				{
					if (input != null)
						try
						{
							input.close();
						}
						catch (IOException ex)
						{
							ex.printStackTrace();
						}
				}
				writer.append(CRLF).flush(); // CRLF is important! It indicates
												// end of binary
												// boundary.
			}

			// End of multipart/form-data.
			writer.append(boundary + BOUNDARY_PREFIX).append(CRLF).flush();

		}
		catch (Exception e)
		{
			e.printStackTrace();

		}
		finally
		{
			if (writer != null)
			{
				writer.close();
			}
		}
	}

	/**
	 * Checks if a response message contains a body.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3">RFC 7230 section 3.3</a>
	 * @param requestMethod
	 *            request method
	 * @param responseCode
	 *            response status code
	 * @return whether the response has a body
	 */
	private static boolean hasResponseBody(int requestMethod, int responseCode)
	{
		return requestMethod != KCHttpRequest.Method.HEAD && !(KCHttpStatus.HTTP_CONTINUE <= responseCode && responseCode < KCHttpStatus.HTTP_OK)
				&& responseCode != KCHttpStatus.HTTP_NO_CONTENT && responseCode != KCHttpStatus.HTTP_NOT_MODIFIED;
	}

	/**
	 * Initializes an {@link KCHttpContent} from the given {@link HttpURLConnection}.
	 *
	 * @param connection
	 * @return an HttpEntity populated with data from <code>connection</code>.
	 */
	private KCHttpContent contentFromConnection(HttpURLConnection connection, final KCHttpRequest<?> request)
	{
		KCHttpContent entity = new KCHttpContent();
		InputStream inputStream;
		try
		{
			inputStream = connection.getInputStream();
		}
		catch (IOException ioe)
		{
			inputStream = connection.getErrorStream();
		}

		final int contentLength = connection.getContentLength();

		try
		{
			byte[] content = KCUtilIO.inputStreamToBytes(mPool, inputStream, contentLength, new KCUtilIO.KCCopyListener() {

				@Override
				public boolean onBytesCopied(int aCurrent, int aTotal, byte[] aBytes) {
					request.notifyProgress(aCurrent, contentLength);

					return true;
				}
			});
			entity.setContent(content);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		entity.setContentLength(contentLength);
		entity.setContentEncoding(connection.getContentEncoding());
		entity.setContentType(connection.getContentType());
		return entity;
	}

	/**
	 * Create an {@link HttpURLConnection} for the specified {@code url}.
	 */
	protected HttpURLConnection createConnection(URL url) throws IOException
	{
		return (HttpURLConnection) url.openConnection();
	}

	/**
	 * Opens an {@link HttpURLConnection} with parameters.
	 *
	 * @param url
	 * @return an open connection
	 * @throws IOException
	 */
	private HttpURLConnection openConnection(URL url, KCHttpRequest<?> request) throws IOException
	{
		HttpURLConnection connection = createConnection(url);

		int timeoutMs = request.getTimeoutMs();
		connection.setConnectTimeout(timeoutMs);
		connection.setReadTimeout(timeoutMs);
		connection.setUseCaches(false);
		connection.setDoInput(true);

		// use caller-provided custom SslSocketFactory, if any, for HTTPS
		if ("https".equals(url.getProtocol()) && mSslSocketFactory != null)
		{
			((HttpsURLConnection) connection).setSSLSocketFactory(mSslSocketFactory);
		}

		return connection;
	}

	/* package */static void setConnectionParametersForRequest(HttpURLConnection connection, KCHttpRequest<?> request) throws IOException, KCAuthFailureError
	{
		switch (request.getMethod())
		{
		case Method.DEPRECATED_GET_OR_POST:
			// This is the deprecated way that needs to be handled for backwards compatibility.
			// If the request's post body is null, then the assumption is that the request is
			// GET. Otherwise, it is assumed that the request is a POST.
			byte[] postBody = request.getBody();
			if (postBody != null)
			{
				// Prepare output. There is no need to set Content-Length explicitly,
				// since this is handled by HttpURLConnection using the size of the prepared
				// output stream.
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.addRequestProperty(KCHttpDefine.HEADER_CONTENT_TYPE, request.getBodyContentType());
				DataOutputStream out = new DataOutputStream(connection.getOutputStream());
				out.write(postBody);
				out.close();
			}
			break;
		case Method.GET:
			// Not necessary to set the request method because connection defaults to GET but
			// being explicit here.
			connection.setRequestMethod("GET");
			break;
		case Method.DELETE:
			connection.setRequestMethod("DELETE");
			break;
		case Method.POST:
			connection.setRequestMethod("POST");
			addBodyIfExists(connection, request);
			break;
		case Method.PUT:
			connection.setRequestMethod("PUT");
			addBodyIfExists(connection, request);
			break;
		case Method.HEAD:
			connection.setRequestMethod("HEAD");
			break;
		case Method.OPTIONS:
			connection.setRequestMethod("OPTIONS");
			break;
		case Method.TRACE:
			connection.setRequestMethod("TRACE");
			break;
		case Method.PATCH:
			// connection.setRequestMethod("PATCH");
			// If server doesnt support patch uncomment this
			connection.setRequestMethod("POST");
			connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
			addBodyIfExists(connection, request);
			break;
		default:
			throw new IllegalStateException("Unknown method type.");
		}
	}

	private static void addBodyIfExists(HttpURLConnection connection, KCHttpRequest<?> request) throws IOException, KCAuthFailureError
	{
		byte[] body = request.getBody();
		if (body != null)
		{
			connection.setDoOutput(true);
			connection.addRequestProperty(KCHttpDefine.HEADER_CONTENT_TYPE, request.getBodyContentType());
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());

			if (request.mHttpListener instanceof KCHttpProgressListener)
			{
				int transferredBytes = 0;
				int totalSize = body.length;
				int offset = 0;
				int chunkSize = Math.min(2048, Math.max(totalSize - offset, 0));
				while (chunkSize > 0 && offset + chunkSize <= totalSize)
				{
					out.write(body, offset, chunkSize);
					transferredBytes += chunkSize;
//					progressListener.onProgress(transferredBytes, totalSize);
					request.notifyProgress(transferredBytes, totalSize);
					offset += chunkSize;
					chunkSize = Math.min(chunkSize, Math.max(totalSize - offset, 0));
				}
			}
			else
			{
				out.write(body);
			}

			out.close();
		}
	}
}
