# rdf-path-examples

This is a prototype of a Clojurescript library that generates example instantiations of RDF paths retrieved via SPARQL endpoint.

The library exposes a single function `generate_examples` in the `rdf_path_examples.core` namespace:

```js
rdf_path_examples.core.generate_examples.generate_examples(config, path, callback);
```

`config` is a JavaScript object:

```js
var config = {
  "sparql-endpoint": "http://localhost:8890/sparql", // URL of a SPARQL endpoint
  "graph-iri": "http://example.com",                 // IRI of a named graph to query
  "selection-method": "random"                       // Identifier of the method for selecting examples 
};
```

`sparql-endpoint` attribute must provide a URL of a SPARQL endpoint allows to retrieve query results serialized in JSON and JSON-LD via JSON-P (e.g., [OpenLink Virtuoso](https://github.com/openlink/virtuoso-opensource)). `graph-iri` is the IRI of the named graph from which instances of the provided RDF path will be retrieved. The only currently supported selection method is `random`, which simply retrieves random examples of the supplied path's instantiations.

`path` is a JavaScript object representing an RDF path serialized in [JSON-LD](http://json-ld.org/) using the [RDF Path vocabulary](https://github.com/jindrichmynarz/rdf-path-examples/blob/master/resources/rdf_path.ttl). For example, here is a path from `gr:BusinessEntity` to `xsd:string` via `foaf:page`:

```js
{
  "@context": {
    "@vocab": "http://purl.org/lodsight/rdf-path#",
    "foaf": "http://xmlns.com/foaf/0.1/",
    "gr": "http://purl.org/goodrelations/v1#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
    "xsd": "http://www.w3.org/2001/XMLSchema#",
    "rdfs:subClassOf": {"@type": "@id"},
    "edge": {"@type": "@id"},
    "end": {"@type": "@id"},
    "start": {"@type": "@id"}
  },
  "@graph": [
    {
      "@id": "#e0",
      "rdfs:subClassOf": "gr:BusinessEntity"
    },
    {
      "@id": "#e1",
      "rdfs:subClassOf": "xsd:string"
    },
    {
      "@type": "Path",
      "start": "#e0",
      "edge": "foaf:page",
      "end": "#e1"
    },
    {
      "@id": "gr:BusinessEntity",
      "@type": "rdfs:Class"
    },
    {
      "@id": "xsd:string",
      "@type": "rdfs:Datatype"
    }
  ]
}
```

`callback` is a function that is called with the generated examples as its argument:

```js
callback(examples);
```

`examples` are formatted as an array of arrays. Each example is represented as an array. The array contains path nodes interleaved with path edges. The order of items in the array represents the order of the path steps. For example, 2 examples for the above-mentioned path may look like the following:

```js
var examples = [
  ["http://linked.opendata.cz/resource/business-entity/7d179039-1f42-4559-90ea-8a624606e663",
   "http://xmlns.com/foaf/0.1/page",
   "http://www.strabag.cz"],
  ["http://linked.opendata.cz/resource/business-entity/b1672053-e4d6-40fb-b76b-1393a8e1390f",
   "http://xmlns.com/foaf/0.1/page",
   "www.mfcr.cz"]
];
```

If we consider the first example, then `http://linked.opendata.cz/resource/business-entity/7d179039-1f42-4559-90ea-8a624606e663` and `http://www.strabag.cz` are the path's nodes, while `http://xmlns.com/foaf/0.1/page` is the path's edge. Edges are always represented as absolute URIs. Nodes can be either absolute URIs or literals, such as numbers.

## License

Copyright © 2015 Jindřich Mynarz

Distributed under the Eclipse Public License version 1.0. 
