(ns drumcirclr.connections
  (:import java.util.UUID
           java.util.concurrent.atomic.AtomicInteger)
  (:use lamina.core
        aleph.formats)
  (:require [clojure.contrib.logging :as log]))

; Connections mapped by UUID
(def conns (ref {}))
(def msg-count (AtomicInteger.))

(def broadcast (permanent-channel))

(defn connected-count [] (count @conns))

(defn dispatch-messages
  "Handles incoming messages from a conn"
  [{:keys [id ch]}]
  (receive-all ch
    (fn [raw-msg]
      (.getAndIncrement msg-count)
      (try
        (let [msg (decode-json raw-msg)
              {cmd :cmd}       msg
              tagged-msg       (assoc msg :user-id (str id))]
          (cond (= "play" cmd)
                  (enqueue broadcast (encode-json->string tagged-msg))
                (= "get-user-id" cmd)
                  (enqueue ch (encode-json->string {:cmd :set-user-id :user-id (str id)}))
                :else
                  (log/warn (format "Unknown message: %s" msg))))
        (catch Exception e (log/warn (str "Caught msg exception: " e)))))))

(defn remove-conn
  "Removes a connection from global list"
  [id]
  (log/info (format "Removing connection %s" id))
  (dosync
    (alter conns dissoc id)))

(defn add-conn
  "Add a new connection to the global list"
  [ch headers]
  (let [id (java.util.UUID/randomUUID)
        conn {:id id :ch ch :headers headers}]
    (log/info (format "Adding connection %s" id))
    (dosync
      (alter conns assoc id conn))
    (on-closed ch #(remove-conn id))
    (dispatch-messages conn)
    (siphon broadcast ch)
    conn))