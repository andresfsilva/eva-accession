package uk.ac.ebi.eva.accession.pipeline.steps.processors;

import org.junit.BeforeClass;
import org.junit.Test;

import uk.ac.ebi.eva.commons.core.models.IVariant;
import uk.ac.ebi.eva.commons.core.models.pipeline.Variant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExcludeStructuralVariantsProcessorTest {

    private static final String SINGLE_BASE_ALLELE = "A";

    private static final String SYMBOLIC_ALLELE = "<ID>";

    private static final String BREAK_END_NOTATION_ALLELE = "G]2 : 421681]";

    private static ExcludeStructuralVariantsProcessor processor;

    @BeforeClass
    public static void setUp() {
        processor = new ExcludeStructuralVariantsProcessor();
    }

    @Test
    public void variantWithSingleBaseAllele() throws Exception {
        IVariant variant = newVariant(SINGLE_BASE_ALLELE);
        assertEquals(variant, processor.process(variant));
    }

    private IVariant newVariant(String alternate) {
        return new Variant("contig", 1000, 1001, "A", alternate);
    }

    @Test
    public void variantWithSymbolicAllele() throws Exception {
        IVariant variant = newVariant(SYMBOLIC_ALLELE);
        assertNull(processor.process(variant));
    }

    @Test
    public void variantWithBreakEndNotationAllele() throws Exception {
        IVariant variant = newVariant(BREAK_END_NOTATION_ALLELE);
        assertNull(processor.process(variant));
    }
}