package uk.ac.ebi.eva.accession.pipeline.steps.processors;

import org.springframework.batch.item.ItemProcessor;

import uk.ac.ebi.eva.commons.core.models.IVariant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filters structural variants.
 *
 * This pipeline will only assign SS and RS identifiers to precise variants if they are SNVs, MNVs or INDELs. Alleles of
 * that kind of variants can only be represented in a VCF by a String composed by A, C, G, T or the character comma (,).
 * Any other representation will be considered a structural variant hence will be excluded.
 */
public class ExcludeStructuralVariantsProcessor implements ItemProcessor<IVariant, IVariant> {

    private static final String ALLELES_REGEX = "^[acgtnACGTN,]+$";

    private static final Pattern ALLELES_PATTERN = Pattern.compile(ALLELES_REGEX);

    @Override
    public IVariant process(IVariant variant) throws Exception {
        Matcher matcher = ALLELES_PATTERN.matcher(variant.getAlternate());
        if(matcher.matches()) {
            return variant;
        }
        return null;
    }
}
