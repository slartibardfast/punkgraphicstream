/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.connolly.david.pgs.debug;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This is to avoid writing 0x50 0x47 in data parts of sup file.
 * @author slarti
 */
public class SupOutputStream extends OutputStream {
    private Integer lastWrite = -1;
    private final OutputStream out;

    public SupOutputStream() {
        out = new ByteArrayOutputStream(); // Old Behaviour
    }

    public SupOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws InvalidBitmapException, IOException {
        synchronized (out) {
            if (lastWrite == 0x50 && b == 0x47) {
                throw new InvalidBitmapException("Invalid bitmap generated, please change settings and/or report");
            }

            out.write(b);

            lastWrite = b;
        }
    }

    @Override
    public synchronized void write(byte[] buffer, int off, int len) throws IOException {
        for (int i = off; i < (len + off); i++) {
            write(buffer[i]);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }
    
    public void writeSupHeader(String fromBytes, String toBytes) throws IOException {
        synchronized (out) {
            out.write(0x50);
            out.write(0x47);

            for (int i = 0; i < 8; i = i + 2) {
                out.write(Integer.parseInt(fromBytes.substring(i, i + 2), 16));
            }

            for (int i = 0; i < 8; i = i + 2) {
                out.write(Integer.parseInt(toBytes.substring(i, i + 2), 16));
            }
            
            lastWrite = -1;
        }
    }

    public boolean isUnsafeWrite(int nextByte) {
        synchronized (out) {
            return lastWrite == 0x50 && nextByte == 0x47;
        }
    }
}
