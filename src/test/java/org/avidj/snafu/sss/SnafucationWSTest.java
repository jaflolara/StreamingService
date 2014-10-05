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

import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static java.util.Arrays.asList;

import org.avidj.snafu.SnafuRecord.Record;
import org.avidj.snafu.sss.StreamingResponse;
import org.avidj.snafu.sss.SnafucationWSClient;
import org.avidj.snafu.sss.Snafucator;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Tests of the MTOM feature for streaming data in SOAP web services.
 */
public class SnafucationWSTest {
    private static final Logger LOG = LoggerFactory.getLogger(SnafucationWSTest.class);    
    private static final List<Record> RESULT_SET_EMPTY = Collections.emptyList();
    private static final List<Record> RESULT_SET_1 = 
            Collections.unmodifiableList(toProtoBufs(asList(
            asList("lancelots", "favourite", "colour"),
            asList("is", "blue"),
            Collections.<String> emptyList(), // test empty rows as well
            asList("How", "much", "wood", "would", "a", "woodchuck", "chuck", "if", "a",
                    "woodchuck", "would", "chuck", "wood?"))));
    private static final List<Record> RS_LARGE = generateResultSet(1000, 5, 10);
    private static final List<Record> RS_VERY_LARGE = generateResultSet(100000, 5, 10);
    private static final List<Record> RS_RIDICULOUSLY_LARGE = 
            generateResultSet(1000000, 5, 10);

    private Server server;
    private SnafucationWSClient client;
    @Autowired 
    private Snafucator snafucator;

    @Before
    public void setUp() throws Exception {
        start(8081);
        client = new SnafucationWSClient("http://localhost:8081/ssss/Snafucation");
    }

    private static List<Record> toProtoBufs(List<List<String>> asList) {
        List<Record> buffers = new ArrayList<Record>(asList.size());
        for ( List<String> record : asList ) {
            buffers.add(Record.newBuilder().addAllValue(record).build());
        }
        return buffers;
    }

    /*
     * Generate a result set of the given size with the given minimal and maximal row lengths.
     */
    private static List<Record> generateResultSet(int size, int minRow, int maxRow) {
        final Random random = new Random(System.currentTimeMillis());
        List<String> valueSet = new ArrayList<>(maxRow);
        for (int i = 0; i < maxRow; i++) {
            valueSet.add((Integer.valueOf(random.nextInt()).toString()));
        }
        List<Record> rs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int rowSize = minRow + random.nextInt(maxRow - minRow + 1);
            Record.Builder row = Record.newBuilder();
            for ( int j = 0; j < rowSize; j++ ) {
                row.addValue(valueSet.get(random.nextInt(maxRow)));
            }
            rs.add(row.build());
        }
        return Collections.unmodifiableList(rs);
    }

    /*
     * Starts an embedded jetty using the {@code src/main/webapp} directory as
     * the web application directory and taking the descriptor file from the
     * {@code WEB-INF} directory therein. Furthermore, configures the
     * "data source" mock behind the web service to return appropriate test
     * data.
     */
    private void start(int aPort) throws Exception {
        // start the embedded jetty
        String webappDir = "src/main/webapp";
        String extraClassPath = "src/main/webapp/WEB-INF;target/classes/";
        String contextPath = "/ssss";
        server = new Server(aPort);
        WebAppContext root = new WebAppContext();
        root.setParentLoaderPriority(true);
        root.setExtraClasspath(extraClassPath);
        root.setDescriptor(new File(webappDir, "WEB-INF/web.xml").getAbsolutePath());
        root.setWar(new File(webappDir).getAbsolutePath());
        root.setContextPath(contextPath);
        server.setHandler(root);
        server.start();

        // configure the snafucator to return certain result sets on certain calls
        ApplicationContext context = ContextAccess.getAndResetApplicationContext();
        context.getAutowireCapableBeanFactory().autowireBean(this); // autowire the snafucator
        when(snafucator.snafucate(0)).thenReturn(RESULT_SET_EMPTY.iterator());
        when(snafucator.snafucate(1)).thenReturn(RESULT_SET_1.iterator());
        when(snafucator.snafucate(2)).thenReturn(RS_LARGE.iterator());
        when(snafucator.snafucate(3)).thenReturn(RS_VERY_LARGE.iterator());
        when(snafucator.snafucate(4)).thenReturn(RS_RIDICULOUSLY_LARGE.iterator());
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        server.join();
    }

    @Test
    public void testQueryEmptyResultSet() throws IOException, InterruptedException {
        StreamingResponse<Record> response = client.snafucate(0);
        Iterator<Record> resultSet = response.getResultSet();
        assertSame(RESULT_SET_EMPTY.iterator(), resultSet);
    }

    @Test
    public void testQuerySmallResultSet() throws IOException, InterruptedException {
        StreamingResponse<Record> response = client.snafucate(1);
        Iterator<Record> resultSet = response.getResultSet();
        assertSame(RESULT_SET_1.iterator(), resultSet);
    }

    @Test
    public void testQueryLargeResultSet() throws IOException, InterruptedException {
        StreamingResponse<Record> response = client.snafucate(2);
        Iterator<Record> resultSet = response.getResultSet();
        assertSame(RS_LARGE.iterator(), resultSet);
    }

    @Test
    public void testQueryVeryLargeResultSet() throws IOException, InterruptedException {
        StreamingResponse<Record> response = client.snafucate(3);
        Iterator<Record> resultSet = response.getResultSet();
        assertSame(RS_VERY_LARGE.iterator(), resultSet);
    }

    @Test
    public void testQueryRidiculouslyLargeResultSet() throws IOException, InterruptedException {
        StreamingResponse<Record> response = client.snafucate(4);
        Iterator<Record> resultSet = response.getResultSet();
        assertSame(RS_RIDICULOUSLY_LARGE.iterator(), resultSet);
    }

    /**
     * Asserts that the elements provided by the two iterators are equal.
     * 
     * @param expected the expected iterator
     * @param actual the actual iterator
     * @throws AssertionError if the two iterators are not equal
     */
    private void assertSame(Iterator<?> expected, Iterator<?> actual) throws AssertionError {
        int cnt = 0;
        while ( expected.hasNext() ) {
            Assert.assertThat(actual.hasNext(), is(true));
            Object exp = expected.next();
            Object act = actual.next();
            cnt++;
            Assert.assertThat(act, is(equalTo(exp)));
        }
        Assert.assertThat(actual.hasNext(), is(false));
        LOG.info("Received " + cnt + " rows");
    }
}
