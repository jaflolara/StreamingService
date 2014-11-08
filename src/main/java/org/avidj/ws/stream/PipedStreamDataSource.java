package org.avidj.ws.stream;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.UUID;

import javax.activation.DataSource;

/**
 * A data source that pipes the content written to an output stream to the input
 * stream returned. Please note that this class violates the contract of the
 * {@link javax.activation.DataSource} interface as the streams returned can be
 * used only once.
 */
class PipedStreamDataSource implements DataSource {
    private final String mime;
    private final String name = UUID.randomUUID().toString();
    private final PipedOutputStream out;
    private final PipedInputStream in;

    PipedStreamDataSource(PipedOutputStream aOut, String aMime) throws IOException {
        out = aOut;
        in = new PipedInputStream(out);
        mime = aMime;
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation violates the contract in that it returns a valid
     * input stream only the first time it is called.
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return in;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return out;
    }

    @Override
    public String getContentType() {
        return mime;
    }

    @Override
    public String getName() {
        return name;
    }
}
