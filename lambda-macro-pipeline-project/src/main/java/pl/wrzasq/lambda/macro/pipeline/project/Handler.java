/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.project;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import pl.wrzasq.commons.aws.cloudformation.macro.CloudFormationMacroRequest;
import pl.wrzasq.commons.aws.cloudformation.macro.CloudFormationMacroResponse;
import pl.wrzasq.commons.aws.cloudformation.macro.MacroHandler;
import pl.wrzasq.lambda.macro.pipeline.project.template.ProcessedTemplate;

/**
 * CloudFormation macro handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler implements RequestHandler<CloudFormationMacroRequest, CloudFormationMacroResponse> {
    /**
     * CloudFormation macro handler.
     */
    private MacroHandler macroHandler;

    /**
     * Default constructor.
     */
    public Handler() {
        this.macroHandler = new MacroHandler(ProcessedTemplate::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFormationMacroResponse handleRequest(CloudFormationMacroRequest event, Context context) {
        return this.macroHandler.handleRequest(event);
    }
}
