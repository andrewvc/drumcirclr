(ns drumcirclr.sequencer
  (:use lamina.core)
  (:require [drumcirclr.connections :as connections])
  (:import java.util.Timer
           java.util.TimerTask))

(def next-measure (ref {}))
(def next-next-measure (ref {}))
(def bpm 120)
(def current-beat (agent 1))

(defn bpm->interval [bpm]
  (long (* (/ 60 bpm) 1000)))

(defn beat-snapshot
  []
  {:bpm bpm
   :beat @current-beat
   :samples @next-measure})

(defn shift-measures
  "Copies next-next-measure into next-measure"
  []
  (dosync (ref-set next-measure (ensure next-next-measure))))

(def metronome-timer (Timer.))

(defn metronome-tick
  [on-tick]
  (send current-beat
    (fn [i]
      (on-tick (beat-snapshot))
      (cond (= i 1) (shift-measures))
      (cond (= 4 i) 1
            :else   (inc i)))))

(defn start-metronome
  [on-tick]
  (.schedule metronome-timer
    (proxy [TimerTask] []
      (run [] (metronome-tick on-tick)))
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