/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.macro.pipeline.multistagecd;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.commons.aws.cloudformation.macro.CloudFormationMacroRequest;
import pl.wrzasq.commons.aws.cloudformation.macro.CloudFormationMacroResponse;
import pl.wrzasq.commons.aws.cloudformation.macro.MacroHandler;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.Handler;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {
    @Mock
    private MacroHandler macroHandler;

    @Test
    public void handle() throws NoSuchFieldException, IllegalAccessException {
        // for code coverage
        var handler = new Handler();
        var hack = Handler.class.getDeclaredField("macroHandler");
        hack.setAccessible(true);
        hack.set(handler, macroHandler);

        var requestId = "abc";
        var source = new HashMap<String, Object>();

        var request = new CloudFormationMacroRequest(requestId, source);
        var result = new CloudFormationMacroResponse(requestId, CloudFormationMacroResponse.STATUS_SUCCESS, null);

        Mockito
            .when(this.macroHandler.handleRequest(request))
            .thenReturn(result);

        var output = handler.handleRequest(
            new CloudFormationMacroRequest(requestId, source),
            null
        );

        Assertions.assertSame(
            result,
            output,
            "Handler.handleRequest() should return response with processed template."
        );
    }
}
