/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.grizzly.http2.frames;

import java.util.Collections;
import java.util.Map;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.memory.MemoryManager;

public class GoAwayFrame extends Http2Frame {
    private static final ThreadCache.CachedTypeIndex<GoAwayFrame> CACHE_IDX = ThreadCache.obtainIndex(GoAwayFrame.class, 8);

    public static final int TYPE = 7;

    private int lastStreamId;
    private ErrorCode errorCode;
    private Buffer additionalDebugData;

    // ------------------------------------------------------------ Constructors

    private GoAwayFrame() {
    }

    // ---------------------------------------------------------- Public Methods

    static GoAwayFrame create() {
        GoAwayFrame frame = ThreadCache.takeFromCache(CACHE_IDX);
        if (frame == null) {
            frame = new GoAwayFrame();
        }
        return frame;
    }

    public static Http2Frame fromBuffer(final int streamId, final Buffer frameBuffer) {
        GoAwayFrame frame = create();
        frame.setStreamId(streamId);
        frame.lastStreamId = frameBuffer.getInt() & 0x7fffffff;
        frame.errorCode = ErrorCode.lookup(frameBuffer.getInt());
        frame.additionalDebugData = frameBuffer.hasRemaining() ? frameBuffer : null;

        frame.setFrameBuffer(frameBuffer);

        return frame;
    }

    public static GoAwayFrameBuilder builder() {
        return new GoAwayFrameBuilder();
    }

    public int getLastStreamId() {
        return lastStreamId;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Buffer getAdditionalDebugData() {
        return additionalDebugData;
    }

    @Override
    public String toString() {
        final boolean hasAddData = additionalDebugData != null && additionalDebugData.hasRemaining();

        final StringBuilder sb = new StringBuilder();
        sb.append("GoAwayFrame {").append(headerToString()).append("{lastStreamId=").append(lastStreamId).append(", errorCode=").append(errorCode);
        if (hasAddData) {
            sb.append(", additionalDebugData={").append(additionalDebugData.toStringContent()).append('}');
        }

        sb.append('}');
        return sb.toString();
    }

    // -------------------------------------------------- Methods from Http2Frame

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public Buffer toBuffer(final MemoryManager memoryManager) {
        final Buffer buffer = memoryManager.allocate(FRAME_HEADER_SIZE + 8);

        serializeFrameHeader(buffer);
        buffer.putInt(lastStreamId & 0x7fffffff);
        buffer.putInt(errorCode.getCode());

        buffer.trim();

        if (additionalDebugData == null || !additionalDebugData.hasRemaining()) {
            return buffer;
        }

        final CompositeBuffer cb = CompositeBuffer.newBuffer(memoryManager, buffer, additionalDebugData);

        cb.allowBufferDispose(true);
        cb.allowInternalBuffersDispose(true);
        return cb;
    }

    @Override
    protected int calcLength() {
        return 8 + (additionalDebugData != null ? additionalDebugData.remaining() : 0);
    }

    @Override
    protected Map<Integer, String> getFlagNamesMap() {
        return Collections.emptyMap();
    }

    // -------------------------------------------------- Methods from Cacheable

    @Override
    public void recycle() {
        if (DONT_RECYCLE) {
            return;
        }

        errorCode = null;
        lastStreamId = 0;
        additionalDebugData = null;

        super.recycle();
        ThreadCache.putToCache(CACHE_IDX, this);
    }

    // ---------------------------------------------------------- Nested Classes

    public static class GoAwayFrameBuilder extends Http2FrameBuilder<GoAwayFrameBuilder> {

        private int lastStreamId;
        private ErrorCode errorCode;
        private Buffer additionalDebugData;

        // -------------------------------------------------------- Constructors

        protected GoAwayFrameBuilder() {
        }

        // ------------------------------------------------------ Public Methods

        public GoAwayFrameBuilder errorCode(final ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public GoAwayFrameBuilder lastStreamId(final int lastStreamId) {
            this.lastStreamId = lastStreamId;
            return this;
        }

        public GoAwayFrameBuilder additionalDebugData(final Buffer additionalDebugData) {
            this.additionalDebugData = additionalDebugData;
            return this;
        }

        @Override
        public GoAwayFrame build() {
            final GoAwayFrame frame = GoAwayFrame.create();
            setHeaderValuesTo(frame);

            frame.lastStreamId = lastStreamId;
            frame.errorCode = errorCode;
            frame.additionalDebugData = additionalDebugData;

            return frame;
        }

        // --------------------------------------- Methods from Http2FrameBuilder

        @Override
        protected GoAwayFrameBuilder getThis() {
            return this;
        }

    }
}
