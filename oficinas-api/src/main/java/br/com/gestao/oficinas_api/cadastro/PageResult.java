package br.com.gestao.oficinas_api.cadastro;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public static <T> PageResult<T> of(List<T> items, int page, int size, long total) {
        return new PageResult<>(items, page, size, total, total == 0 ? 0 : (int) Math.ceil((double) total / size));
    }
}
