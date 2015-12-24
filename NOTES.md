# NOTES

## Specification

The library exposes a single function `generate_examples`:

```
rdf_path_examples.core.generate_examples(config, path, callback);
```

`config` is a JavaScript object:

```
var config = {
  "sparql-endpoint": "http://localhost:8890/sparql", // URL of a SPARQL endpoint
  "graph-iri": "http://example.com",                 // IRI of a named graph to query
  "selection-method": "random"                       // Identifier of a method to select examples 
};
```

`path` is an RDF path serialized in JSON-LD. For example, here is a path from `gr:BusinessEntity` to `xsd:string` via `foaf:page`:

```
{
  "@context": {
    "@vocab": "http://purl.org/lodsight/",
    "foaf": "http://xmlns.com/foaf/0.1/",
    "gr": "http://purl.org/goodrelations/v1#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
    "xsd": "http://www.w3.org/2001/XMLSchema#",
    "edge": {"@type": "@id"},
    "end": {"@type": "@id"},
    "start": {"@type": "@id"}
  },
  "@graph": [
    {
      "@id": "#e0",
      "@type": "gr:BusinessEntity"
    },
    {
      "@id": "#e1",
      "@type": "xsd:string"
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

```
callback(examples);
```

The `generate-examples()` can raise several exceptions:

* Invalid arguments, when validation of their schema fails.
* HTTP exception when SPARQL query fails. 

TODO: Define serialization of `examples`: r1 => p1 => r2 => p2 => r3, basic sorted array is probably good enough. 

Path invariants:
- Paths have an odd count of members.
- Paths have at least 3 members.

Path validation:
- 1 instance of `lodsight:Path`.
- Each instance of `lodsight:Path` must specify `lodsight:start`, `lodsight:edge`, and `lodsight:end`.
- Path has to be continuous. There can be only 1 `start` that is not used as `end`.
- Only the end node of a path can be a data type instance.
- Each start and end has `@type`.
- Input JSON-LD must have @graph. It is not syntactically valid to nest resources under @type. I.e. this is not valid:
  `"@type": {"@id": "gr:BusinessEntity"}`

Path sorting:
- Find the beginning path. This is the path that has `start` that does not figure as `end`.

## Technical

- Note that Clojurescript can cache JSONs for a long while (even after `lein clean`).

### Done

- Loading query templates via macros: <http://stackoverflow.com/a/23769146/385505>
- Use Mustache for query templates
- When executing SPARQL queries, first compare SPARQL endpoint's URL with the origin URL. When the URLs share the domain, use a regular AJAX call. Otherwise, use JSON-P.
  - It will be always a different origin. For example, if it was on the same server, it would likely be running on a different port, therefore creating a different origin.
- First implement random selection method.
- Implement path validation.
- Make the dependency on JSONLD.js work.
- Use Prismatic's Schema to validate the arguments of the `loadExamples()` function. 
  - Use JSON coercion? <http://prismatic.github.io/schema/schema.coerce.html#var-json-coercion-matcher>

### To do

- Other selection method will be based on the random selection of a small sample (e.g., 100 path instantiations), for which descriptions will be retrieved (up to a configurable number of hops, default = 1). There will be an inner SELECT query to randomly select path instantiations, while the outer CONSTRUCT query will retrieve descriptions of the instantiations.
- Move test.check to test dependencies. How to do that?
  - See :profiles.
