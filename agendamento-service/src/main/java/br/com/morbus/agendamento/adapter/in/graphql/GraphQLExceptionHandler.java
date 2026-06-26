package br.com.morbus.agendamento.adapter.in.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment environment) {
        if (ex instanceof RuntimeException && isDomainException(ex)) {
            return GraphqlErrorBuilder.newError(environment)
                    .message(ex.getMessage())
                    .errorType(org.springframework.graphql.execution.ErrorType.BAD_REQUEST)
                    .build();
        }

        return null;
    }

    private boolean isDomainException(Throwable ex) {
        Package exceptionPackage = ex.getClass().getPackage();
        return exceptionPackage != null
                && exceptionPackage.getName().startsWith("br.com.morbus.agendamento.domain.exception");
    }
}
