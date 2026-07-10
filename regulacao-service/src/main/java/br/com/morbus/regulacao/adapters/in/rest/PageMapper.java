package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.domain.dto.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public final class PageMapper {

    private PageMapper() {
    }

    public static <T> Page<T> toSpringPage(PageResult<T> result) {
        return new PageImpl<>(result.content(), PageRequest.of(result.page(), result.size()), result.totalElements());
    }
}
