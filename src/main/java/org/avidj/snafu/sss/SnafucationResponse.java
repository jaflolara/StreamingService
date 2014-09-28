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

import java.io.IOException;import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlAccessType;

import org.avidj.snafu.SnafuRecord.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.ws.developer.StreamingDataHandler;
import com.sun.xml.ws.encoding.DataSourceStreamingDataHandler;

/**
 * A response to a snafucation request. This result set can be accessed by
 * calling {@link #getResultSet()} which returns an iterator backed by a
 * blocking queue. This allows to stream structured results from the server to
 * the client. So even very large results need not be held in memory. This
 * example returns results in the form of {@code Record} objects which encapsulate
 * lists of strings resembling (stringly typed) relational tuples. 
 * The tuples can be of varying length.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "snafucationResponse")
@XmlType(name = "snafucationResponseType")
public class SnafucationResponse {
    private static final Logger LOG = LoggerFactory.getLogger(SnafucationResponse.class);
    private static final String OCTET_STREAM = "application/octet-stream";
    private final Object lock = new Object();
    private DataHandler dataHandler;
    private BlockingQueue<Record> queue;
    private Iterator<Record> resultIterator;
    private boolean closed = false;

    SnafucationResponse() { }

    /**
     * Create a new response at the server side given an iterator over the
     * results.
     * 
     * @param resultSet the result set to encapsulate, not {@code null}
     * @throws IOException if an error occurs while initializing the data handler
     */
    SnafucationResponse(Iterator<Record> resultSet) throws IOException {
        if (resultSet == null) {
            throw new NullPointerException("resultSet");
        }
        dataHandler = encode(resultSet);
    }

    /**
     * Returns a data handler that encapsulates the result set, called by JAXB.
     * 
     * @return a data handler encapsulating the binary attachment
     */
    @XmlElement(required = true)
    @XmlMimeType(OCTET_STREAM)
    DataHandler getResults() {
        return dataHandler;
    }

    /**
     * Sets a data handler that encapsulates the result set, called by JAXB on
     * the client side.
     * 
     * @param aDataHandler a data handler encapsulating the binary attachment
     * @throws IOException if an error occurs while initializing the decoding of the result set
     */
    void setResults(DataHandler aDataHandler) throws IOException {
        queue = new ArrayBlockingQueue<Record>(1000);
        resultIterator = new ResultIterator();
        StreamingDataHandler dh = (StreamingDataHandler) aDataHandler;
        SnafucationDecoder decoder = new SnafucationDecoder(dh.readOnce(), queue);
        new Thread(decoder).start(); // start to fill the queue to make results available
    }

    /**
     * The returned iterator reads results received asynchronously from the
     * server.
     * 
     * @return an iterator over the result set
     */
    public Iterator<Record> getResultSet() {
        return resultIterator;
    }

    /**
     * Writes the contents of the given result set to the stream provided by the
     * data handler. The data handler is used to transmit the MIME attachment to
     * the client.
     * 
     * @param resultSet the results to encode
     * @return a data handler that can be transferred to the client
     * @throws IOException if an I/O error occurs while creating the piped data source
     */
    private DataHandler encode(Iterator<Record> resultSet) throws IOException {
        PipedOutputStream out = new PipedOutputStream();
        SnafucationEncoder encoder = new SnafucationEncoder(out, resultSet);
        dataHandler = 
                new DataSourceStreamingDataHandler(new PipedStreamDataSource(out, OCTET_STREAM));
        new Thread(encoder).start(); // start writing to the stream
                                     // asynchronously
        return dataHandler;
    }

    /**
     * Guarded wait condition that passes when there is at least one result
     * available or the underlying stream has been closed, i.e., we know that
     * waiting won't do any good.
     * 
     * @throws InterruptedException if the waiting thread is interrupted
     */
    private void awaitResultsAvailable() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty() && !closed) {
                lock.wait();
            }
        }
    }

    /**
     * Writes the contents of an iterator to an output stream. This encoder
     * closes the given output stream after having written the last element of
     * the iterator.
     */
    private static class SnafucationEncoder implements Runnable {
        private final OutputStream out;
        private final Iterator<Record> resultSet;
        private int rowCount = 0;

        /**
         * @param aOut
         *            the stream to write to
         * @param aResultSet
         *            the result set to be written
         */
        SnafucationEncoder(OutputStream aOut, Iterator<Record> aResultSet) {
            assert (aOut != null);
            assert (aResultSet != null);
            out = aOut;
            resultSet = aResultSet;
        }

        @Override
        public void run() {
            try ( OutputStream out = this.out ) {
                while ( resultSet.hasNext() ) {
                    rowCount++;
                    if (rowCount % 100000 == 0) {
                        LOG.debug("server: writing element " + rowCount);
                    }
                    Record row = resultSet.next();
                    // write the size of the row
                    row.writeDelimitedTo(out);
                }
            } catch (IOException e) {
                LOG.error("Could not write to output stream.", e);
            }
        }
    }

    /**
     * This decoder takes an input stream, reads rows from it and offers them to
     * the given blocking queue. The expected encoding is the one produced by
     * the {@link org.avidj.snafu.sss.SnafucationResponse.SnafucationEncoder}.
     */
    private class SnafucationDecoder implements Runnable {
        private final BlockingQueue<Record> resultSet;
        private final InputStream in;

        /**
         * @param aIn
         *            the stream to read from
         * @param aQueue
         *            the queue to offer results to
         */
        SnafucationDecoder(InputStream aIn, BlockingQueue<Record> aQueue) {
            resultSet = aQueue;
            in = aIn;
        }

        @Override
        public void run() {
            try ( InputStream in = this.in ) {
                Record record = null;
                while ( ( record = Record.parseDelimitedFrom(in) ) != null ) {
                    put(record);
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read from stream.", e);
            } catch (InterruptedException e) {
                LOG.warn("Result parsing has been cancelled.", e);
            } finally {
                synchronized (lock) {
                    // signal to waiting readers that nothing more will be written to the queue
                    closed = true; 
                    lock.notifyAll();
                }
            }
        }

        private void put(Record row) throws InterruptedException {
            synchronized (lock) {
                while ( !resultSet.offer(row) ) {
                    lock.wait();
                }
                lock.notifyAll(); // notify readers that new rows are available
            }
        }
    }

    /**
     * Allows for asynchronously iterating results at the client while the server is still writing.
     */
    private class ResultIterator implements Iterator<Record> {
        private int rowCount = 0;

        /**
         * {@inheritDoc}
         * 
         * This method may block until results are available.
         */
        @Override
        public boolean hasNext() {
            synchronized (lock) {
                try {
                    awaitResultsAvailable();
                    return (queue.size() > 0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * Results will be non-null. If this method returns a {@code null}
         * result you may want to check whether the current thread has been
         * interrupted.
         * 
         * @throws NoSuchElementException if {@code !hasNext()}
         */
        @Override
        public Record next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            try {
                synchronized (lock) {
                    Record next = queue.take();
                    rowCount++;
                    lock.notifyAll(); // notify waiting threads about new
                                      // available elements
                    if (rowCount % 100000 == 0) {
                        LOG.debug("client: read element " + rowCount);
                    }
                    return next;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @throws UnsupportedOperationException always
         */
        @Override
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }
}
