(ns nsign.unstable.io.http.spec
  (:require [nsign.unstable.io.http :as http]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.string :as str])
  (:import (java.io ByteArrayInputStream CharArrayReader File IOException InputStream Reader)
           (java.net HttpCookie MalformedURLException URL)
           (java.util Collections)))

(s/def ::chars
  (s/with-gen
    #(instance? (Class/forName "[C") %)
    #(gen/fmap (fn [s] (.toCharArray s)) (gen/string))))

(s/def ::file
  (s/with-gen
    #(instance? File %)
    #(gen/fmap (fn [[prefix suffix content]] (doto (File/createTempFile prefix suffix) (spit content)))
               (let [g (gen/such-that (fn [s] (<= 4 (count s) 64)) (gen/string-alphanumeric) 100)]
                 (gen/tuple g g (gen/string))))))

(s/def ::input-stream
  (s/with-gen
    #(instance? InputStream %)
    #(gen/fmap (fn [bs] (ByteArrayInputStream. bs)) (gen/bytes))))

(s/def ::reader
  (s/with-gen
    #(instance? Reader %)
    #(gen/fmap (fn [s] (CharArrayReader. (.toCharArray s))) (gen/string))))

(s/def ::headers (s/map-of keyword? (s/coll-of string?)))

(def ^:private alexa-top
  #{"https://www.google.com"
    "https://www.facebook.com"
    "https://www.youtube.com"
    "https://www.baidu.com"
    "https://www.wikipedia.org"
    "https://www.yahoo.com"
    "https://www.reddit.com"
    "https://www.google.co.in"
    "https://www.amazon.com"
    "https://www.google.co.jp"
    "https://vk.com"
    "https://www.instagram.com"
    "https://yandex.ru"})

(s/def ::url
  (s/with-gen
    (s/and (s/or :string string? :url #(instance? URL %))
           (s/conformer
            (fn [[k x]]
              (case k
                :string (try (URL. x) (catch MalformedURLException _ ::s/invalid))
                :url x)))
           #(contains? #{"http" "https"} (.getProtocol %))
           #(not (str/blank? (.getHost %))))
    #(gen/fmap
      (fn [[url path]] (str/join \/ (cons url path)))
      (gen/tuple (s/gen alexa-top) (gen/list (gen/string-alphanumeric))))))

(defn ^:private shorthand [s]
  (let [ns (symbol (str *ns* \. s))]
    (create-ns ns)
    (alias s ns)))

(shorthand 'request)

(s/def ::request/body
  (s/or :bytes bytes?
        :chars ::chars
        :file ::file
        :input-stream ::input-stream
        :reader ::reader
        :string string?))

(s/def ::request/method #{:GET :POST :HEAD :OPTIONS :PUT :DELETE :TRACE})

(s/def ::request
  (s/and (s/keys :opt-un [::request/body ::headers ::request/method])
         #(not (and (:body %) (contains? #{:GET :HEAD :TRACE} (:method %))))))

(shorthand 'response)

(s/def ::response/body ::input-stream)
(s/def ::response/status (s/int-in 100 600))
(s/def ::response (s/keys :req-un [::response/body ::headers ::response/status]))

(s/fdef http/request
  :args (s/cat :request (s/? ::request) :url ::url)
  :ret ::response)

(s/def ::cookie
  (s/with-gen
    #(instance? HttpCookie %)
    #(gen/fmap
      (fn [[n v]] (HttpCookie. n v))
      (gen/tuple (gen/not-empty (gen/string-alphanumeric)) (gen/string-alphanumeric)))))

(s/def ::cookies (s/coll-of ::cookie))

(s/fdef http/>cookies
  :args (s/cat :request (s/? ::request) :cookies ::cookies)
  :ret ::request)

(s/fdef http/<cookies
  :args (s/cat :response ::response)
  :ret ::cookies)

(s/fdef http/success?
  :args (s/cat :response ::response)
  :ret boolean?)

(s/fdef http/redirect?
  :args (s/cat :response ::response)
  :ret boolean?)

(s/fdef http/client-error?
  :args (s/cat :response ::response)
  :ret boolean?)

(s/fdef http/server-error?
  :args (s/cat :response ::response)
  :ret boolean?)
