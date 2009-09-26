/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.connolly.david.pgs.debug;

/**
 *
 * @author slarti
 */
public class InvalidBitmapException extends Exception {

    public InvalidBitmapException(Throwable cause) {
        super(cause);
    }

    public InvalidBitmapException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidBitmapException(String message) {
        super(message);
    }

    public InvalidBitmapException() {
    }

}
