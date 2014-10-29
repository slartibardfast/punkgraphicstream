/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.connolly.david.pgs;

/**
 *
 * @author slarti
 */
public class BitmapOversizeException extends Exception {

    public BitmapOversizeException(Throwable cause) {
        super(cause);
    }

    public BitmapOversizeException(String message, Throwable cause) {
        super(message, cause);
    }

    public BitmapOversizeException(String message) {
        super(message);
    }

    public BitmapOversizeException() {
    }

}
