(ns nsign.unstable.io.http.tasks.codox
  (:require [codox.main :refer [generate-docs]]))

(defn -main []
  (generate-docs
    {:name "unstable.io.http"
     :version "" ; https://github.com/weavejester/codox/pull/183
     :source-uri "https://gitlab.com/nsign/unstable.io.http/blob/{git-commit}/{filepath}#L{line}"
     :output-path "public"}))
