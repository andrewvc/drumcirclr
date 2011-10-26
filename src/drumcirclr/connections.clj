(ns drumcirclr.connections
  (:import java.util.UUID
           java.util.concurrent.atomic.AtomicLong)
  (:use lamina.core
        aleph.formats)
  (:require [clojure.contrib.logging :as log]
            [drumcirclr.sequencer :as sequencer]))

; Connections mapped by UUID
(def conns (ref {}))
(def msg-count (AtomicLong.))
(def broadcast (permanent-channel))

(defn connected-count [] (count @conns))

; Dump this all to the console for logging
(receive-all broadcast
             (fn [m] (log/debug (format "Broadcast: %s" m))))

(defn broadcast-beat
  "Sends the current beat info to broadcast"
  [current-beat]
  (cond
    (= 2 (:beat current-beat))
    (enqueue broadcast
      (encode-json->string
        (assoc (sequencer/beat-snapshot) :cmd "setAllSamples")))))

(sequencer/start-metronome broadcast-beat)

(defn route-command
  "Route a msg based on the command name"
  [msg ch]
  (let [{:keys [cmd user-id]} msg]
    (cond
      (= "play" cmd)
        (enqueue broadcast (encode-json->string msg))
      (= "setSamples" cmd)
        (sequencer/set-user-samples user-id (:samples msg))
      (= "get-user-id" cmd)
        (enqueue ch (encode-json->string {:cmd :set-user-id :user-id (str user-id)}))
      :else
        (log/warn (format "Unknown message: %s" msg)))))

(defn dispatch-messages
  "Handles incoming messages from a conn"
  [{:keys [id ch]}]
  (receive-all ch
    (fn [raw-msg]
      (cond
        (nil? raw-msg) (log/warn (format "Received nil message: %s" raw-msg))
        :else
        (try
          (.getAndIncrement msg-count)
          (cond
            (nil? raw-msg)
              (log/info "Empty message encountered")
            :else
              (let [parsed-msg    (decode-json raw-msg)
                    msg-with-user (assoc parsed-msg :user-id (str id))]
                   (route-command msg-with-user ch)))
          (catch Exception e
                 (log/warn (str "Caught msg exception: " e))
                 (.printStackTrace e)))))))

(defn remove-conn
  "Removes a connection from global list"
  [user-id]
  (log/info (format "Removing connection %s" user-id))
  (dosync
    (alter conns dissoc user-id)
    (sequencer/delete-user-samples user-id)))

(defn add-conn
  "Add a new connection to the global list"
  [ch headers]
  (let [id (java.util.UUID/randomUUID)
        conn {:id id :ch ch :headers headers}]
    (log/info (format "Adding connection %s" id))
    (dosync (alter conns assoc id conn))
    (on-closed ch #(remove-conn id))
    (dispatch-messages conn)
    (siphon broadcast ch)
    conn))