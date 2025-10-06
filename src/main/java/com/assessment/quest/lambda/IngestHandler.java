package com.assessment.quest.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.assessment.quest.QuestApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;
import java.util.function.Function;

/**
 * Boots Spring once per cold start and dispatches to the "ingest" Function bean.
 */
public class IngestHandler implements RequestHandler<Map<String, Object>, String> {

    private static final ConfigurableApplicationContext CTX;
    @SuppressWarnings("unchecked")
    private static final Function<Map<String, Object>, String> FN;

    static {
        CTX = new SpringApplicationBuilder(QuestApplication.class)
                .web(WebApplicationType.NONE)
                .run();
        FN = (Function<Map<String, Object>, String>) CTX.getBean("ingest", Function.class);
    }

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        return FN.apply(input == null ? Map.of() : input);
    }
}
