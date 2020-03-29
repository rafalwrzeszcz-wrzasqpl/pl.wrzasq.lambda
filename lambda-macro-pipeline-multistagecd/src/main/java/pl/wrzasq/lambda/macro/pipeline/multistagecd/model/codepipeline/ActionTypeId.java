/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.multistagecd.model.codepipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * CodePipeline action type.
 */
@Data
public class ActionTypeId {
    /**
     * Source action type.
     */
    private static final String CATEGORY_SOURCE = "Source";

    /**
     * Approval action type.
     */
    private static final String CATEGORY_APPROVAL = "Approval";

    /**
     * Deploy action type.
     */
    private static final String CATEGORY_DEPLOY = "Deploy";

    /**
     * S3 service actions.
     */
    private static final String PROVIDER_S3 = "S3";

    /**
     * CloudFormation service actions.
     */
    private static final String PROVIDER_CLOUDFORMATION = "CloudFormation";

    /**
     * Manual actions provider.
     */
    private static final String PROVIDER_MANUAL = "Manual";

    /**
     * Action type category.
     */
    @JsonProperty("Category")
    private String category;

    /**
     * Action type owner.
     */
    @JsonProperty("Owner")
    private String owner = "AWS";

    /**
     * Providing service.
     */
    @JsonProperty("Provider")
    private String provider;

    /**
     * Type version.
     */
    @JsonProperty("Version")
    private String version = "1";

    /**
     * Creates S3 source action.
     *
     * @return Action type definition.
     */
    public static ActionTypeId s3Source() {
        return ActionTypeId.buildBuiltInAction(ActionTypeId.CATEGORY_SOURCE, ActionTypeId.PROVIDER_S3);
    }

    /**
     * Creates S3 deploy action.
     *
     * @return Action type definition.
     */
    public static ActionTypeId s3Deploy() {
        return ActionTypeId.buildBuiltInAction(ActionTypeId.CATEGORY_DEPLOY, ActionTypeId.PROVIDER_S3);
    }

    /**
     * Creates CloudFormation deploy action.
     *
     * @return Action type definition.
     */
    public static ActionTypeId cloudFormationDeploy() {
        return ActionTypeId.buildBuiltInAction(ActionTypeId.CATEGORY_DEPLOY, ActionTypeId.PROVIDER_CLOUDFORMATION);
    }

    /**
     * Creates manual approval action.
     *
     * @return Action type definition.
     */
    public static ActionTypeId manualApproval() {
        return ActionTypeId.buildBuiltInAction(ActionTypeId.CATEGORY_APPROVAL, ActionTypeId.PROVIDER_MANUAL);
    }

    /**
     * Creates type definition for build in CodePipeline actions.
     *
     * @param category Action category.
     * @param provider Providing service.
     * @return Action type definition.
     */
    private static ActionTypeId buildBuiltInAction(String category, String provider) {
        var type = new ActionTypeId();
        type.setCategory(category);
        type.setProvider(provider);
        return type;
    }
}
