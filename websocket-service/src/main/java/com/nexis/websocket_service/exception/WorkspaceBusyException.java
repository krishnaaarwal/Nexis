package com.nexis.websocket_service.exception;

public class WorkspaceBusyException extends RuntimeException {
    public WorkspaceBusyException(String message) {
        super(message);
    }
}
