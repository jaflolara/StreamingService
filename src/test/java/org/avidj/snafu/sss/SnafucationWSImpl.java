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

import javax.jws.WebService;
import javax.xml.ws.soap.MTOM;

import org.avidj.snafu.SnafuRecord.Record;
import org.avidj.ws.stream.StreamingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sun.xml.ws.developer.StreamingAttachment;

/**
 * Implementation of the snafucation web service.
 */
@WebService(endpointInterface = "org.avidj.snafu.sss.SnafucationWS")
@MTOM // enable the MTOM feature for parsing buffers at the client while server is sending
@StreamingAttachment(parseEagerly = false, dir = "/tmp", memoryThreshold = 1000000L)
@Service
public class SnafucationWSImpl implements SnafucationWS {
    @Autowired
    private Snafucator snafucator;

    @Override
    public StreamingResponse<Record> snafucate(int i) throws IOException {
        return new StreamingResponse<>(snafucator.snafucate(i));
    }
}
