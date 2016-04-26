(ns rdf-path-examples.util
  (:require [clojure.java.io :as io]))

(defn average
  "Compute average of numbers in collection `coll`."
  [coll]
  (if (seq coll)
    (/ (apply + coll)
       (count coll))
    0))

(def duration-regex
  "Regular expression matching xsd:duration."
  #"^-?P(\d+Y)?(\d+M)?(\d+D)?T?(\d+H)?(\d+M)?(\d+(\.\d+)?S)?$")

(defn parse-number
  "Reads a number from a string. Returns nil if not a number.
  Taken from <http://stackoverflow.com/a/12285023/385505>."
  [s]
  (when (and s (re-find #"^-?\d+\.?\d*$" s))
    (read-string s)))

(def resource->input-stream (comp io/input-stream io/resource))

(def resource->string (comp slurp io/resource))

(defn try-times*
  "Try @body for number of @times. If number of retries exceeds @times,
  then exception is raised. Each unsuccessful try is followed by sleep,
  which increase in length in subsequent tries."
  [times body]
  (loop [n times]
    (if-let [result (try [(body)]
                         (catch Exception ex
                           (if (zero? n)
                             (throw ex)
                             (Thread/sleep (-> (- times n)
                                               (* 2000)
                                               (+ 1000))))))]
      (result 0)
      (recur (dec n)))))

; ----- Macros -----

(defmacro try-times
  "Executes @body. If an exception is thrown, will retry. At most @times retries
  are done. If still some exception is thrown it is bubbled upwards in the call chain.
  Adapted from <http://stackoverflow.com/a/1879961/385505>."
  [times & body]
  `(try-times* ~times (fn [] ~@body)))
