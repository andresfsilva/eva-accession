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
package uk.ac.ebi.eva.accession.ws;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import uk.ac.ebi.eva.accession.core.configuration.SubmittedVariantAccessioningConfiguration;
import uk.ac.ebi.eva.accession.core.persistence.SubmittedVariantAccessioningRepository;
import uk.ac.ebi.eva.accession.ws.rest.SubmittedVariantDTO;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({SubmittedVariantAccessioningConfiguration.class})
@TestPropertySource("classpath:accession-ws-test.properties")
public class VariantAccessioningRestControllerTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private SubmittedVariantAccessioningRepository accessioningRepository;

    @Test
    public void testRestApi() {
        String url = "/v1/variant";
        HttpEntity<Object> requestEntity = new HttpEntity<>(getListOfVariantMessages());
        ResponseEntity<Map> response = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    public void requestPostTwiceAndWeGetSameAccessions() {
        String url = "/v1/variant";
        HttpEntity<Object> requestEntity = new HttpEntity<>(getListOfVariantMessages());
        ResponseEntity<Map> response = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals(2, accessioningRepository.count());

        //Accessing Post Request again with same files
        response = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals(2, accessioningRepository.count());
    }

    @Test
    public void testGetVariantsRestApi() {
        String getAccessionsUrl = "/v1/variant";
        HttpEntity<Object> requestEntity = new HttpEntity<>(getListOfVariantMessages());
        ResponseEntity<Map> getAccessionsResponse = testRestTemplate.exchange(getAccessionsUrl, HttpMethod.POST,
                requestEntity, Map.class);
        assertEquals(HttpStatus.OK, getAccessionsResponse.getStatusCode());
        assertEquals(2, getAccessionsResponse.getBody().size());
        Object[] accessions = getAccessionsResponse.getBody().keySet().toArray();
        String getVariantsUrl = "/v1/variant/" + accessions[0] + "," + accessions[1];
        ResponseEntity<Map> getVariantsResponse = testRestTemplate.getForEntity(getVariantsUrl, Map.class);
        assertEquals(HttpStatus.OK, getVariantsResponse.getStatusCode());
        assertEquals(2, getVariantsResponse.getBody().size());
    }

    public List<SubmittedVariantDTO> getListOfVariantMessages() {
        SubmittedVariantDTO variant1 = new SubmittedVariantDTO("ASMACC01", "TAXACC01", "PROJACC01", "CHROM1", 1234,
                                                               "REF", "ALT", false);
        SubmittedVariantDTO variant2 = new SubmittedVariantDTO("ASMACC02", "TAXACC02", "PROJACC02", "CHROM2", 1234,
                                                               "REF", "ALT", false);
        return asList(variant1, variant2);
    }
}
