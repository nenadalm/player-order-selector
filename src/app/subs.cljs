(ns app.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::touches
 (fn [db _]
   (vals (get-in db [:game :touches]))))

(re-frame/reg-sub
 ::decide-at
 (fn [db _]
   (get-in db [:game :decide-at])))

(re-frame/reg-sub
 ::decided
 (fn [db _]
   (= :decided (get-in db [:game :state]))))

(re-frame/reg-sub
 ::show-help
 (fn [db _]
   (let [game (:game db)]
     (and (= :deciding (:state game))
          (not (:decide-at game))))))

(re-frame/reg-sub
 ::app-info
 (fn [db _]
   (:app-info db)))
