(ns yegortimoshenko.io.http.spec
  (:require [yegortimoshenko.io.http :as http]
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

(s/def ::header-name
  (s/and string? (comp not str/blank?) (partial not-any? #{\:})))

(s/def ::request/headers (s/map-of ::header-name (s/coll-of string?)))
(s/def ::request/method #{:GET :POST :HEAD :OPTIONS :PUT :DELETE :TRACE})

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

(s/def ::request/url
  (s/with-gen
    (s/and (s/conformer #(try (URL. (str %)) (catch MalformedURLException _ ::s/invalid)))
           #(contains? #{"http" "https"} (.getProtocol %))
           #(not (str/blank? (.getHost %))))
    #(gen/fmap
      (fn [[url path]] (str/join \/ (cons url path)))
      (gen/tuple (s/gen alexa-top) (gen/list (gen/string-alphanumeric))))))

(s/def ::request
  (s/and (s/keys :req-un [::request/url]
                 :opt-un [::request/body ::request/headers ::request/method])
         #(not (and (:body %) (contains? #{:GET :HEAD :TRACE nil} (:method %))))))

(shorthand 'response)

(s/def ::response/body ::input-stream)

(s/def ::response/headers
  (s/with-gen
    (s/and (s/conformer (partial map (fn [[k v]] [k (vec v)])))
           (s/every-kv (s/nilable ::header-name) (s/coll-of string?)))
    #(gen/fmap (fn [m] (Collections/unmodifiableMap m))
               (gen/map (s/gen ::header-field)
                        (gen/fmap (fn [v] (Collections/unmodifiableList v))
                                  (gen/vector (gen/string-ascii)))))))

(s/def ::response/status (s/int-in 100 600))
(s/def ::response (s/keys :req-un [::response/body ::response/headers ::request ::response/status]))

(s/fdef http/request
  :args (s/cat :request ::request)
  :ret ::response)

(s/def ::cookie
  (s/with-gen
    #(instance? HttpCookie %)
    #(gen/fmap
      (fn [[n v]] (HttpCookie. n v))
      (gen/tuple (gen/not-empty (gen/string-alphanumeric)) (gen/string-alphanumeric)))))

(s/def ::cookies (s/coll-of ::cookie))

(s/fdef http/>cookies
  :args (s/cat :request ::request :cookies ::cookies)
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
