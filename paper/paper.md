---
title: 'JPhyloRef: a tool for testing and resolving phyloreferences'
tags:
  - clade definitions
  - phyloreferences
  - Java
  - continuous integration testing
authors:
  - name: Gaurav Vaidya
    orcid: 0000-0003-0587-0454
    affiliation: "1, 2" # (Multiple affiliations must be quoted)
  - name: Nico Cellinese
    orcid: 0000-0002-7157-9414
    affiliation: 2
  - name: Hilmar Lapp
    orcid: 0000-0001-9107-0714
    affiliation: 3
affiliations:
 - name: Renaissance Computing Institute, University of North Carolina, Chapel Hill, NC, USA
   index: 1
 - name: Florida Museum of Natural History, University of Florida, Gainesville, FL, USA
   index: 2
 - name: Center for Genomic and Computational Biology, Duke University, Durham, NC, USA
   index: 3
date: 15 April 2021
bibliography: paper.bib
---

# Background and Overview

In evolutionary biology, groups of organisms consisting of an ancestor and all of its descendants ("clades") are a fundamental unit for understanding evolution and describing biodiversity [@De_Queiroz2007-xm]. Phylogenetic clade definitions define clades based on shared ancestry, providing the theoretical foundation for the semantics of taxon concepts to be defined and reproducibly resolved within a hypothesis of evolutionary relationships, i.e., a phylogeny [@De_Queiroz1990-ho; @De_Queiroz1992-vy; @De_Queiroz1992-oq]. We have proposed a mechanism, called Phyloreferencing, for representing phylogenetic clade definitions as structured data with fully machine-processable semantics, using the Web Ontology Language (OWL) [@W3C_OWL_Working_Group2012-we]. We refer to such machine-interpretable clade definitions as "phyloreferences" [@Cellinese2021-gp].

Because phyloreferences have an OWL ontology representation in the form of logically defined classes, combining them with an OWL representation of a phylogenetic tree allows an OWL reasoner to infer, based on their logical definitions, which nodes in the phylogeny, if any, instantiate each phyloreference. We refer to this logical inference as "resolving a phyloreference". Phylogenetic clade definitions are normally created and published in the form of natural language text, and in the context of a phylogenetic hypothesis, the so-called reference phylogeny. When digitized, usually through manual curation, to a structured, machine-interpretable representation, which we call a phyloreference, a key question in quality control of both the digitization product and the ontologies and logical expression algorithm it uses arises from one of the founding premises of phyloreferences: given that phyloreferences make the semantics of their represented clade machine-interpretable, can automated computational testing check whether a phyloreference resolves, and only resolves to the node in the reference phylogeny which the original authors of the published phylogenetic clade definition designate as the expected node.

# Commands and Design

To achieve this, we built JPhyloRef, a command-line tool in Java. When run with the `test` command with an OWL ontology containing phyloreferences as well as reference phylogenies as input, JPhyloRef identifies all the phyloreferences within this ontology, compares for each one the inferred phylogeny node(s) to the expected one, and determines success (inferred node identical to expected) or failure (no inferred node, or inferred node different from expected). To better facilitate use in automated continuous integration testing, JPhyloRef also allows for phyloreferences to be marked as draft (resulting in an "incomplete" test status), or as to be skipped (by no nodes having been annotated as an expected resolution of a particular phyloreference). These test results are reported in the Test Anything Protocol (TAP) format (https://testanything.org/), a cross-platform format for reporting test results. JPhyloRef will also return an exit code indicating success (0), failure (the number of failed phyloreferences as a positive integer) or an ontology containing no phyloreferences (-1, interpreted as 255 by POSIX-compatible operating systems).

Internally, JPhyloRef relies on an external OWL reasoner (which can be configured) to perform the actual OWL inferences, with which it communicates using the OWL API 4.2.7 [@Horridge2011-fu]. We strongly recommend to use ELK [@Kazakov2014-uk], a reasoner for the OWL-EL profile, which is sufficient for the inferences necessary for phyloreference resolution. In our experience, reasoners for more expressive profiles, such as OWL-DL reasoners, do not perform efficiently enough even for modestly sized ontologies of phyloreferences.

To support integrating Phyloreferencing with other tools, JPhyloRef includes two additional commands: `resolve` and `webserver`. Using the `resolve` command with an input of an OWL ontology in any of the supported formats will result in a JSON object whose keys are the IRIs of all the phyloreferences in the ontology that resolved to any nodes, and whose values are lists of the IRIs of the phylogeny nodes that they resolved to. Using the `webserver` command starts an HTTP server on the hostname and TCP port specified in the command line arguments. This webserver provides a POST endpoint at `/reason` that can be used to submit OWL ontologies in JSON-LD format for reasoning, which returns a JSON object in the same format as produced by the `resolve` command. Neither of these commands return test statuses for phyloreferences.

JPhyloRef is currently in active use for integrating the digitization of phylogenetic clade definitions in the form of phyloreferences into an automated testing framework, as well as for resolving phyloreferences on the command line. For example, we use it in the continuous integration testing infrastructure of the phyx.js JavaScript library (https://github.com/phyloref/phyx.js/actions), where the output from the `test` command is interpreted by a JavaScript-based TAP library to ensure that phyloreferences in example OWL ontology files resolve as expected. We also use the `webserver` command of JPhyloRef as a backend for the Klados phyloreference curation software (https://github.com/phyloref/klados), allowing the curator to submit phyloreferences created from user input or a publication, along with associated phylogenies, to a backend server running JPhyloRef for resolution. The source code for JPhyloRef has been archived to Zenodo [@gaurav_vaidya_2021_4697965].

# Statement of Need

JPhyloRef is a command line tool as well as a web service for reasoning with ontologies containing logical definitions of phylogenetic clade definitions, called phyloreferences, and their accompanying reference phylogenetic trees. It has two main goals. The primary one is to facilitate automated testing that the semantics of the logical definitions imply ("resolve to") the correct nodes in the reference tree as clade ancestors. This is key in supporting quality control for the digitization of phylogenetic clade definitions from natural language text to a structured machine-interpretable representation. It also verifies that one of the theoretical foundational premises of phyloreferences, computational reproducibility, holds in practice. The secondary goal is to enable integration with external tools that need to obtain the clade ancestor node(s) resulting from a given ontology of phyloreferences and reference tree(s). When run as part of an automated testing workflow, JPhyloRef reports test results in the cross-platform Test Anything Protocol (TAP) format. When used to find clade ancestor nodes implied by logical clade definitions, results are returned as a JSON object. JPhyloRef uses the OWL API reference library for reading Web Ontology Language (OWL) ontologies, and for the actual ontology reasoning step it uses an external and configurable OWL reasoner.

# Acknowledgements

This work was funded by the US National Science Foundation through collaborative grants [DBI-1458484] and [DBI-1458604] to Hilmar Lapp (Duke University) and Nico Cellinese (University of Florida), respectively.

# References

  [DBI-1458484]: http://www.nsf.gov/awardsearch/showAward?AWD_ID=1458484
  [DBI-1458604]: http://www.nsf.gov/awardsearch/showAward?AWD_ID=1458604
