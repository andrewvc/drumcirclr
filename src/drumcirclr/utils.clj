(ns drumcirclr.utils
  (:use
    aleph.formats
    clojure.contrib.condition)
  (:import java.util.HashMap)
  (:require
    [aleph.http :as http]
    [clojure.contrib.logging :as log]))


(defn string-map->keyword-map
  "Convert a map with string keys to one with keyword keys"
  [smap]
  (reduce (fn [m [k v]] (assoc m k v))
          {}
          (map #( [(keyword %1) (smap %1)] ) (keys smap))))

(defn remote-json
  ([url]
    (remote-json url {:method :get}))
  ([url http-opts]
  (let [response (http/sync-http-request (merge http-opts {:url url}))
        status   (response :status)]
    (cond
      (= 200 status)
        (string-map->keyword-map (decode-json response))
      (= 404 status)
        (raise :http-status :missing)
      :else
        (do (log/error (str "Error, received unexpected status ")
            (raise :http-status :unknown)))))))
