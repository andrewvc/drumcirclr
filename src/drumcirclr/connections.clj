(ns drumcirclr.connections
  (:import java.util.UUID)
  (:use lamina.core)
  (:require [clojure.contrib.logging :as log]))

; Connections mapped by UUID
(def conns (ref {}))

(def broadcast (permanent-channel))

(defn connected-count [] (count @conns))

(defn dispatch-messages
  "Handles incoming messages from a conn"
  [{:keys [id ch]}]
  (receive-all ch
    (fn [msg]
      (log/info (format "Client sent message %s" msg)))))

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
    (siphon ch broadcast)
    (siphon broadcast ch)
    conn))