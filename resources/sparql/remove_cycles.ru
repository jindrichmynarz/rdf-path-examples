PREFIX : <http://non-existent.ns/>

DELETE {
  ?mid ?p ?s .
}
WHERE {
  ?s (!:p)* ?mid .
  ?mid ?p ?s .
}
