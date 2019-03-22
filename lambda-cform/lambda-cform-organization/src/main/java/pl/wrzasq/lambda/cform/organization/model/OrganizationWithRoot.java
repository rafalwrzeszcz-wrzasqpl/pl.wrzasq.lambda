/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.organization.model;

import com.amazonaws.services.organizations.model.Organization;
import com.amazonaws.services.organizations.model.Root;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Extended Organization model that contains root ID.
 */
@Data
@AllArgsConstructor
public class OrganizationWithRoot
{
    /**
     * Organization data.
     */
    private Organization organization;

    /**
     * Root organizational unit.
     */
    private Root root;
}
