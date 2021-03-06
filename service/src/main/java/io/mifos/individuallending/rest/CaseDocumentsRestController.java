/*
 * Copyright 2017 Kuelap, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.individuallending.rest;

import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseCustomerDocuments;
import io.mifos.individuallending.internal.command.ChangeCaseDocuments;
import io.mifos.individuallending.internal.service.CaseDocumentsService;
import io.mifos.portfolio.api.v1.PermittableGroupIds;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.service.internal.service.CaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/individuallending/products/{productidentifier}/cases/{caseidentifier}/documents")
public class CaseDocumentsRestController {
  private final CommandGateway commandGateway;
  private final CaseService caseService;
  private final CaseDocumentsService caseDocumentsService;

  @Autowired
  public CaseDocumentsRestController(
      final CommandGateway commandGateway,
      final CaseService caseService,
      final CaseDocumentsService caseDocumentsService) {
    this.commandGateway = commandGateway;
    this.caseService = caseService;
    this.caseDocumentsService = caseDocumentsService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_DOCUMENT_MANAGEMENT)
  @RequestMapping(
      method = RequestMethod.GET,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody
  CaseCustomerDocuments
  getCaseDocuments(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier) {
    throwIfCaseDoesntExist(productIdentifier, caseIdentifier);

    final List<CaseCustomerDocuments.Document> ret = caseDocumentsService.find(productIdentifier, caseIdentifier)
        .collect(Collectors.toList());

    return new CaseCustomerDocuments(ret);
  }


  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_DOCUMENT_MANAGEMENT)
  @RequestMapping(
      method = RequestMethod.PUT,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  ResponseEntity<Void>
  changeCaseDocuments(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      final @RequestBody CaseCustomerDocuments instance) {
    throwIfCaseDoesntExist(productIdentifier, caseIdentifier);

    commandGateway.process(new ChangeCaseDocuments(productIdentifier, caseIdentifier, instance));

    return ResponseEntity.accepted().build();
  }

  private void throwIfCaseDoesntExist(final String productIdentifier, final String caseIdentifier) throws ServiceException {
    //noinspection unused
    Case x = caseService.findByIdentifier(productIdentifier, caseIdentifier)
        .orElseThrow(() -> ServiceException.notFound("Case ''{0}.{1}'' does not exist.", productIdentifier, caseIdentifier));
  }
}
