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
package uk.ac.ebi.eva.accession.core.persistence;

import com.mongodb.BulkWriteError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.persistence.models.AccessionProjection;
import uk.ac.ebi.ampt2d.commons.accession.persistence.models.IAccessionedObject;
import uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.document.AccessionedDocument;
import uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.repository.BasicMongoDbAccessionedCustomRepositoryImpl;
import uk.ac.ebi.ampt2d.commons.accession.persistence.repositories.IAccessionedObjectCustomRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SubmittedVariantAccessioningRepositoryImpl
        implements IAccessionedObjectCustomRepository<Long, SubmittedVariantEntity> {
//        extends BasicMongoDbAccessionedCustomRepositoryImpl<Long, SubmittedVariantEntity> {

    private final static Logger logger = LoggerFactory.getLogger(SubmittedVariantAccessioningRepositoryImpl.class);
    private final Class<SubmittedVariantEntity> clazz;
    private MongoTemplate mongoTemplate;

    public SubmittedVariantAccessioningRepositoryImpl(MongoTemplate mongoTemplate) {
        //super(SubmittedVariantEntity.class, mongoTemplate);
        this.mongoTemplate = mongoTemplate;
        this.clazz = SubmittedVariantEntity.class;
    }

    List<AccessionProjection<Long>> findByAccessionGreaterThanEqualAndAccessionLessThanEqual(Long start, Long end) {
        return mongoTemplate.find(Query.query(Criteria.where("accession").gte(start).lte(end)),
                                    SubmittedVariantEntity.class)
                              .stream()
                              .map(IAccessionedObject::getAccession)
                              .map(accession -> (AccessionProjection<Long>) () -> accession)
                              .collect(Collectors.toList());
    }

    @Override
    public SaveResponse<Long> insert(List<SubmittedVariantEntity> documents) {
        checkHashUniqueness(documents);
        setAuditCreatedDate(documents);
        final BulkOperations insert = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, clazz).insert(documents);
        final Set<String> erroneousIds = new HashSet<>();

        try {
            insert.execute();
        } catch (BulkOperationException e) {
            e.getErrors().forEach(error -> {
                String errorId = reportBulkOperationException(error).orElseThrow(() -> e);
                erroneousIds.add(errorId);
            });
        } catch (RuntimeException e) {
            logger.error("Unexpected runtime exception in MongoDB bulk insert", e);
            throw e;
        }
        return generateSaveResponse(documents, erroneousIds);
    }

    private void checkHashUniqueness(Collection<SubmittedVariantEntity> documents) {
        final Set<String> duplicatedHash = new HashSet<>();
        documents.forEach(document -> {
            if (duplicatedHash.contains(document.getHashedMessage())) {
                throw new RuntimeException("Duplicated hash in MongoDB bulk insert.");
            }
            duplicatedHash.add(document.getHashedMessage());
        });
    }

    /**
     * Set this manually when using a bulk operation.
     *
     * @param documents MongoDB documents to have their creation date set
     */
    private void setAuditCreatedDate(Iterable<SubmittedVariantEntity> documents) {
        LocalDateTime createdDate = LocalDateTime.now();
        for (SubmittedVariantEntity document: documents) {
            document.setCreatedDate(createdDate);
        }
    }

    private Optional<String> reportBulkOperationException(BulkWriteError error) {
        if (11000 == error.getCode()) {
            final String message = error.getMessage();
            Pattern pattern = Pattern.compile("_id_ dup key:.\\{.:.\"(.*)\".\\}");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            } else {
                logger.error("Error parsing BulkWriteError in BulkOperationException. Code '" + error.getCode()
                                     + "' Message: '" + error.getMessage() + "'");
            }
        } else {
            logger.error("Unexpected BulkWriteError in BulkOperationException. Code: '" + error.getCode()
                                 + "'. Message: '" + error.getMessage() + "'");
        }
        return Optional.empty();
    }

    private SaveResponse<Long> generateSaveResponse(Collection<SubmittedVariantEntity> documents, Set<String> duplicatedHash) {
        final Set<Long> savedAccessions = new HashSet<>();
        final Set<Long> saveFailedAccessions = new HashSet<>();

        documents.forEach(document -> {
            if (!duplicatedHash.contains(document.getHashedMessage())) {
                savedAccessions.add(document.getAccession());
            } else {
                saveFailedAccessions.add(document.getAccession());
            }
        });

        return new SaveResponse<>(savedAccessions, saveFailedAccessions);
    }






//    public class SubmittedVariantAccessioningRepositoryImpl
//        extends BasicMongoDbAccessionedCustomRepositoryImpl<Long, SubmittedVariantEntity> {
//
//    private MongoOperations mongoOperations;
//
//    public SubmittedVariantAccessioningRepositoryImpl(MongoTemplate mongoTemplate) {
//        super(SubmittedVariantEntity.class, mongoTemplate);
//        mongoOperations = mongoTemplate;
//    }
//
//    List<AccessionProjection<Long>> findByAccessionGreaterThanEqualAndAccessionLessThanEqual(Long start, Long end) {
//        return mongoOperations.find(Query.query(Criteria.where("accession").gte(start).lte(end)),
//                                    SubmittedVariantEntity.class)
//                              .stream()
//                              .map(AccessionedDocument::getAccession)
//                              .map(accession -> (AccessionProjection<Long>) () -> accession)
//                              .collect(Collectors.toList());
//    }
}
