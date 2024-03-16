(ns app.core
  (:require
   [app.config :as config]
   [app.game :as game]
   [nenadalm.clojure-utils.cljs :as cljs-utils]))

(defn- canvas-ref [^js canvas]
  (if canvas
    (do
      (set! (.-dataset.inst canvas) (str (random-uuid)))
      (js/setTimeout #(js/window.requestAnimationFrame game/game-iter) 0)

      (game/init-game canvas)
      (set! (.-onresize js/window) (fn [] (game/resize game/game-state)))
      (game/resize game/game-state))
    (set! (.-onresize js/window) (fn [])))
  nil)

(defn mount-root []
  (canvas-ref (js/document.querySelector "canvas")))

(defn register-worker []
  (some-> js/navigator
          .-serviceWorker
          (.register "worker.js")
          (.then
           (fn [registration]
             (if (and (.-waiting registration)
                      js/navigator.serviceWorker.controller)
               (game/set-update-available)
               (.addEventListener
                registration
                "updatefound"
                (fn []
                  (when-let [installing (.-installing registration)]
                    (.addEventListener
                     installing
                     "statechange"
                     (fn []
                       (when (and (.-waiting registration)
                                  js/navigator.serviceWorker.controller)
                         (game/set-update-available))))))))))))

(defn- dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn- prod-setup []
  (when-not config/debug?
    (register-worker)))

(defn ^:export init []
  (dev-setup)
  (prod-setup)
  (cljs-utils/prevent-screen-lock)
  (mount-root))

(defn ^:dev/after-load after-load []
  (mount-root))
