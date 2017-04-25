(ns yegortimoshenko.io.http
  "HTTP/1.1 client based on HttpURLConnection"
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io File InputStream IOException)
           (java.net HttpCookie HttpURLConnection URL URLEncoder)))

(defn ^:private header->keyword [h]
  (keyword (str/lower-case h)))

(defn ^:private keyword->header [kw]
  (str/join \- (map str/capitalize (str/split (name kw) #"-"))))

(defn ^:private normalize-headers [map]
  (into {} (for [[k v] map] (if k [(header->keyword k) (vec v)]))))

(defn ^:private ->url [x]
  (if (instance? URL x) x (URL. x)))

(defn request
  "Sends a HTTP request and returns a response map that contains
  :body (InputStream), :headers (map from keyword to collection
  of strings), and :status (in range 100-600).

  Takes a HTTP/HTTPS URL and optionally a map of parameters:
    :body     byte[], char[], File, InputStream, Reader or String
    :encoding if body is char[], String, or Reader, specifies encoding
              for conversion to bytes (default: UTF-8)
    :headers  map from keywords to collection of strings
    :length   body length, if known in advance; when missing, byte[]/File
              length is pre-calculated, char[]/Reader/String are buffered,
              InputStream is sent in chunks
    :method   keyword that represents HTTP method to be used; can be one of
              :GET, :POST, :HEAD, :PUT, :DELETE, :TRACE, :OPTIONS, by default
              it's either :GET when no body is present or POST otherwise

  HEAD and TRACE requests can't have a body, as per RFC 7231.
  Due to HttpURLConnection limitations, GET request with a body
  and CONNECT/PATCH methods are not supported."
  ([url] (request {} url))
  ([{:keys [body encoding headers length method]} url]
   (let [conn ^HttpURLConnection (.openConnection (->url url))]
     (when body
       (.setDoOutput conn true)
       (if-let [length (or length (if (instance? File body) (.length body)))]
         (.setFixedLengthStreamingMode conn length)
         (if (instance? InputStream body)
           (.setChunkedStreamingMode conn 0))))
     (doseq [[kw vs] headers]
       (let [h (keyword->header kw)]
         (doseq [v vs]
           (.addRequestProperty conn h v))))
     (if method (.setRequestMethod conn (name method)))
     (try (.connect conn)
          (if body (io/copy body (.getOutputStream conn) :encoding encoding))
          {:body (.getInputStream conn)
           :headers (normalize-headers (.getHeaderFields conn))
           :status (.getResponseCode conn)}
          (catch IOException e
            {:body (.getErrorStream conn)
             :headers (normalize-headers (.getHeaderFields conn))
             :status (.getResponseCode conn)})))))

(defn >cookies
  "Given request map and collection of cookies, adds a Cookie header to the request"
  ([cookies] (>cookies {} cookies))
  ([request cookies]
   (update-in request [:headers :cookie] concat (map str cookies))))

(defn <cookies
  "Returns a collection of HttpCookie instances, which is empty unless
   Set-Cookie header(s) are present in the response."
  [response]
  (->> (get-in response [:headers :set-cookie])
       (map #(HttpCookie/parse %))
       (reduce concat)))

(defn success?
  "Returns true if the response status code is in 2xx range"
  [{:keys [status]}]
  (<= 200 status 299))

(defn redirect?
  "Returns true if the response status code is in 3xx range"
  [{:keys [status]}]
  (<= 300 status 399))

(defn client-error?
  "Returns true if the response status code is in 4xx range"
  [{:keys [status]}]
  (<= 400 status 499))

(defn server-error?
  "Returns true if the response status code is in 5xx range"
  [{:keys [status]}]
  (<= 500 status 599))

(defn ^:private encode [s]
  (URLEncoder/encode s "UTF-8"))

(defn build-query
  "Takes an associative collection and returns a URL-encoded query string"
  [coll]
  (str/join \& (reduce (fn [q [k v]] (conj q (str (encode (name k)) \= (encode (str v))))) [] coll)))
