/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.project;

import java.util.Map;
import java.util.function.Function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import pl.wrzasq.lambda.macro.pipeline.project.model.CloudFormationMacroRequest;
import pl.wrzasq.lambda.macro.pipeline.project.model.CloudFormationMacroResponse;
import pl.wrzasq.lambda.macro.pipeline.project.template.ProcessedTemplate;

/**
 * CloudFormation macro handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler implements RequestHandler<CloudFormationMacroRequest, CloudFormationMacroResponse> {
    /**
     * CloudFormation template provider.
     */
    private Function<Map<String, Object>, ProcessedTemplate> templateFactory;

    /**
     * Default constructor.
     */
    public Handler() {
        this.templateFactory = ProcessedTemplate::new;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFormationMacroResponse handleRequest(CloudFormationMacroRequest event, Context context) {
        return new CloudFormationMacroResponse(
            event.getRequestId(),
            CloudFormationMacroResponse.STATUS_SUCCESS,
            this.templateFactory.apply(event.getFragment()).getTemplate()
        );
    }
}
