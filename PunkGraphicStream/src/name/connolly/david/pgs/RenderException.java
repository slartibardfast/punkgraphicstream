package name.connolly.david.pgs;

public class RenderException extends Exception {
	private static final long serialVersionUID = -7581580943250009746L;
	
	public RenderException() {
		super();
	}

	public RenderException(String message, Throwable cause) {
		super(message, cause);
	}

	public RenderException(String message) {
		super(message);
	}

	public RenderException(Throwable cause) {
		super(cause);
	}
}