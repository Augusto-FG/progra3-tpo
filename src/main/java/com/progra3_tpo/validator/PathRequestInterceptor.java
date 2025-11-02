package com.progra3_tpo.validator;

import com.progra3_tpo.service.PathRequest;
import com.progra3_tpo.service.PathResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.Optional;

@ControllerAdvice
public class PathRequestInterceptor extends RequestBodyAdviceAdapter {

    private final PathRequestValidator validator;

    public PathRequestInterceptor(PathRequestValidator validator) {
        this.validator = validator;
    }

    //decide si el advice aplica (aquí cuando el parámetro es PathRequest).
    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return PathRequest.class.equals(methodParameter.getParameterType())
                || PathRequest.class.getName().equals(targetType.getTypeName());
    }

    //se ejecuta justo después de deserializar el body; llama al PathRequestValidator y, si hay error, lanza una excepción (InvalidPathRequestException) que corta el flujo.
    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        if (body instanceof PathRequest) {
            Optional<PathResponse> maybeError = validator.validate((PathRequest) body);
            if (maybeError.isPresent()) {
                throw new BadPathRequestException(maybeError.get());
            }
        }
        return body;
    }

    @ExceptionHandler(BadPathRequestException.class)
    public ResponseEntity<PathResponse> handleInvalid(BadPathRequestException ex) {
        PathResponse resp = ex.getResponse();
        // devolver 400 con el PathResponse de error (estructura ya definida)
        return ResponseEntity.badRequest().body(resp);
    }
}
