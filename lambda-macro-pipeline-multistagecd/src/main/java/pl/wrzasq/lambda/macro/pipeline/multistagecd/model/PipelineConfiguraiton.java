/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.multistagecd.model;

import lombok.Data;

/**
 * Pipeline configuration.
 */
@Data
public class PipelineConfiguraiton {
    /**
     * Checkout step default parameter and condition names.
     */
    private static final String DEFAULT_CHECKOUT_STEP_KEY = "HasCheckoutStep";

    /**
     * Next stage default parameter and condition names.
     */
    private static final String DEFAULT_NEXT_STAGE_KEY = "HasNextStage";

    /**
     * Manual approval step default parameter and condition names.
     */
    private static final String DEFAULT_MANUAL_APPROVAL_STEP_KEY = "RequiresManualApproval";

    /**
     * Name of pipeline resource.
     */
    private String resourceName = "Pipeline";

    /**
     * Parameter name for checkout step existence.
     */
    private String hasCheckoutStepParameterName = PipelineConfiguraiton.DEFAULT_CHECKOUT_STEP_KEY;

    /**
     * Parameter name for promotion step existence.
     */
    private String hasNextStageParameterName = PipelineConfiguraiton.DEFAULT_NEXT_STAGE_KEY;

    /**
     * Parameter name for approval step existence.
     */
    private String requiresManualApprovalParameterName = PipelineConfiguraiton.DEFAULT_MANUAL_APPROVAL_STEP_KEY;

    /**
     * Condition name for checkout step existence.
     */
    private String hasCheckoutStepConditionName = PipelineConfiguraiton.DEFAULT_CHECKOUT_STEP_KEY;

    /**
     * Condition name for promotion step existence.
     */
    private String hasNextStageConditionName = PipelineConfiguraiton.DEFAULT_NEXT_STAGE_KEY;

    /**
     * Condition name for approval step existence.
     */
    private String requiresManualApprovalConditionName = PipelineConfiguraiton.DEFAULT_MANUAL_APPROVAL_STEP_KEY;

    /**
     * Pipeline source action webhook authentication type (optional).
     */
    private Object webhookAuthenticationType;

    /**
     * Pipeline source action webhook token (optional).
     */
    private Object webhookSecretToken;
}
