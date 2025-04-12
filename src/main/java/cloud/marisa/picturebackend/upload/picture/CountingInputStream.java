package cloud.marisa.picturebackend.upload.picture;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author MarisaDAZE
 * @description 可以统计大小的InputStream
 * @date 2025/4/11
 */
@Getter
public class CountingInputStream extends BufferedInputStream {
    private long size;

    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    protected CountingInputStream(InputStream in) {
        super(in);
        size = 0;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            size++;
        }
        return b;
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n != -1) {
            size += n;
        }
        return n;
    }
}
