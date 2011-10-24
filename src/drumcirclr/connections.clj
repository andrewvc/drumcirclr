(ns drumcirclr.connections
  (:import java.util.UUID
           java.util.concurrent.atomic.AtomicInteger
           java.util.Timer
           java.util.TimerTask)
  (:use lamina.core
        aleph.formats)
  (:require [clojure.contrib.logging :as log]))

; Connections mapped by UUID
(def conns (ref {}))
(def msg-count (AtomicInteger.))
(def next-measure (ref {}))
(def next-next-measure (ref {}))
(def broadcast (permanent-channel))
(defn connected-count [] (count @conns))
(def bpm 120)
(def current-beat (agent 1))

; Dump this all to the console for logging
(receive-all broadcast
             (fn [m] (log/debug (format "Broadcast: %s" m))))

(defn bpm->interval [bpm]
  (long (* (/ 60 bpm) 1000)))

(defn broadcast-beat
  "Sends the current beat info to broadcast"
  []
  (enqueue broadcast
              (encode-json->string
                {:cmd "setAllSamples"
                 :bpm bpm
                 :beat @current-beat
                 :samples @next-measure})))

(defn shift-measures
  "Copies next-next-measure into next-measure"
  []
  (dosync (ref-set next-measure (ensure next-next-measure))))

(defn metronome-tick []
  (cond
    (> (connected-count) 0)
    (send current-beat
      (fn [i]
        (cond
          (= i 1) (shift-measures)
          (= i 2) (broadcast-beat))
        (cond (= 4 i) 1
              :else   (inc i))))))

(def metronome (.schedule (Timer.)
                          (proxy [TimerTask] []
                            (run [] (metronome-tick)))
                          (long 0)
                          (bpm->interval bpm)))

(defn set-user-samples [user-id new-samples]
  (dosync
    (let [[measure-data ensure-me] (cond (<= @current-beat 2)
                                     [next-measure next-next-measure]
                                     :else         [next-next-measure next-measure])]
          (ensure ensure-me)
          (alter measure-data update-in [user-id :samples] (fn [_] new-samples)))))

(defn delete-user-samples
  [user-id]
  (dosync
    (alter next-measure      dissoc user-id)
    (alter next-next-measure dissoc user-id)))

(defn route-command
  "Route a msg based on the command name"
  [msg ch]
  (let [{:keys [cmd user-id]} msg]
    (cond
      (= "play" cmd)
        (enqueue broadcast (encode-json->string msg))
      (= "setSamples" cmd)
        (set-user-samples user-id (:samples msg))
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
        (not (nil? raw-msg))
        (try
          (.getAndIncrement msg-count)
          (cond (nil? raw-msg)
                  (log/info "Empty message encountered")
                :else
                  (let [msg-with-user (assoc (decode-json raw-msg) :user-id (str id))]
                    (route-command msg-with-user ch)))
          (catch Exception e
                 (log/warn (str "Caught msg exception: " e))
                 (.printStackTrace e)))
        :else
          (log/warn (format "Received nil message: %s" raw-msg))))))

(defn remove-conn
  "Removes a connection from global list"
  [user-id]
  (log/info (format "Removing connection %s" user-id))
  (dosync
    (alter conns dissoc user-id)
    (alter next-measure dissoc user-id)))

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