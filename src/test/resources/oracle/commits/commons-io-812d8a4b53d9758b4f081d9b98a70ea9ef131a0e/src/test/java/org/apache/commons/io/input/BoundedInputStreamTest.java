/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.io.input;

import static org.apache.commons.io.IOUtils.EOF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link BoundedInputStream}.
 */
public class BoundedInputStreamTest {

    private void compare(final String msg, final byte[] expected, final byte[] actual) {
        assertEquals(expected.length, actual.length, msg + " length");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], msg + " byte[" + i + "]");
        }
    }

    @Test
    public void testBuilderGet() {
        // java.lang.IllegalStateException: origin == null
        assertThrows(IllegalStateException.class, () -> BoundedInputStream.builder().get());
    }

    @SuppressWarnings("deprecation")
    @ParameterizedTest
    @ValueSource(longs = { -100, -1, 0, 1, 2, 4, 8, 16, 32, 64 })
    public void testCounts(final long startCount) throws Exception {

        final byte[] helloWorld = "Hello World".getBytes(StandardCharsets.UTF_8);
        final byte[] hello = "Hello".getBytes(StandardCharsets.UTF_8);
        final long actualStart = startCount < 0 ? 0 : startCount;

        // limit = length
        try (BoundedInputStream bounded = BoundedInputStream.builder().setInputStream(new ByteArrayInputStream(helloWorld)).setCount(startCount)
                .setMaxCount(helloWorld.length).get()) {
            assertEquals(helloWorld.length, bounded.getMaxCount());
            assertEquals(helloWorld.length, bounded.getMaxLength());
            assertEquals(actualStart, bounded.getCount());
            assertEquals(Math.max(0, bounded.getMaxCount() - actualStart), bounded.getRemaining());
            assertEquals(Math.max(0, bounded.getMaxLength() - actualStart), bounded.getRemaining());
            int readCount = 0;
            for (int i = 0; i < helloWorld.length; i++) {
                final byte expectedCh = bounded.getRemaining() > 0 ? helloWorld[i] : EOF;
                final int actualCh = bounded.read();
                assertEquals(expectedCh, actualCh, "limit = length byte[" + i + "]");
                if (actualCh != EOF) {
                    readCount++;
                }
                assertEquals(helloWorld.length, bounded.getMaxCount());
                assertEquals(helloWorld.length, bounded.getMaxLength());
                assertEquals(actualStart + readCount, bounded.getCount(), "i=" + i);
                assertEquals(Math.max(0, bounded.getMaxCount() - (readCount + actualStart)), bounded.getRemaining());
                assertEquals(Math.max(0, bounded.getMaxLength() - (readCount + actualStart)), bounded.getRemaining());
            }
            assertEquals(-1, bounded.read(), "limit = length end");
            assertEquals(helloWorld.length, bounded.getMaxLength());
            assertEquals(readCount + actualStart, bounded.getCount());
            assertEquals(0, bounded.getRemaining());
            assertEquals(0, bounded.available());
        }
        // limit > length
        final int maxCountP1 = helloWorld.length + 1;
        try (BoundedInputStream bounded = BoundedInputStream.builder().setInputStream(new ByteArrayInputStream(helloWorld)).setCount(startCount)
                .setMaxCount(maxCountP1).get()) {
            assertEquals(maxCountP1, bounded.getMaxLength());
            assertEquals(actualStart, bounded.getCount());
            assertEquals(Math.max(0, bounded.getMaxCount() - actualStart), bounded.getRemaining());
            assertEquals(Math.max(0, bounded.getMaxLength() - actualStart), bounded.getRemaining());
            int readCount = 0;
            for (int i = 0; i < helloWorld.length; i++) {
                final byte expectedCh = bounded.getRemaining() > 0 ? helloWorld[i] : EOF;
                final int actualCh = bounded.read();
                assertEquals(expectedCh, actualCh, "limit = length byte[" + i + "]");
                if (actualCh != EOF) {
                    readCount++;
                }
                assertEquals(maxCountP1, bounded.getMaxCount());
                assertEquals(maxCountP1, bounded.getMaxLength());
                assertEquals(actualStart + readCount, bounded.getCount(), "i=" + i);
                assertEquals(Math.max(0, bounded.getMaxCount() - (readCount + actualStart)), bounded.getRemaining());
                assertEquals(Math.max(0, bounded.getMaxLength() - (readCount + actualStart)), bounded.getRemaining());
            }
            assertEquals(-1, bounded.read(), "limit > length end");
            assertEquals(0, bounded.available());
            assertEquals(maxCountP1, bounded.getMaxLength());
            assertEquals(readCount + actualStart, bounded.getCount());
            assertEquals(Math.max(0, maxCountP1 - bounded.getCount()), bounded.getRemaining());
        }
        // limit < length
        try (BoundedInputStream bounded = new BoundedInputStream(new ByteArrayInputStream(helloWorld), hello.length)) {
            assertEquals(hello.length, bounded.getMaxLength());
            assertEquals(0, bounded.getCount());
            assertEquals(bounded.getMaxLength(), bounded.getRemaining());
            int readCount = 0;
            for (int i = 0; i < hello.length; i++) {
                assertEquals(hello[i], bounded.read(), "limit < length byte[" + i + "]");
                readCount++;
                assertEquals(hello.length, bounded.getMaxLength());
                assertEquals(readCount, bounded.getCount());
                assertEquals(bounded.getMaxLength() - readCount, bounded.getRemaining());
            }
            assertEquals(-1, bounded.read(), "limit < length end");
            assertEquals(0, bounded.available());
            assertEquals(hello.length, bounded.getMaxLength());
            assertEquals(readCount, bounded.getCount());
            assertEquals(bounded.getMaxLength() - readCount, bounded.getRemaining());
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testOnMaxLength() throws Exception {
        final byte[] helloWorld = "Hello World".getBytes(StandardCharsets.UTF_8);
        final byte[] hello = "Hello".getBytes(StandardCharsets.UTF_8);
        final AtomicBoolean boolRef = new AtomicBoolean();

        // limit = length
        BoundedInputStream bounded = new BoundedInputStream(new ByteArrayInputStream(helloWorld), helloWorld.length) {
            @Override
            protected void onMaxLength(final long max, final long readCount) {
                boolRef.set(true);
            }
        };
        assertEquals(helloWorld.length, bounded.getMaxCount());
        assertEquals(helloWorld.length, bounded.getMaxLength());
        assertEquals(0, bounded.getCount());
        assertEquals(bounded.getMaxCount(), bounded.getRemaining());
        assertEquals(bounded.getMaxLength(), bounded.getRemaining());
        assertFalse(boolRef.get());
        int readCount = 0;
        for (int i = 0; i < helloWorld.length; i++) {
            assertEquals(helloWorld[i], bounded.read(), "limit = length byte[" + i + "]");
            readCount++;
            assertEquals(helloWorld.length, bounded.getMaxCount());
            assertEquals(helloWorld.length, bounded.getMaxLength());
            assertEquals(readCount, bounded.getCount());
            assertEquals(bounded.getMaxCount() - readCount, bounded.getRemaining());
            assertEquals(bounded.getMaxLength() - readCount, bounded.getRemaining());
        }
        assertEquals(-1, bounded.read(), "limit = length end");
        assertEquals(0, bounded.available());
        assertEquals(helloWorld.length, bounded.getMaxLength());
        assertEquals(readCount, bounded.getCount());
        assertEquals(bounded.getMaxLength() - readCount, bounded.getRemaining());
        assertTrue(boolRef.get());

        // limit > length
        boolRef.set(false);
        final int length2 = helloWorld.length + 1;
        bounded = new BoundedInputStream(new ByteArrayInputStream(helloWorld), length2) {
            @Override
            protected void onMaxLength(final long max, final long readCount) {
                boolRef.set(true);
            }
        };
        assertEquals(length2, bounded.getMaxLength());
        assertEquals(0, bounded.getCount());
        assertEquals(bounded.getMaxLength(), bounded.getRemaining());
        assertFalse(boolRef.get());
        readCount = 0;
        for (int i = 0; i < helloWorld.length; i++) {
            assertEquals(helloWorld[i], bounded.read(), "limit > length byte[" + i + "]");
            readCount++;
            assertEquals(length2, bounded.getMaxLength());
            assertEquals(readCount, bounded.getCount());
            assertEquals(bounded.getMaxLength() - readCount, bounded.getRemaining());
        }
        assertEquals(0, bounded.available());
        assertEquals(-1, bounded.read(), "limit > length end");
        assertEquals(length2, bounded.getMaxLength());
        assertEquals(readCount, bounded.getCount());
        assertEquals(bounded.getMaxLength() - readCount, bounded.getRemaining());
        assertFalse(boolRef.get());

        // limit < length
        boolRef.set(false);
        bounded = new BoundedInputStream(new ByteArrayInputStream(helloWorld), hello.length) {
            @Override
            protected void onMaxLength(final long max, final long readCount) {
                boolRef.set(true);
            }
        };
        assertEquals(hello.length, bounded.getMaxLength());
        assertEquals(0, bounded.getCount());
        assertEquals(bounded.getMaxLength(), bounded.getRemaining());
        assertFalse(boolRef.get());
        readCount = 0;
        for (int i = 0; i < hello.length; i++) {
            assertEquals(hello[i], bounded.read(), "limit < length byte[" + i + "]");
            readCount++;
            assertEquals(hello.length, bounded.getMaxLength());
            assertEquals(readCount, bounded.getCount());
            assertEquals(bounded.getMaxLength() - readCount, bounded.getRemaining());
        }
        assertEquals(-1, bounded.read(), "limit < length end");
        assertEquals(hello.length, bounded.getMaxLength());
        assertEquals(readCount, bounded.getCount());
        assertEquals(bounded.getMaxLength() - readCount, bounded.getRemaining());
        assertTrue(boolRef.get());
    }

    @Test
    public void testReadArray() throws Exception {

        final byte[] helloWorld = "Hello World".getBytes(StandardCharsets.UTF_8);
        final byte[] hello = "Hello".getBytes(StandardCharsets.UTF_8);

        try (BoundedInputStream bounded = BoundedInputStream.builder().setInputStream(new ByteArrayInputStream(helloWorld)).get()) {
            compare("limit = -1", helloWorld, IOUtils.toByteArray(bounded));
        }

        try (BoundedInputStream bounded = BoundedInputStream.builder().setInputStream(new ByteArrayInputStream(helloWorld)).setMaxCount(0).get()) {
            compare("limit = 0", IOUtils.EMPTY_BYTE_ARRAY, IOUtils.toByteArray(bounded));
        }

        try (BoundedInputStream bounded = BoundedInputStream.builder().setInputStream(new ByteArrayInputStream(helloWorld))
                .setMaxCount(helloWorld.length).get()) {
            compare("limit = length", helloWorld, IOUtils.toByteArray(bounded));
        }

        try (BoundedInputStream bounded = BoundedInputStream.builder().setInputStream(new ByteArrayInputStream(helloWorld))
                .setMaxCount(helloWorld.length + 1).get()) {
            compare("limit > length", helloWorld, IOUtils.toByteArray(bounded));
        }
        try (BoundedInputStream bounded = BoundedInputStream.builder().setInputStream(new ByteArrayInputStream(helloWorld))
                .setMaxCount(helloWorld.length - 6).get()) {
            compare("limit < length", hello, IOUtils.toByteArray(bounded));
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testReadSingle() throws Exception {
        BoundedInputStream bounded;
        final byte[] helloWorld = "Hello World".getBytes(StandardCharsets.UTF_8);
        final byte[] hello = "Hello".getBytes(StandardCharsets.UTF_8);

        // limit = length
        bounded = new BoundedInputStream(new ByteArrayInputStream(helloWorld), helloWorld.length);
        for (int i = 0; i < helloWorld.length; i++) {
            assertEquals(helloWorld[i], bounded.read(), "limit = length byte[" + i + "]");
        }
        assertEquals(-1, bounded.read(), "limit = length end");

        // limit > length
        bounded = new BoundedInputStream(new ByteArrayInputStream(helloWorld), helloWorld.length + 1);
        for (int i = 0; i < helloWorld.length; i++) {
            assertEquals(helloWorld[i], bounded.read(), "limit > length byte[" + i + "]");
        }
        assertEquals(-1, bounded.read(), "limit > length end");

        // limit < length
        bounded = new BoundedInputStream(new ByteArrayInputStream(helloWorld), hello.length);
        for (int i = 0; i < hello.length; i++) {
            assertEquals(hello[i], bounded.read(), "limit < length byte[" + i + "]");
        }
        assertEquals(-1, bounded.read(), "limit < length end");
    }
}
