(ns app.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::app-info
 (fn [db _]
   (:app-info db)))
