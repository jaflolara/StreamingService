package org.avidj.snafu.sss;

/*
 * #%L
 * SnafucationSoapStreamingService
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 David Kensche
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.MTOMFeature;

import org.avidj.snafu.SnafuRecord.Record;
import org.avidj.ws.stream.StreamingResponse;

import com.sun.xml.ws.developer.StreamingAttachmentFeature;

/**
 * The snafucation client serves as a stub to the web service. It is configured
 * with a web service URL and forwards calls to the
 * {@link org.avidj.snafu.sss.SnafucationWS} interface to the specified web
 * service implementation.
 */
public final class SnafucationWSClient implements SnafucationWS {
    private final Object lock = new Object();
    private final String serviceUrl;
    private volatile SnafucationWS service;

    /**
     * Create a new client connecting to the given URL.
     * 
     * @param aServiceUrl the URL to connect to
     */
    public SnafucationWSClient(String aServiceUrl) {
        serviceUrl = aServiceUrl;
    }

    private SnafucationWS connect() {
        Service result = null;
        try {
            result = Service.create(
                    new URL(serviceUrl + "?wsdl"), 
                    new QName("http://sss.snafu.avidj.org/", "SnafucationWSImplService"));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not create web service endpoint.", e);
        }
        SnafucationWS port = result.getPort(SnafucationWS.class,
                // Enable MTOM at the client for transmission of binary data
                new MTOMFeature(),
                // Load off attachments to the file system when exceeding 4MB in size.
                new StreamingAttachmentFeature("/tmp", false, 4000000L));
        return port;
    }

    /**
     * Lazily connect to the web service.
     * 
     * @return the web service to forward requests to
     */
    private SnafucationWS getService() {
        SnafucationWS result = service;
        if (result == null) {
            synchronized (lock) {
                result = service;
                if (result == null) {
                    result = service = connect();
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation forwards calls to a possibly remote service.
     */
    @Override
    public StreamingResponse<Record> snafucate(int i) throws IOException {
        return getService().snafucate(i);
    }
}
