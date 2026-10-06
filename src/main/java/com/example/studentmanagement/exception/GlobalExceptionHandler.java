package com.example.studentmanagement.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class GlobalExceptionHandler {


	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception,
													 HttpServletRequest request) {
		Map<String, String> errors = new TreeMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return response(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
	}

	@ExceptionHandler({AuthenticationException.class, InvalidTokenException.class})
	public ResponseEntity<ApiError> handleUnauthorized(RuntimeException exception, HttpServletRequest request) {
		String message = exception instanceof AuthenticationException
				? "Invalid username or password" : exception.getMessage();
		return response(HttpStatus.UNAUTHORIZED, message, request, null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleForbidden(HttpServletRequest request) {
		return response(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request, null);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
		return response(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException exception, HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, exception.getMessage(), request, null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(HttpServletRequest request) {
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
	}

	private ResponseEntity<ApiError> response(HttpStatus status, String message, HttpServletRequest request,
											  Map<String, String> fieldErrors) {
		return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(),
				status.getReasonPhrase(), message, request.getRequestURI(), fieldErrors));
	}
}
