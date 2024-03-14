(ns app.events
  (:refer-clojure :exclude [time])
  (:require
   [nenadalm.clojure-utils.cljs :as cljs-utils]
   [re-frame.core :as re-frame]))

(re-frame/reg-cofx
 :app-version
 (fn [coeffects _]
   (assoc coeffects :app-version (cljs-utils/app-version))))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :app-version)]
 (fn [{:keys [app-version db]} _]
   (let [data {:app-info {:version app-version}}]
     {:db (merge db data)})))

(re-frame/reg-event-fx
 ::update-available
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:app-info :update-available] true)}))
