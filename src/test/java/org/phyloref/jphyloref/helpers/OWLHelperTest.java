package org.phyloref.jphyloref.helpers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/** A unit test for running the JPhyloRef application. */
@DisplayName("OWLHelper")
class OWLHelperTest {
  @Nested
  @DisplayName("has methods for reading labels that can")
  class ReadingLabelsTest {
    OWLOntologyManager ontologyManager;
    OWLOntology testOntology;

    /** Set up the test ontology with annotations involving labels */
    @BeforeEach
    void setupOntology() throws OWLOntologyCreationException {
      // Set up a set of axioms we will use to run these tests.
      ontologyManager = OWLManager.createOWLOntologyManager();
      OWLDataFactory df = ontologyManager.getOWLDataFactory();

      // A property for RDFS_LABEL.
      OWLAnnotationProperty RDFSLabelProperty =
          df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());

      // Set up some labels on a named individual.
      List<OWLAxiom> axioms = new ArrayList<>();
      IRI phyloref1IRI = IRI.create("http://example.org/phyloref1");
      OWLNamedIndividual phyloref = df.getOWLNamedIndividual(phyloref1IRI);
      axioms.add(
          df.getOWLAnnotationAssertionAxiom(
              RDFSLabelProperty, phyloref1IRI, df.getOWLLiteral("Label without a language", "")));
      axioms.add(
          df.getOWLAnnotationAssertionAxiom(
              RDFSLabelProperty, phyloref1IRI, df.getOWLLiteral("Label in English", "en")));
      axioms.add(
          df.getOWLAnnotationAssertionAxiom(
              RDFSLabelProperty, phyloref1IRI, df.getOWLLiteral("Etikett auf Englisch", "de")));
      axioms.add(
          df.getOWLAnnotationAssertionAxiom(
              RDFSLabelProperty, phyloref1IRI, df.getOWLLiteral("अंग्रेजी में लेबल", "hi")));

      // Set up the test ontology.
      testOntology = ontologyManager.createOntology(new HashSet(axioms));
    }

    @Test
    @DisplayName("read labels in English")
    void canReadLabelInEnglish() {
      OWLDataFactory df = ontologyManager.getOWLDataFactory();
      OWLNamedIndividual phyloref =
          df.getOWLNamedIndividual(IRI.create("http://example.org/phyloref1"));
      Set<String> englishLabels = OWLHelper.getLabelsInEnglish(phyloref, testOntology);

      assertEquals(1, englishLabels.size());
      assertTrue(
          englishLabels.contains("Label in English"),
          "Label 'Label in English' correctly identified.");
    }
  }
}