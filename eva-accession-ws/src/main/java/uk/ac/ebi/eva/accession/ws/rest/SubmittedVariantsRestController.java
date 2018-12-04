/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.ebi.eva.accession.ws.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.hashing.SHA1HashingFunction;
import uk.ac.ebi.ampt2d.commons.accession.rest.controllers.BasicRestController;
import uk.ac.ebi.ampt2d.commons.accession.rest.dto.AccessionResponseDTO;

import uk.ac.ebi.eva.accession.core.ISubmittedVariant;
import uk.ac.ebi.eva.accession.core.SubmittedVariant;
import uk.ac.ebi.eva.accession.core.SubmittedVariantAccessioningService;
import uk.ac.ebi.eva.accession.core.summary.SubmittedVariantSummaryFunction;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/v1/submitted-variants")
@Api(tags = {"Submitted variants"})
public class SubmittedVariantsRestController {

    private final BasicRestController<SubmittedVariant, ISubmittedVariant, String, Long> basicRestController;

    private Function<ISubmittedVariant, String> hashingFunction =
            new SubmittedVariantSummaryFunction().andThen(new SHA1HashingFunction());

    private SubmittedVariantAccessioningService submittedVariantsService;

    public SubmittedVariantsRestController(
            BasicRestController<SubmittedVariant, ISubmittedVariant, String, Long> basicRestController,
            SubmittedVariantAccessioningService submittedVariantsService) {
        this.basicRestController = basicRestController;
        this.submittedVariantsService = submittedVariantsService;
    }

    @ApiOperation(value = "Find submitted variants (SS) by identifier", notes = "This endpoint returns the submitted "
            + "variants (SS) represented by the given identifiers. For a description of the response, see "
            + "https://github.com/EBIvariation/eva-accession/wiki/Import-accessions-from-dbSNP#clustered-variant-refsnp-or-rs")
    @GetMapping(value = "/{identifiers}", produces = "application/json")
    public List<AccessionResponseDTO<SubmittedVariant, ISubmittedVariant, String, Long>> get(
            @PathVariable @ApiParam(value = "List of numerical identifiers of submitted variants, e.g.: 5000000000,"
                    + "5000000002", required = true) List<Long> identifiers) {

        return basicRestController.get(identifiers);
    }

    @GetMapping(value = "/exists", produces = "application/json")
    public boolean isVariantExists(@RequestParam(name="assemblyId") String assembly,
                                   @RequestParam(name="referenceName") String contig,
                                   @RequestParam(name="study") String study,
                                   @RequestParam(name="start") long start,
                                   @RequestParam(name="referenceBases") String reference,
                                   @RequestParam(name="alternateBases") String alternate) {

        return !getVariantByIdFields(assembly, contig, study, start, reference, alternate).isEmpty();
    }

    @GetMapping(value = "/by-id-fields", produces = "application/json")
    public List<AccessionResponseDTO<SubmittedVariant, ISubmittedVariant, String, Long>> getByIdFields(
            @RequestParam(name="assemblyId") String assembly,
            @RequestParam(name="referenceName") String contig,
            @RequestParam(name="study") String study,
            @RequestParam(name="start") long start,
            @RequestParam(name="referenceBases") String reference,
            @RequestParam(name="alternateBases") String alternate) {

        return getVariantByIdFields(assembly, contig, study, start, reference, alternate);
    }

    private List<AccessionResponseDTO<SubmittedVariant, ISubmittedVariant, String, Long>> getVariantByIdFields(
            String assembly, String contig, String study, long start, String reference, String alternate) {

        ISubmittedVariant variant = new SubmittedVariant(assembly, 0, study, contig, start, reference, alternate, null);
        String hash = hashingFunction.apply(variant);

        List<AccessionWrapper<ISubmittedVariant, String, Long>> variants = submittedVariantsService.getByHashedMessageIn(
                Collections.singletonList(hash));

        return variants.stream()
                       .map(wrapper -> new AccessionResponseDTO<>(wrapper, SubmittedVariant::new))
                       .collect(Collectors.toList());
    }

}

