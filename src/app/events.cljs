(ns app.events
  (:refer-clojure :exclude [time])
  (:require
   [reagent.core :as reagent]
   [nenadalm.clojure-utils.cljs :as cljs-utils]
   [re-frame.core :as re-frame]))

(defn- deciding? [db]
  (= :deciding (get-in db [:game :state])))

(defn- animation-interval
  "https://gist.github.com/jakearchibald/cb03f15670817001b1157e62a076fe95"
  [ms signal f]
  (let [start (.-currentTime js/document.timeline)]
    (letfn [(frame [time]
              (when-not (.-aborted signal)
                (f time)
                (schedule-frame time)))
            (schedule-frame [time]
              (let [elapsed (- time start)
                    rounded-elapsed (* (js/Math.round (/ elapsed ms)) ms)
                    target-next (+ start rounded-elapsed ms)
                    delay (- target-next (js/performance.now))]
                (js/setTimeout #(js/requestAnimationFrame frame) delay)))]
      (schedule-frame start))))

(re-frame/reg-cofx
 :app-version
 (fn [coeffects _]
   (assoc coeffects :app-version (cljs-utils/app-version))))

(defn reset-game [db]
  (assoc db
         :game {:state :deciding ;; :deciding | :decided
                }))

(defn- get-time []
  (.getTime (js/Date.)))

(re-frame/reg-cofx
 :now
 (fn [coeffects _]
   (assoc coeffects :now (get-time))))

(def time (reagent/atom (get-time)))

(re-frame/reg-fx
 ::schedule
 (fn [{:keys [key at dispatch]}]
   (remove-watch time key)
   (add-watch time key (fn [_ _ _ ns]
                         (when (<= (- at ns) 0)
                           (re-frame/dispatch dispatch)
                           (remove-watch time key))))))

(re-frame/reg-fx
 ::unschedule
 (fn [key]
   (remove-watch time key)))

(re-frame/reg-fx
 :update-time
 (fn [interval-ms]
   (animation-interval
    interval-ms
    (.-signal (js/AbortController.))
    (fn [_]
      (reset! time (get-time))))))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :app-version)]
 (fn [{:keys [app-version db]} _]
   (let [data {:app-info {:version app-version}}]
     {:db (if (seq db)
            (merge db data)
            (reset-game data))
      :update-time 1000})))

(re-frame/reg-event-fx
 ::reset
 (fn [{:keys [db]}]
   {:db (reset-game db)}))

(re-frame/reg-event-fx
 ::update-available
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:app-info :update-available] true)}))

(defn- update-touches [db touches]
  (update-in db [:game :touches] (fn [existing]
                                   (reduce
                                    (fn [acc next]
                                      (update acc (:id next) merge next))
                                    existing
                                    touches))))

(re-frame/reg-event-fx
 ::decide
 (fn [{:keys [db]}]
   (let [touches (get-in db [:game :touches])
         ordered-touches (into
                          {}
                          (map-indexed
                           (fn [k i]
                             [k (assoc (get touches k) :order (inc i))]))
                          (shuffle (keys touches)))]
     {:db (-> db
              (update :game dissoc :decide-at)
              (update :game assoc :state :decided)
              (assoc-in [:game :touches] ordered-touches))})))

(def ^:private conjv (fnil conj []))

(defn- schedule [effects now]
  (let [decide-at (+ now 3000)]
    (-> effects
        (assoc-in [:db :game :decide-at] decide-at)
        (update :fx conjv [::schedule {:key :decide
                                       :at decide-at
                                       :dispatch [::decide]}]))))

(defn- unschedule [effects]
  (-> effects
      (update-in [:db :game] dissoc :decide-at)
      (update :fx conjv [::unschedule :decide])))

(defn- reschedule [effects now]
  (if (empty? (get-in effects [:db :game :touches]))
    (unschedule effects)
    (schedule effects now)))

(re-frame/reg-event-fx
 ::update-touches
 [(re-frame/inject-cofx :now)]
 (fn [{:keys [db now]} [_ touches reset-timer]]
   (if (deciding? db)
     (let [db (update-touches db touches)]
       (cond-> {:db db}
         reset-timer (schedule now)))
     {})))

(re-frame/reg-event-fx
 ::remove-touches
 [(re-frame/inject-cofx :now)]
 (fn [{:keys [db now]} [_ touches]]
   (if (deciding? db)
     (-> {:db (update-in db [:game :touches] (fn [existing]
                                               (reduce
                                                (fn [acc next]
                                                  (dissoc acc (:id next) next))
                                                existing
                                                touches)))}
         (reschedule now))
     {})))
