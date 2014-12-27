package com.hua.exception;

public class IMException extends Exception {
	private static final long serialVersionUID = 1L;

	public IMException(String message) {
		super(message);
	}

	public IMException(String message, Throwable cause) {
		super(message, cause);
	}
}
