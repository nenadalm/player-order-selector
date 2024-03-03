(ns app.views
  (:require
   [re-frame.core :as re-frame]
   [app.subs :as subs]
   [app.events :as events]))

(defn- ->touch [touch]
  (let [r (+ (max
              (.-radiusX touch)
              (.-radiusY touch))
             30)]

    {:id (.-identifier touch)
     :x (- (.-pageX touch) r)
     :y (- (.-pageY touch) r)
     :r r}))

(defn- e->touches [e]
  (mapv ->touch (.-changedTouches e)))

(defn touches []
  [:div.touches
   (for [touch @(re-frame/subscribe [::subs/touches])]
     (let [d (str (* 2 (:r touch)) "px")]
       ^{:key (:id touch)} [:div.touch
                            {:style {:background-color "rgb(55, 151, 250)"
                                     :width d
                                     :height d
                                     :left (str (:x touch) "px")
                                     :top (str (:y touch) "px")}}
                            (:order touch)]))])

(defn countdown []
  (let [now @events/time
        decide-at @(re-frame/subscribe [::subs/decide-at])]
    (when decide-at
      [:div.countdown
       (max 0
            (Math/ceil (/ (- decide-at now) 1000)))])))

(defn reset-btn []
  (when @(re-frame/subscribe [::subs/decided])
    [:button.reset-btn
     {:on-click #(re-frame/dispatch [::events/reset])}
     "Reset"]))

(defn help []
  (when @(re-frame/subscribe [::subs/show-help])
    [:div.help "All players should tap and hold to decide order."]))

(defn footer []
  (let [app-info @(re-frame/subscribe [::subs/app-info])]
    [:div.footer
     (str "Version: " (:version app-info))]))

(defn game []
  [:div.game {:on-touchStart (fn [^js e]
                               (re-frame/dispatch [::events/update-touches (e->touches e) true]))
              :on-touchMove (fn [^js e]
                              (re-frame/dispatch [::events/update-touches (e->touches e)]))
              :on-touchEnd (fn [^js e]
                             (re-frame/dispatch [::events/remove-touches (e->touches e)]))
              :on-touchCancel (fn [^js e]
                                (re-frame/dispatch [::events/remove-touches (e->touches e)]))}
   [touches]
   [countdown]
   [reset-btn]
   [help]
   [footer]])

(defn app []
  [game])
