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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Provides access to the spring application context. This is used for getting a
 * reference to the mocking data services that are used by the test cases.
 */
class ContextAccess implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    /**
     * Returns the spring application context and resets the reference to null
     * 
     * @return the application context
     */
    static ApplicationContext getAndResetApplicationContext() {
        ApplicationContext context = applicationContext;
        applicationContext = null;
        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext aApplicationContext) 
            throws BeansException {
        applicationContext = aApplicationContext;
    }
}
