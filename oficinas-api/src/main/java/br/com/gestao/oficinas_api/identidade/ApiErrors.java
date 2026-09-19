package br.com.gestao.oficinas_api.identidade;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> api(ApiException e, HttpServletRequest request) {
        var p=ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(e.status), e.getMessage());
        p.setProperty("code",e.code);
        p.setProperty("traceId",UUID.randomUUID().toString());
        p.setProperty("errors",java.util.List.of());
        return ResponseEntity.status(e.status).body(p);
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> validation(Exception e, HttpServletRequest request) {
        return api(new ApiException(400,"DADOS_INVALIDOS","Confira os campos informados."),request);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> conflict(Exception e, HttpServletRequest request) {
        return api(new ApiException(409,"CADASTRO_INDISPONIVEL","Não foi possível cadastrar com esses dados."),request);
    }
    public static void write(HttpServletResponse response, int status, String code, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        var body=java.util.Map.of("type","about:blank","status",status,
            "title",HttpStatus.valueOf(status).getReasonPhrase(),"code",code,"detail",detail,
            "traceId",UUID.randomUUID().toString(),"errors",java.util.List.of());
        response.getWriter().write(new tools.jackson.databind.ObjectMapper().writeValueAsString(body));
    }
}
