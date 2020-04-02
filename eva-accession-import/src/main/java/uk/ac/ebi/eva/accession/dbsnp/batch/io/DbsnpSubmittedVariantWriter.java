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
package uk.ac.ebi.eva.accession.dbsnp.batch.io;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteResult;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;

import uk.ac.ebi.eva.accession.core.model.dbsnp.DbsnpSubmittedVariantEntity;
import uk.ac.ebi.eva.accession.core.batch.listeners.ImportCounts;

import java.util.List;

public class DbsnpSubmittedVariantWriter implements ItemWriter<DbsnpSubmittedVariantEntity> {

    private MongoTemplate mongoTemplate;

    private ImportCounts importCounts;

    public DbsnpSubmittedVariantWriter(MongoTemplate mongoTemplate, ImportCounts importCounts) {
        this.mongoTemplate = mongoTemplate;
        this.importCounts = importCounts;
    }

    @Override
    public void write(List<? extends DbsnpSubmittedVariantEntity> importedSubmittedVariants) {
        try {
            BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                                                                  DbsnpSubmittedVariantEntity.class);
            bulkOperations.insert(importedSubmittedVariants);
            bulkOperations.execute();
            importCounts.addSubmittedVariantsWritten(importedSubmittedVariants.size());
        } catch (DuplicateKeyException exception) {
            MongoBulkWriteException writeException = ((MongoBulkWriteException) exception.getCause());
            BulkWriteResult bulkWriteResult = writeException.getWriteResult();
            importCounts.addSubmittedVariantsWritten(bulkWriteResult.getInsertedCount());
            throw exception;
        }
    }

}
