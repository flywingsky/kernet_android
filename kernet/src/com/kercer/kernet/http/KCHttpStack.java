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

//import org.apache.http.HttpResponse;

import com.kercer.kernet.http.KCDeliveryResponse;
import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.KCHttpResponse;
import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.error.KCAuthFailureError;

import java.io.IOException;

/**
 * An HTTP stack abstraction.
 */
public interface KCHttpStack
{
	/**
	 * Performs an HTTP request with the given parameters.
	 *
	 * <p>
	 * A GET request is sent if request.getPostBody() == null. A POST request is sent otherwise, and the Content-Type header is set to
	 * request.getPostBodyContentType().
	 * </p>
	 *
	 * @param aRequest
	 *            the request to perform
	 * @param aAdditionalHeaders
	 *            additional headers to be sent together with {@link KCHttpRequest#getHeaders()}
	 * @param aDelivery
	 *            delivery
	 * @return the HTTP response
	 */
	public KCHttpResponse performRequest(KCHttpRequest<?> aRequest, KCHeaderGroup aAdditionalHeaders, KCDeliveryResponse aDelivery) throws IOException, KCAuthFailureError;

}
