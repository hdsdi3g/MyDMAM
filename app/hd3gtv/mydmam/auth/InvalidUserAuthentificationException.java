package hd3gtv.mydmam.auth;

public class InvalidUserAuthentificationException extends Exception {
	
	InvalidUserAuthentificationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	InvalidUserAuthentificationException(String message) {
		super(message);
	}
	
}
