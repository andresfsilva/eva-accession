/*
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
 */
package uk.ac.ebi.eva.accession.pipeline.batch.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;

import uk.ac.ebi.eva.accession.core.model.ISubmittedVariant;
import uk.ac.ebi.eva.accession.core.service.nonhuman.eva.SubmittedVariantAccessioningService;
import uk.ac.ebi.eva.accession.pipeline.batch.processors.VariantConverter;
import uk.ac.ebi.eva.commons.core.models.IVariant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AccessionWriter implements ItemStreamWriter<IVariant> {

    private static final Logger logger = LoggerFactory.getLogger(AccessionWriter.class);

    private SubmittedVariantAccessioningService service;

    private AccessionReportWriter accessionReportWriter;

    private VariantConverter variantConverter;

    public AccessionWriter(SubmittedVariantAccessioningService service, AccessionReportWriter accessionReportWriter,
                           VariantConverter variantConverter) {
        this.service = service;
        this.accessionReportWriter = accessionReportWriter;
        this.variantConverter = variantConverter;
    }

    @Override
    public void write(List<? extends IVariant> variants) throws Exception {
        List<ISubmittedVariant> submittedVariants = variants.stream().map(variantConverter::convert)
                                                            .collect(Collectors.toList());
        List<AccessionWrapper<ISubmittedVariant, String, Long>> accessions = service.getOrCreate(submittedVariants);
        accessionReportWriter.write(variants, accessions);
        checkCountsMatch(submittedVariants, accessions);
    }

    void checkCountsMatch(List<? extends ISubmittedVariant> variants,
                          List<AccessionWrapper<ISubmittedVariant, String, Long>> accessions) {
        if (variants.size() != accessions.size()) {
            Set<ISubmittedVariant> accessionedVariants = accessions.stream()
                                                                   .map(AccessionWrapper::getData)
                                                                   .collect(Collectors.toSet());
            HashSet<ISubmittedVariant> distinctVariants = new HashSet<>(variants);
            int duplicateCount = variants.size() - distinctVariants.size();
            if (duplicateCount != 0) {
                logger.warn("A variant chunk contains {} repeated variants. This is not an error, but please check " +
                                    "it's expected.", duplicateCount);
            }

            List<ISubmittedVariant> variantsWithoutAccession = distinctVariants.stream()
                                                                               .filter(v -> !accessionedVariants
                                                                                       .contains(v))
                                                                               .collect(Collectors.toList());
            if (variantsWithoutAccession.size() != 0) {
                logger.error("A problem occurred while accessioning a chunk. Total num variants = {}, distinct = {}, " +
                                     "duplicate = {}, accessioned = {}, not accessioned = {}",
                             variants.size(), distinctVariants.size(), duplicateCount, accessionedVariants.size(),
                             variantsWithoutAccession.size());
                logger.error("The non-accessioned variants are: {}", variantsWithoutAccession.toString());

                throw new IllegalStateException(
                        "A problem occurred while accessioning a chunk. See log for details.");
            }
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        accessionReportWriter.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        accessionReportWriter.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        accessionReportWriter.close();
    }
}