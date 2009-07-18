/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.connolly.david.pgs.debug;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This is to avoid writing 0x50 0x47 in data parts of sup file.
 * @author slarti
 */
public class SafeByteArrayOutputStream extends ByteArrayOutputStream {
    private int lastWrite = -1;
    
    @Override
    public synchronized void write(int b) {
        if (lastWrite == 0x50 && b == 0x47) {
            //throw new RuntimeException("Invalid bitmap output.");
            System.out.println("Transparent output instead of invalid output.");
            super.write(0);
            return;
        }

        super.write(b);

        lastWrite = b;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        super.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        super.write(b);
    }

}
