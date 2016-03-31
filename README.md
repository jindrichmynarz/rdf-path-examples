# rdf-path-examples

A web service that generates examples of RDF paths retrieved via a SPARQL endpoint.

## Usage

Send an RDF path serialized in [JSON-LD](http://json-ld.org) in the body of a POST request. Use the `Content-Type` header to indicate the MIME type of the POST's body (i.e., `application/ld+json`). Provide the `Accept` header to specify the preferred MIME type of the response. Currently, only JSON-LD (i.e. `application/ld+json`) is supported.

Configuration is sent using the query string parameters. The parameters must be URL-encoded. Supported parameters are:

* `sparql-endpoint`: URL of the SPARQL endpoint to query. This is a mandatory parameter.
* `graph-iri`: IRI of the named graph to query. If no value is provided, the default graph is queried.
* `selection-method`: ID of the chosen method for selecting examples. Supported values are `random` for random     selection, `distinct` for distinct selection, and `representative` for representative selection. This is a         mandatory parameter.
* `limit`: Positive integer indicating the maximum number of examples requested in the response. The response may  contain fewer examples (even none), if data in the queried SPARQL endpoint cannot satisfy the RDF path in the      request's body. If no value is provided, the default value 5 is used.
* `sampling-factor`: Positive integer that is used as the multiplier of `limit` when a selection method that uses sampling is chosen. If no value is provided, the default value 20 is used.

## License

Copyright © 2016 Jindřich Mynarz

Distributed under the Eclipse Public License version 1.0.
