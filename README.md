# rdf-path-examples

This is a prototype of a Clojurescript library that generates example instantiations of RDF paths retrieved via SPARQL endpoint.

The library exposes a single function `generate_examples` in the `rdf_path_examples.core` namespace:

```js
rdf_path_examples.core.generate_examples(config, path, callback);
```

`config` is a JavaScript object:

```js
var config = {
  "sparql-endpoint": "http://localhost:8890/sparql", // URL of a SPARQL endpoint
  "graph-iri": "http://example.com",                 // IRI of a named graph to query
  "selection-method": "random",                      // Identifier of the method for selecting examples 
  "limit": 5                                         // Number of examples to retrieve
};
```

`sparql-endpoint` attribute must provide a URL of a SPARQL endpoint allows to retrieve query results serialized in NTriples. Note that querying the endpoint's URL must be allowed by the [same-origin policy](https://developer.mozilla.org/en-US/docs/Web/Security/Same-origin_policy) or [cross-origin access](https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS) must be enabled. `graph-iri` is the IRI of the named graph from which instances of the provided RDF path will be retrieved. The only currently supported selection method is `random`, which simply retrieves random examples of the supplied path's instantiations. The configuration can optionally provide the `limit` attribute that specifies the number of examples to be retrieved. This attribute defaults to 5.

`path` is a JavaScript object representing an RDF path serialized in [JSON-LD](http://json-ld.org/) using the [RDF Path vocabulary](https://w3id.org/lodsight/rdf-path). For example, here is a path from `gr:BusinessEntity` to `xsd:string` via `foaf:page`:

```js
{
  "@context": {
    "@vocab": "https://w3id.org/lodsight/rdf-path#",
    "foaf": "http://xmlns.com/foaf/0.1/",
    "gr": "http://purl.org/goodrelations/v1#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
    "xsd": "http://www.w3.org/2001/XMLSchema#",
    "edgeProperty": {"@type": "@id"},
    "edges": {"@container": "@list"}
  },
  "@graph": [{
    "@type": "Path",
    "edges": [{
      "@type": "Edge",
      "start": {"@type": "gr:BusinessEntity"},
      "edgeProperty": "foaf:page",
      "end": {"@type": "xsd:string"}
    }]},
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

The path needs to contain 1 instance of the `:Path` class. The path must have 1 or more values of the `:edges` property wrapped as a list (i.e. instance of `rdf:Seq`). The edges constituting a path are instances of the `:Edge` class. Each edge has a `:start` and `:end` nodes and `:edgeProperty` that denotes their relation. Start and end nodes must instantiate either a class or data type. Classes must explicitly instantiate `rdfs:Class`, while data types must instantiate `rdfs:Datatype`. 

`callback` is a function that is called with the generated examples as its argument:

```js
callback(examples);
```

`examples` are too represented using JSON-LD. For example, 2 examples for the above-mentioned path may look like the following:

```js
{
  "@context": {
    "@vocab": "https://w3id.org/lodsight/rdf-path",
    "foaf": "http://xmlns.com/foaf/0.1/",
    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "start": {"@type": "@id"},
    "edgeProperty": {"@type": "@id"},
    "edges": {"@container": "@list"}
  },
  "@graph": [
    {
      "@type": "Path",
      "edges": [{
        "@type": "Edge",
        "start": "http://linked.opendata.cz/resource/business-entity/ce177c67-6be4-4f26-90ea-c267466d0dfd",
        "edgeProperty": "foaf:page",
        "end": "http://www.fnkv.cz"
      }]
    },
    {
      "@type": "Path",
      "edges": [{
        "@type": "Edge",
        "start": "http://linked.opendata.cz/resource/business-entity/81c5edb3-ac84-47c3-9a3c-f3fe5c492cd1",
        "edgeProperty": "foaf:page",
        "end": "www.colas.cz"
      }]
    }
  ]
};
```

If no path examples match the provided path, then an empty array is returned.

## Tests

You can run tests using [lein-doo](https://github.com/bensu/doo), for example by `lein doo phantom test once`.

## License

Copyright © 2015 Jindřich Mynarz

Distributed under the Eclipse Public License version 1.0. 
