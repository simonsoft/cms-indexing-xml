/**
 * Delivers XML elements to a listener, primarily for indexing.
 * 
 * Keeps the original source as verbatim as possible,
 * and each element parseable as xml,
 * so tailored post processing can be done before indexing
 * and/or using copy-field with different tokenizers/analyzers in solr.
 */
package se.simonsoft.xmltracking.source;
