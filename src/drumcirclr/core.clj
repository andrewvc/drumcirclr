(ns drumcirclr.core
  "The **NEW** *NEW* Boxy realtime service for Vokle"
	(:use compojure.core
        ring.middleware.stacktrace
        aleph.formats
        aleph.http
        ring.middleware.file-info
        ring.middleware.file
        ring.middleware.params
        ring.middleware.session
        lamina.core)
	(:require [drumcirclr.connections :as connections]
            [drumcirclr.sequencer :as sequencer]
            [clojure.contrib.logging :as log]
            [compojure.route :as route]
            [compojure.handler :as handler])
  (:import java.util.Calendar)
  (:require clojure.contrib.mock)
	(:gen-class
    :name drumcirclr.BoxyCore
    :methods [[startServer    [] void]]))

(defn respond-text [& body-parts]
  "Responds using text/plain"
  {:headers {"Content-Type:" "text/plain"}
   :body    (apply str body-parts)})

(defn respond-json
  "Responds with JSON generated from data passed in"
  ([data]
    (respond-json data nil))
  ([data extra]
    (merge {:status 200
           :headers {"content-type" "application/json"}
           :body (encode-json->string data)} extra)))

(defn wrap-logging [handler]
  "Log each request"
  (fn [{:keys [request-method uri params] :as req}]
    (let [start  (System/currentTimeMillis)
          resp   (handler req)
          finish (System/currentTimeMillis)
          total  (- finish start)]
      (println (format "-> %s %s (%dms): " request-method uri total params))
      resp)))

; Route actions

(defn handle-status
  "Respond with the current server status"
  []
  (respond-json {:status "OK"
                 :date   (str (.getTime (Calendar/getInstance)))
                 :msg-count (.get connections/msg-count)
                 :timestamp (System/currentTimeMillis)
                 :connections (connections/connected-count)
                 :next-measure @sequencer/next-measure}))

(defn handle-connect
  "Handle client websocket connection"
  [resp-ch headers]
  (connections/add-conn resp-ch headers))

(defroutes app-routes
  (GET "/" [] (slurp "resources/public/index.html"))
	(GET "/status" [] (handle-status))
  (GET "/connect" [] (wrap-aleph-handler handle-connect))
  (route/resources "/")
  (route/not-found "Page Not Found"))

(def ring-app (-> app-routes
                  (wrap-logging)
                  (wrap-params)
                  (wrap-session)
                  (wrap-stacktrace)))

(defn -startServer [this]
  (log/info "Starting server...")
	(start-http-server (wrap-ring-handler ring-app) {:port 3001 :websocket true})
	(log/info "Started"))

(defn -main [& args]
  (-startServer nil))