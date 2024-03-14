(ns app.views
  (:require
   [re-frame.core :as re-frame]
   [app.subs :as subs]
   [app.game :as game]))

(defn footer []
  (let [app-info @(re-frame/subscribe [::subs/app-info])]
    [:div.footer
     (str "Version: " (:version app-info))]))

(defn- resize [^js state]
  (let [canvas (.-canvas state)
        context (.-context state)
        dpr (.-devicePixelRatio js/window)]
    (set! (.-width canvas) (* dpr (.-clientWidth canvas)))
    (set! (.-height canvas) (* dpr (.-clientHeight canvas)))
    (.scale context dpr dpr)
    (set! (.-width state) (.-clientWidth canvas))
    (set! (.-height state) (.-clientHeight canvas))))

(defn- canvas-ref [^js canvas]
  (if canvas
    (do
      (set! (.-dataset.inst canvas) (str (random-uuid)))
      (js/setTimeout #(js/window.requestAnimationFrame game/game-iter) 0)

      (game/init-game canvas)
      (set! (.-onresize js/window) (fn [] (resize game/game-state)))
      (resize game/game-state))
    (set! (.-onresize js/window) (fn [])))
  nil)

(defn game []
  [:div.game
   [:canvas
    {:tab-index 1
     :style {:width "100%"
             :height "100%"
             :touch-action "none"}
     :ref canvas-ref}]
   [footer]])

(defn app []
  [game])
