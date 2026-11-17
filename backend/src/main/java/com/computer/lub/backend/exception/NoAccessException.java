package com.computer.lub.backend.exception;

//Выбрасывается, когда пользователь пытается выполнить действие с чужими данными.
//Глобальный обработчик превращает его в HTTP 403.
public class NoAccessException extends RuntimeException {

    public NoAccessException(String message) {
        super(message);
    }
}
