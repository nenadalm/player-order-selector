(ns app.game
  (:require
   [goog.object]))

(defonce  game-state #js {})

(declare update-game render-game)

(defn game-iter [current-time]
  (let [^js state game-state
        delta (- current-time (.-prev-time state))]
    (if (.-running state)
      (do
        (js/window.requestAnimationFrame game-iter)
        (set! (.-current-time state) current-time)
        (update-game state delta)
        (render-game state)
        (set! (.-prev-time state) current-time))
      (js-delete state "prev_time"))))

(defn- start [^js state]
  (when-not (.-running state)
    (js/window.requestAnimationFrame game-iter)
    (set! (.-running state) true)))

(defn- stop [^js state]
  (set! (.-running state) false))

(defn resize [^js state]
  (let [canvas (.-canvas state)
        context (.-context state)
        dpr (.-devicePixelRatio js/window)]
    (set! (.-width canvas) (* dpr (.-clientWidth canvas)))
    (set! (.-height canvas) (* dpr (.-clientHeight canvas)))
    (.scale context dpr dpr)
    (set! (.-width state) (.-clientWidth canvas))
    (set! (.-height state) (.-clientHeight canvas))
    (start state)))

(defn init-game [canvas]
  (let [push-event (fn [event]
                     (start game-state)
                     (.push (.-events game-state) event))
        context (.getContext canvas "2d")]
    (set!
     game-state
     (js/Object.assign
      #js {:width 0
           :height 0
           :prev-time (js/window.performance.now)

           :state "deciding" ;; "deciding" | "decided"
           :touches #js {}
           :events #js []
           :ui #js []}
      game-state
      #js {:canvas canvas
           :context context
           :running true}))
    (.addEventListener
     canvas
     "touchstart"
     (fn [e]
       (push-event #js ["touchstart" (js/Array.from (.-changedTouches e))]))
     #js {:passive true})
    (.addEventListener
     canvas
     "touchmove"
     (fn [e]
       (push-event #js ["touchmove" (js/Array.from (.-changedTouches e))]))
     #js {:passive true})
    (.addEventListener
     canvas
     "touchend"
     (fn [e]
       (push-event #js ["touchend" (js/Array.from (.-changedTouches e))]))
     #js {:passive true})
    (.addEventListener
     canvas
     "touchcancel"
     (fn [e]
       (push-event #js ["touchend" (js/Array.from (.-changedTouches e))]))
     #js {:passive true})))

(defn- schedule [^js state]
  (set! (.-decide-at state) (+ 3000 (.-current-time state))))

(defn- unschedule [^js state]
  (js-delete state "decide_at"))

(defn- reschedule [^js state]
  (if (== 0 (.-length (js/Object.keys (.-touches state))))
    (unschedule state)
    (schedule state)))

(defn- point-in-rectangle? [px py rx1 rx2 ry1 ry2]
  (and (<= rx1 px rx2)
       (<= ry1 py ry2)))

(defn- reset-game [^js state]
  (set! (.-state state) "deciding")
  (set! (.-touches state) #js {}))

(defn update-game [^js state _delta]
  (let [deciding (= "deciding" (.-state state))]
    (doseq [event (.-events state)]
      (case (first event)
        "touchstart" (do
                       (.forEach
                        (js/Object.values (.-ui state))
                        (fn [^js ui]
                          (let [touches (second event)]
                            (.forEach
                             touches
                             (fn [touch]
                               (when (. (.-intersects ui) call ui (.-pageX touch) (.-pageY touch))
                                 (set! (.-state ui) "active")))))))
                       (when deciding
                         (let [touches (second event)]
                           (.forEach
                            touches
                            (fn [touch]
                              (js/Object.assign (.-touches state) (clj->js {(.-identifier touch) touch})))))
                         (schedule state)))
        "touchmove" (do
                      (.forEach
                       (js/Object.values (.-ui state))
                       (fn [^js ui]
                         (let [touches (second event)]
                           (.forEach
                            touches
                            (fn [touch]
                              (when (and (= "active" (.-state ui))
                                         (not (. (.-intersects ui) call ui (.-pageX touch) (.-pageY touch))))
                                (set! (.-state ui) "normal")))))))
                      (when deciding
                        (let [touches (second event)]
                          (.forEach
                           touches
                           (fn [touch]
                             (js/Object.assign (.-touches state) (clj->js {(.-identifier touch) touch})))))))
        "touchend" (do
                     (.forEach
                      (js/Object.values (.-ui state))
                      (fn [^js ui]
                        (let [touches (second event)]
                          (.forEach
                           touches
                           (fn [touch]
                             (when (= "active" (.-state ui))
                               (when (. (.-intersects ui) call ui (.-pageX touch) (.-pageY touch))
                                 (. (.-onclick ui) call ui state))
                               (set! (.-state ui) "normal")))))))
                     (when deciding
                       (let [touches (second event)]
                         (.forEach
                          touches
                          (fn [touch]
                            (js-delete (.-touches state) (.-identifier touch))))
                         (reschedule state)))))))
  (set! (.-events state) #js [])

  (when (= "deciding" (.-state state))
    (when-let [decide-at (.-decide-at state)]
      (when (<= decide-at (.-current-time state))
        (.forEach
         (js/Array.from (shuffle (js/Object.keys (.-touches state))))
         (fn [i order-1]
           (set! (.-order (goog.object/get (.-touches state) i)) (inc order-1))))
        (set! (.-state state) "decided")
        (js-delete state "decide_at")
        (let [id (str (random-uuid))]
          (js/Object.assign
           (.-ui state)
           (clj->js {id #js {:id id
                             :type "button"
                             :state "normal" ;; "normal" | "active"
                             :x 0
                             :y 0
                             :r 40
                             :intersects (fn [x y]
                                           (this-as
                                            this
                                            (let [r (.-r this)
                                                  x1 (- (.-x this) r)
                                                  x2 (+ (.-x this) r)
                                                  y1 (- (.-y this) r)
                                                  y2 (+ (.-y this) r)]
                                              (point-in-rectangle? x y x1 x2 y1 y2))))
                             :onclick (fn [^js state]
                                        (this-as
                                         this
                                         (js-delete (.-ui state) (.-id this)))
                                        (reset-game state))
                             :update (fn [^js state]
                                       (let [width (.-width state)
                                             height (.-height state)]
                                         (this-as
                                          this
                                          (set! (.-x this) (/ width 2))
                                          (set! (.-y this) (/ height 2)))))
                             :render (fn [^js state]
                                       (this-as
                                        this
                                        (let [context (.-context state)
                                              width (.-width state)
                                              height (.-height state)
                                              r (.-r this)
                                              x (.-x this)
                                              y (.-y this)]
                                          (set! (.-fillStyle context) (if (= "active" (.-state this)) "rgba(239, 239, 239, 0.7)" "rgba(239, 239, 239)"))
                                          (.arc context x y r 0 (* 2 js/Math.PI))
                                          (.fill context)

                                          (set! (.-font context) "1rem sans-serif")
                                          (set! (.-fillStyle context) "#000")
                                          (.fillText context "Reset" (/ width 2) (/ height 2)))))}})))))

    (.forEach
     (js/Object.values (.-ui state))
     (fn [^js ui]
       (. (.-update ui) call ui state))))

  (when (or
         (= "decided" (.-state state))
         (== 0 (.-length (js/Object.keys (.-touches state)))))
    (stop state))

  state)

(defn- ->touch [touch]
  (let [r (+ (js/Math.max
              (.-radiusX touch)
              (.-radiusY touch))
             30)]

    #js {:x (- (.-pageX touch) r)
         :y (- (.-pageY touch) r)
         :r r}))

(defn render-game [^js state]
  (let [context (.-context state)
        width (.-width state)
        height (.-height state)]
    (.clearRect context 0 0 width height context)

    (set! (.-font context) "3rem sans-serif")
    (set! (.-textAlign context) "center")
    (set! (.-textBaseline context) "middle")

    (.forEach
     (js/Object.values (.-touches state))
     (fn [touch*]
       (let [touch (->touch touch*)
             x (+ (.-x touch) (.-r touch))
             y (+ (.-y touch) (.-r touch))]
         (.beginPath context)

         (set! (.-fillStyle context) "rgb(55, 151, 250)")
         (.arc context (+ (.-x touch) (.-r touch)) (+ (.-y touch) (.-r touch)) (.-r touch) 0 (* 2 js/Math.PI))
         (.fill context)

         (when-let [order (.-order touch*)]
           (set! (.-fillStyle context) "rgba(255, 255, 255, 0.87)")
           (.fillText context order x y)))))

    (when (and (= "deciding" (.-state state))
               (not (.-decide-at state)))
      (set! (.-font context) "1rem sans-serif")
      (set! (.-fillStyle context) "rgba(255, 255, 255, 0.87)")
      (set! (.-textAlign context) "center")
      (set! (.-textBaseline context) "middle")
      (.fillText context "All players should tap and hold to decide order." (/ width 2) (/ height 2)))

    (when-let [decide-at (.-decide-at state)]
      (let [countdown (max 0
                           (Math/ceil (/ (- decide-at (.-current-time state)) 1000)))]
        (set! (.-font context) "5rem sans-serif")
        (set! (.-fillStyle context) "rgba(255, 255, 255, 0.87)")
        (set! (.-textAlign context) "center")
        (set! (.-textBaseline context) "middle")
        (.fillText context countdown (/ width 2) (/ height 2))))

    (.forEach
     (js/Object.values (.-ui state))
     (fn [^js ui]
       (.beginPath context)
       (. (.-render ui) call ui state)))))

(defn set-update-available []
  (set! (.-update-available game-state) true))
