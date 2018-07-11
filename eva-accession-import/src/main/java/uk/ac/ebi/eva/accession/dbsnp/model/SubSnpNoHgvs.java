/*
 * Copyright 2014-2018 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.eva.accession.dbsnp.model;

import uk.ac.ebi.eva.commons.core.models.Region;

import java.sql.Date;

public class SubSnpNoHgvs {

    private Long ssId;

    private Long rsId;

    private String alleles;

    private String assembly;

    private String batchHandle;

    private String batchName;

    private Region chromosomeRegion;

    private Region contigRegion;

    private Orientation subsnpOrientation;

    private Orientation snpOrientation;

    private Orientation contigOrientation;

    private int contigStart;

    private boolean frequencyExists;

    private boolean genotypeExists;

    private String reference;

    private Date createTime;

    private int taxonomyId;

    private boolean assemblyMatch;

    public SubSnpNoHgvs(Long ssId, Long rsId, String alleles, String assembly, String batchHandle, String batchName,
                        String chromosome, Long chromosomeStart, String contigName, Orientation subsnpOrientation,
                        Orientation snpOrientation, Orientation contigOrientation, Long contigStart,
                        boolean frequencyExists, boolean genotypeExists, String reference, Date createTime,
                        int taxonomyId) {
        this.ssId = ssId;
        this.rsId = rsId;
        this.alleles = alleles;
        this.assembly = assembly;
        this.batchHandle = batchHandle;
        this.batchName = batchName;
        this.chromosomeRegion = createRegion(chromosome, chromosomeStart);
        this.contigRegion = createRegion(contigName, contigStart);
        this.subsnpOrientation = subsnpOrientation;
        this.snpOrientation = snpOrientation;
        this.contigOrientation = contigOrientation;
        this.frequencyExists = frequencyExists;
        this.genotypeExists = genotypeExists;
        this.reference = reference;
        this.createTime = createTime;
        this.taxonomyId = taxonomyId;
    }

    private Region createRegion(String sequenceName, Long start) {
        if (sequenceName != null) {
            if (start != null) {
                return new Region(sequenceName, start);
            }
            return new Region(sequenceName);
        }
        // This should happen only with chromosomes, when a contig-to-chromosome mapping is not available
        return null;
    }

    public Long getSsId() {
        return ssId;
    }

    public void setSsId(Long ssId) {
        this.ssId = ssId;
    }

    public Long getRsId() {
        return rsId;
    }

    public void setRsId(Long rsId) {
        this.rsId = rsId;
    }

    public String getAlleles() {
        return alleles;
    }

    public void setAlleles(String alleles) {
        this.alleles = alleles;
    }

    public String getAssembly() {
        return assembly;
    }

    public void setAssembly(String assembly) {
        this.assembly = assembly;
    }

    public String getBatchHandle() {
        return batchHandle;
    }

    public void setBatchHandle(String batchHandle) {
        this.batchHandle = batchHandle;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public Region getChromosomeRegion() {
        return chromosomeRegion;
    }

    public void setChromosomeRegion(Region chromosomeRegion) {
        this.chromosomeRegion = chromosomeRegion;
    }

    public Region getContigRegion() {
        return contigRegion;
    }

    public void setContigRegion(Region contigRegion) {
        this.contigRegion = contigRegion;
    }

    public Orientation getContigOrientation() {
        return contigOrientation;
    }

    public void setContigOrientation(Orientation contigOrientation) {
        this.contigOrientation = contigOrientation;
    }

    public int getContigStart() {
        return contigStart;
    }

    public void setContigStart(int contigStart) {
        this.contigStart = contigStart;
    }

    public boolean isFrequencyExists() {
        return frequencyExists;
    }

    public void setFrequencyExists(boolean frequencyExists) {
        this.frequencyExists = frequencyExists;
    }

    public boolean isGenotypeExists() {
        return genotypeExists;
    }

    public void setGenotypeExists(boolean genotypeExists) {
        this.genotypeExists = genotypeExists;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getTaxonomyId() {
        return taxonomyId;
    }

    public void setTaxonomyId(int taxonomyId) {
        this.taxonomyId = taxonomyId;
    }

    public boolean isAssemblyMatch() {
        return assemblyMatch;
    }

    public void setAssemblyMatch(boolean assemblyMatch) {
        this.assemblyMatch = assemblyMatch;
    }

    public String getReferenceInForwardStrand() {
        if (contigOrientation.equals(Orientation.REVERSE)) {
            return calculateReverseComplement(reference);
        } else {
            return reference;
        }
    }

    public String getAlternateInForwardStrand() {
        String[] alleles = splitAlleles();

        String reference = getReferenceInForwardStrand();
        for (String allele : alleles) {
            if (!allele.equals(reference)) {
                return allele;
            }
        }
        // TODO: if there are several alleles this is returning just the first

        return null;
    }

    private String[] splitAlleles() {
        Orientation allelesOrientation;
        try {
            allelesOrientation = getAllelesOrientation();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown alleles orientation for variant " + this, e);
        }

        if (allelesOrientation.equals(Orientation.FORWARD)) {
            return getAlleles().split("/");
        } else if (allelesOrientation.equals(Orientation.REVERSE)) {
            return getReversedComplementedAlleles();
        } else {
            throw new IllegalArgumentException(
                    "Unknown alleles orientation " + allelesOrientation + " for variant " + this);
        }
    }

    private Orientation getAllelesOrientation() {
        return Orientation.getOrientation(
                this.subsnpOrientation.getValue() * this.snpOrientation.getValue() * this.contigOrientation.getValue());
    }

    private String[] getReversedComplementedAlleles() {
        String[] alleles = this.alleles.split("/");
        for (int i=0; i < alleles.length; i++) {
            alleles[i] = calculateReverseComplement(alleles[i]);
        }
        return alleles;
    }

    private static String calculateReverseComplement(String alleleInReverseStrand) {
        StringBuilder alleleInForwardStrand = new StringBuilder(alleleInReverseStrand).reverse();
        for (int i = 0; i < alleleInForwardStrand.length(); i++) {
            switch (alleleInForwardStrand.charAt(i)) {
                // Capitalization holds a special meaning for dbSNP so we need to preserve it.
                // See https://www.ncbi.nlm.nih.gov/books/NBK44414/#_Reports_Lowercase_Small_Sequence_Letteri_
                case 'A':
                    alleleInForwardStrand.setCharAt(i, 'T');
                    break;
                case 'a':
                    alleleInForwardStrand.setCharAt(i, 't');
                    break;
                case 'C':
                    alleleInForwardStrand.setCharAt(i, 'G');
                    break;
                case 'c':
                    alleleInForwardStrand.setCharAt(i, 'g');
                    break;
                case 'G':
                    alleleInForwardStrand.setCharAt(i, 'C');
                    break;
                case 'g':
                    alleleInForwardStrand.setCharAt(i, 'c');
                    break;
                case 'T':
                    alleleInForwardStrand.setCharAt(i, 'A');
                    break;
                case 't':
                    alleleInForwardStrand.setCharAt(i, 'a');
                    break;
            }
        }
        return alleleInForwardStrand.toString();
    }
}
