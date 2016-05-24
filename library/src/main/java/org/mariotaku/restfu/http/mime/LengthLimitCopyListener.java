package org.mariotaku.restfu.http.mime;

import org.mariotaku.commons.io.StreamUtils;

/**
 * Created by mariotaku on 16/5/22.
 */
class LengthLimitCopyListener implements StreamUtils.PreCopyListener, StreamUtils.CopyListener {

    private final long length;
    private long total;

    LengthLimitCopyListener(long length) {
        this.length = length;
        this.total = 0;
    }

    @Override
    public long onPreCopy(long max) {
        return Math.min(max, length - total);
    }

    @Override
    public boolean onCopied(long len, long total) {
        this.total = total;
        return total < length;
    }
}
