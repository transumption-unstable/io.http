(defproject com.yegortimoshenko/io.http "20170425.115747"
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]]
  :deploy-repositories {"sonatype" {:creds :gpg :url "https://oss.sonatype.org/service/local/staging/deploy/maven2"}}
  :description "HTTP/1.1 client based on HttpURLConnection"
  :license {:name "Internet Systems Consortium License"
            :url "https://github.com/yegortimoshenko/io.http/blob/master/LICENSE"}
  :plugins [[autodoc/lein-autodoc "1.1.1"]
            [lein-stamp "20170312.223701"]]
  :pom-addition [:developers
                 [:developer
                  [:name "Yegor Timoshenko"]
                  [:email "yegortimoshenko@gmail.com"]]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}}
  :repl-options {:init-ns yegortimoshenko.io.http}
  :scm {:url "git@github.com:yegortimoshenko/io.http.git"}
  :url "https://github.com/yegortimoshenko/io.http")
