/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.cognito.domain.data.model;

import lombok.Data;

/**
 * Cognito domain query request.
 */
@Data
public class CognitoDomainDataRequest {
    /**
     * Domain name.
     */
    private String domain;
}
