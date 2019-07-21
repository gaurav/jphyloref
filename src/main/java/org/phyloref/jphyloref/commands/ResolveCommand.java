package org.phyloref.jphyloref.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.eclipse.rdf4j.rio.RDFParser;
import org.json.JSONObject;
import org.phyloref.jphyloref.helpers.JSONLDHelper;
import org.phyloref.jphyloref.helpers.PhylorefHelper;
import org.phyloref.jphyloref.helpers.ReasonerHelper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * Resolve an ontology of phyloreferences provided on the command line, and report on the resolution
 * of each clade definition. The resulting report or an appropriate error message will be provided
 * in JSON, and the exit code will be set to non-zero if an error occurred.
 *
 * <p>This code was extracted from WebserverCommand so its functionality can be used from the
 * command line, which is while for now it returns its results in the JSON format. We might
 * eventually want to switch over to YAML or another more command line friendly format.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ResolveCommand implements Command {
  /**
   * This command is named "resolve". It should be invoked as "java -jar jphyloref.jar resolve ..."
   */
  @Override
  public String getName() {
    return "resolve";
  }

  /**
   * A description of the Resolve command.
   *
   * @return A description of this command.
   */
  @Override
  public String getDescription() {
    return "Resolve phyloreferences in the input ontology and report on their resolution in JSON.";
  }

  /**
   * Add command-line options specific to this command.
   *
   * @param opts The command-line options to modify for this command.
   */
  @Override
  public void addCommandLineOptions(Options opts) {
    opts.addOption(
        "i",
        "input",
        true,
        "The input ontology to read in RDF/XML or JSON-LD (can also be provided without the '-i').");

    opts.addOption(
        "j",
        "jsonld",
        false,
        "Treat the input file as a JSON-LD file. Files with a '.json' or '.jsonld' extension will automatically be treated as a JSON-LD file.");
  }

  /** Use a default base URI when reading JSON-LD file. */
  private static final String DEFAULT_URI_PREFIX = "http://example.org/jphyloref";

  /**
   * Set up a webserver to listen on the provided hostname and port (or their defaults).
   *
   * @param cmdLine The command line options provided to this command.
   */
  @Override
  public int execute(CommandLine cmdLine) throws RuntimeException {
    // Extract command-line options
    String inputFilename = cmdLine.getOptionValue("input");

    if (inputFilename == null && cmdLine.getArgList().size() > 1) {
      // No 'input'? Maybe it's just provided as a left-over option?
      inputFilename = cmdLine.getArgList().get(1);
    }

    if (inputFilename == null) {
      throw new IllegalArgumentException("Error: no input ontology specified (use '-i input.owl')");
    }

    // If the input filename is '-', we should read the ontology from STDIN instead.
    InputStream inputStreamToReadFrom = null;
    if (inputFilename.equals("-")) {
      inputStreamToReadFrom = System.in;
    } else {
      try {
        inputStreamToReadFrom = new FileInputStream(inputFilename);
      } catch (FileNotFoundException ex) {
        System.err.println("Could not open input file '" + inputFilename + "': " + ex);
        return 1;
      }
    }

    // Report the name of the file being tested.
    System.err.println("Input: " + inputFilename);

    // Set up an OWL Ontology Manager to work with.
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    // Is purl.obolibrary.org down? No worries, you can access local copies
    // of your ontologies in the 'ontologies/' folder.
    AutoIRIMapper mapper = new AutoIRIMapper(new File("ontologies"), true);
    System.err.println("Found local ontologies: " + mapper.getOntologyIRIs());
    manager.addIRIMapper(mapper);

    // Is this a JSON or JSON-LD file?
    OWLOntology ontology;
    String inputFileLowercase = inputFilename.toLowerCase();
    try {
      if (cmdLine.hasOption("jsonld")
          || inputFileLowercase.endsWith(".json")
          || inputFileLowercase.endsWith(".jsonld")) {
        // Use the JSONLD Helper to load the ontology.
        ontology = manager.createOntology();
        RDFParser parser = JSONLDHelper.createRDFParserForOntology(ontology);

        // Read from the provided input stream (either STDIN or a file).
        parser.parse(inputStreamToReadFrom, DEFAULT_URI_PREFIX);

      } else {
        // Load the ontology using OWLManager, by reading from the provided
        // input stream (either STDIN or a file).
        ontology = manager.loadOntologyFromOntologyDocument(inputStreamToReadFrom);
      }
    } catch (OWLOntologyCreationException ex) {
      System.err.println("Could not create ontology '" + inputFilename + "': " + ex);
      return 1;
    } catch (IOException ex) {
      System.err.println("Could not read and load ontology '" + inputFilename + "': " + ex);
      return 1;
    }

    // Ontology loaded.
    System.err.println("Loaded ontology: " + ontology);

    // We have an ontology! Let's reason over it, and store the results as
    // a map of a list of node IRIs matched by each phyloref IRI.
    Map<String, Set<String>> nodesPerPhylorefAsString = new HashMap<>();

    // Set up and start the reasoner.
    OWLReasonerFactory factory = ReasonerHelper.getReasonerFactoryFromCmdLine(cmdLine);
    OWLReasoner reasoner = factory.createReasoner(ontology);

    // Go through all the phyloreferences, identifying all the nodes that have
    // matched to that phyloreference.
    for (OWLClass phyloref : PhylorefHelper.getPhyloreferences(ontology, reasoner)) {
      IRI phylorefIRI = phyloref.getIRI();

      // Identify all individuals contained in this phyloref class, but filter out
      // everything that is not an IRI_CDAO_NODE.
      Set<String> nodes =
          reasoner
              .getInstances(phyloref, false)
              .getFlattened()
              .stream()
              // This includes the phyloreference itself. We only want to
              // look at phylogeny nodes here. So, let's filter down to named
              // individuals that are asserted to be cdao:Nodes.
              .filter(
                  indiv ->
                      EntitySearcher.getTypes(indiv, ontology)
                          .stream()
                          .anyMatch(
                              type ->
                                  (!type.getClassExpressionType()
                                          .equals(ClassExpressionType.OWL_CLASS))
                                      || type.asOWLClass()
                                          .getIRI()
                                          .equals(PhylorefHelper.IRI_CDAO_NODE)))
              .map(indiv -> indiv.getIRI().toString())
              // Strip the default prefix on the node URI if present.
              .map(iri -> iri.replaceFirst("^" + DEFAULT_URI_PREFIX, ""))
              .collect(Collectors.toSet());

      // Strip the default prefix on the phyloref URI if present.
      String nodeURI = phylorefIRI.toString();
      nodeURI = nodeURI.replaceFirst("^" + DEFAULT_URI_PREFIX, "");

      nodesPerPhylorefAsString.put(nodeURI, nodes);
    }

    // Dispose of the reasoner.
    reasoner.dispose();

    // Write the JSON response to STDOUT.
    System.out.println(new JSONObject(nodesPerPhylorefAsString).toString());
    return 0;
  }
}