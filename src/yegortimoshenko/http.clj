(ns yegortimoshenko.http
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.net HttpCookie HttpURLConnection URL URLEncoder]
           [java.io IOException]))

(defn request
  ([url] (request {} url))
  ([{:keys [body cookies headers method]} url]
   (let [conn ^HttpURLConnection (.openConnection (io/as-url url))]
     (.setRequestMethod conn (or method "GET"))
     (if cookies
       (.setRequestProperty conn "Cookie" (str/join \; (map str cookies))))
     (doseq [[k v] headers]
       (.setRequestProperty conn k v))
     (if body (.setDoOutput conn true))
     (try (.connect conn)
          (if body (io/copy body (.getOutputStream conn)))
          {:body (.getInputStream conn)
           :headers (.getHeaderFields conn)
           :status (.getResponseCode conn)}
          (catch IOException e
            {:body (.getErrorStream conn)
             :headers (.getHeaderFields conn)
             :status (.getResponseCode conn)})))))

(defn query [m]
  (str/join \& (map (fn [vec] (str/join \= (map #(URLEncoder/encode (name %) "UTF-8") vec))) m)))

(defn cookies [resp]
  (let [headers (-> resp :headers (get "Set-Cookie"))]
    (reduce concat []
            (map (fn [h] (HttpCookie/parse h)) headers))))

(defn ok? [resp]
  (let [status (:status resp)]
    (and (>= status 200) (< status 300))))
