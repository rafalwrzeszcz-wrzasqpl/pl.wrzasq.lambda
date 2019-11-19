/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.macro.lambda.function;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.lambda.macro.lambda.function.Handler;
import pl.wrzasq.lambda.macro.lambda.function.model.CloudFormationMacroRequest;
import pl.wrzasq.lambda.macro.lambda.function.model.CloudFormationMacroResponse;
import pl.wrzasq.lambda.macro.lambda.function.template.ProcessedTemplate;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {
    @Mock
    private ProcessedTemplate template;

    @Mock
    private Function<Map<String, Object>, ProcessedTemplate> templateFactory;

    @Test
    public void handle() throws NoSuchFieldException, IllegalAccessException {
        // for code coverage
        var handler = new Handler();
        var hack = Handler.class.getDeclaredField("templateFactory");
        hack.setAccessible(true);
        hack.set(handler, templateFactory);

        var source = new HashMap<String, Object>();
        var result = new HashMap<String, Object>();

        Mockito
            .when(this.templateFactory.apply(source))
            .thenReturn(template);

        Mockito
            .when(this.template.getTemplate())
            .thenReturn(result);

        String requestId = "abc";

        var output = handler.handleRequest(
            new CloudFormationMacroRequest(requestId, source),
            null
        );

        Assertions.assertEquals(
            requestId,
            output.getRequestId(),
            "Handler.handleRequest() should return response for same request ID."
        );
        Assertions.assertEquals(
            CloudFormationMacroResponse.STATUS_SUCCESS,
            output.getStatus(),
            "Handler.handleRequest() should set successful status of response."
        );
        Assertions.assertSame(
            result,
            output.getFragment(),
            "Handler.handleRequest() should use processor to modify template structure."
        );
    }
}
