(ns second-brain-studio-webapp.ui-generator
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs.reader :as reader]
            [clojure.string :as str]))

;; 🔹 State Management
(def ui-state (r/atom {:loading? false
                       :error? false
                       :error-message ""
                       :command ""
                       :visual-type nil
                       :visual-data nil
                       :visual-labels nil
                       :visual-title nil
                       :ui-code nil}))

(def my-data [{:Year 2023, :TAM 7.5, :SAM 0.5, :SOM 0}
              {:Year 2025, :TAM 11, :SAM 0.825, :SOM 0.015}
              {:Year 2030, :TAM 27.5, :SAM 1.65, :SOM 0.033}
              {:Year 2033, :TAM 35, :SAM 2.45, :SOM 0.1225}])

(def component-map
  {'ResponsiveContainer ResponsiveContainer
   'PieChart            PieChart
   'Pie                 Pie
   'Cell                Cell
   'BarChart            BarChart
   'Bar                 Bar
   'XAxis              XAxis
   'YAxis              YAxis
   'CartesianGrid      CartesianGrid
   'Tooltip            Tooltip
   'Legend             Legend
   'Line               Line
   'LineChart          LineChart})

(defn substitute-placeholders [hiccup]
  (cond
    (symbol? hiccup)
    (if (= hiccup 'my-data)
      my-data
      hiccup)
    (vector? hiccup)
    (vec (map substitute-placeholders hiccup))
    (map? hiccup)
    (into {} (map (fn [[k v]]
                    [k (substitute-placeholders v)])
                  hiccup))
    (seq? hiccup)
    (doall (map substitute-placeholders hiccup))
    :else hiccup))

(defn resolve-components [hiccup]
  (cond
    (symbol? hiccup)
    (if (contains? component-map hiccup)
      (get component-map hiccup)
      hiccup)
    (vector? hiccup)
    (vec (map resolve-components hiccup))
    (map? hiccup)
    (into {} (map (fn [[k v]]
                    [k (resolve-components v)])
                  hiccup))
    (seq? hiccup)
    (doall (map resolve-components hiccup))
    :else hiccup))

(defn extract-ui-code [response-body]
  (let [pattern #"(?s)\$ui-code-start\$(.*?)\$ui-code-end\$"
        match   (re-find pattern response-body)]
    (if match
      (-> (second match)
          str/trim)
      nil)))

(defn parse-hiccup [hiccup-str]
  (try
    (reader/read-string hiccup-str)
    (catch js/Error e
      (println "Error parsing Hiccup:" (.-message e))
      nil)))

;; 🔹 Fetch UI Code from Backend
(defn fetch-ui
  "Fetch UI from the API based on the prompt and editor content"
  [prompt editor-content]
  (js/console.log "Fetching UI with prompt:" prompt)
  (js/console.log "Editor content:" editor-content)
  
  ;; Combine prompt and editor content for the API call
  (let [combined-prompt (str "Markdown content:\n\n" editor-content "\n\nCommand: " prompt)]
    (js/console.log "Combined prompt:" combined-prompt)
    
    (swap! ui-state assoc :loading? true :error? false)
    
    (-> (js/fetch "/api/generate-ui"
                 (clj->js {:method "POST"
                           :headers {"Content-Type" "application/json"}
                           :body (js/JSON.stringify (clj->js {:prompt combined-prompt}))}))
        (.then (fn [response]
                 (if (.-ok response)
                   (.json response)
                   (throw (new js/Error "Failed to fetch UI")))))
        (.then (fn [data]
                 (js/console.log "API Response:" data)
                 (let [ui-code (.-code data)]
                   (js/console.log "Received UI code:" ui-code)
                   (swap! ui-state assoc 
                          :loading? false 
                          :ui-code ui-code)
                   ;; Try to parse the visualization information from the code
                   (try
                     (let [type-match (re-find #"type: ['\"]([^'\"]+)" ui-code)
                           data-match (re-find #"data: (\[.+\])" ui-code)
                           labels-match (re-find #"labels: (\[.+\])" ui-code)
                           title-match (re-find #"title: ['\"]([^'\"]+)" ui-code)]
                       
                       (when (and type-match data-match)
                         (swap! ui-state assoc
                                :visual-type (second type-match)
                                :visual-data (js/JSON.parse (second data-match))
                                :visual-labels (when labels-match (js/JSON.parse (second labels-match)))
                                :visual-title (when title-match (second title-match)))))
                     (catch js/Error e
                       (js/console.warn "Failed to parse visualization data:" e))))))
        (.catch (fn [error]
                  (js/console.error "Error fetching UI:" error)
                  (swap! ui-state assoc 
                         :loading? false
                         :error? true
                         :error-message (.-message error)))))))

;; 🔹 Generated UI Container Component
(defn generated-ui-container []
  (let [{:keys [loading? error? error-message ui-code]} @ui-state]
    [:div.generated-ui-container
     (cond
       loading? [:div.ui-loading
                [:div.loading-spinner]
                [:p "Generating visualization..."]]
       
       error? [:div.ui-error
               [:p "Error: " error-message]
               [:p.hint "Try a different prompt or check your connection."]]
       
       (nil? ui-code) [:div.ui-empty
                        [:p "No visualization generated yet."]
                        [:p.hint "Type a command and click 'Generate Visual' to create a visualization."]]
       
       :else [:div.ui-result
              [:div.ui-preview
               [execute-ui-code ui-code]]
              [:div.ui-code-preview
               [:h4 "Generated Code"]
               [:pre.code-block ui-code]]])]))

;; 🔹 UI Generator Component
(defn ui-generator []
  [:div.ui-generator
   [generated-ui-container]
   [:div.generate-visual-container
    [:input.generate-visual-input
     {:type "text"
      :placeholder "Describe the visualization you want to create..."
      :value (:command @ui-state)
      :on-change #(swap! ui-state assoc :command (.. % -target -value))
      :on-key-down #(when (= (.-key %) "Enter")
                      (fetch-ui (:command @ui-state) (.-secondBrainEditorContent js/window)))}]
    [:button.generate-visual-button
     {:on-click #(fetch-ui (:command @ui-state) (.-secondBrainEditorContent js/window))}
     [:svg
      {:xmlns "http://www.w3.org/2000/svg"
       :viewBox "0 0 24 24"
       :fill "none"
       :stroke "currentColor"
       :stroke-width "2"
       :stroke-linecap "round"
       :stroke-linejoin "round"}
      [:path {:d "M12 19V5M5 12l7-7 7 7"}]]
     "Generate Visual"]]])

(defn bar-chart
  "Render a bar chart with the given data"
  [data labels title]
  (let [max-value (apply max data)
        chart-height 200
        bar-width (/ 100 (count data))
        bar-gap (/ bar-width 5)]
    [:div.chart-container
     [:h3 title]
     [:div.chart
      {:style {:height (str chart-height "px")
               :display "flex"
               :align-items "flex-end"
               :justify-content "center"
               :gap (str bar-gap "%")}}
      (map-indexed
       (fn [idx val]
         (let [height-pct (* 100 (/ val max-value))]
           [:div.bar
            {:key idx
             :style {:height (str height-pct "%")
                     :width (str (- bar-width bar-gap) "%")
                     :background-color "rgb(234, 88, 12)"
                     :border-radius "4px 4px 0 0"
                     :position "relative"}}
            [:div.bar-value
             {:style {:position "absolute"
                      :top "-20px"
                      :left "0"
                      :right "0"
                      :text-align "center"
                      :font-size "12px"}}
             val]
            [:div.bar-label
             {:style {:position "absolute"
                      :bottom "-20px"
                      :left "0"
                      :right "0"
                      :text-align "center"
                      :font-size "12px"
                      :white-space "nowrap"
                      :overflow "hidden"
                      :text-overflow "ellipsis"}}
             (get labels idx (str "Item " idx))]]))
       data)]]]))

(defn line-chart
  "Render a line chart with the given data"
  [data labels title]
  (let [max-value (apply max data)
        chart-height 200
        chart-width 100
        point-gap (/ chart-width (dec (count data)))]
    [:div.chart-container
     [:h3 title]
     [:div.chart
      {:style {:height (str chart-height "px")
               :width "100%"
               :position "relative"
               :margin-top "20px"
               :margin-bottom "30px"}}
      [:svg
       {:width "100%"
        :height "100%"
        :viewBox (str "0 0 " chart-width " " chart-height)
        :preserveAspectRatio "none"}
       [:polyline
        {:points (str/join " " 
                          (map-indexed
                           (fn [idx val]
                             (str (* idx point-gap) "," (- chart-height (* chart-height (/ val max-value)))))
                           data))
         :fill "none"
         :stroke "rgb(234, 88, 12)"
         :stroke-width "2"}]
       (map-indexed
        (fn [idx val]
          [:circle
           {:key idx
            :cx (* idx point-gap)
            :cy (- chart-height (* chart-height (/ val max-value)))
            :r "3"
            :fill "rgb(234, 88, 12)"}])
        data)]
      (map-indexed
       (fn [idx val]
         [:div.point-label
          {:key idx
           :style {:position "absolute"
                   :bottom "-25px"
                   :left (str (* idx point-gap) "%")
                   :transform "translateX(-50%)"
                   :text-align "center"
                   :font-size "12px"
                   :white-space "nowrap"
                   :overflow "hidden"
                   :text-overflow "ellipsis"
                   :width "40px"}}
          (get labels idx (str "Item " idx))])
       data)
      (map-indexed
       (fn [idx val]
         [:div.point-value
          {:key idx
           :style {:position "absolute"
                   :bottom (str (* 100 (/ val max-value)) "%")
                   :left (str (* idx point-gap) "%")
                   :transform "translate(-50%, -20px)"
                   :text-align "center"
                   :font-size "12px"}}
          val])
       data)]]))

(defn pie-chart
  "Render a pie chart with the given data"
  [data labels title]
  (let [total (apply + data)
        segments (map #(/ % total) data)
        colors ["#ea580c" "#fb923c" "#fdba74" "#fed7aa" "#ffedd5"]
        radius 80
        center 100
        pie-container-style {:width "200px" 
                             :height "200px" 
                             :margin "0 auto 30px auto" 
                             :position "relative"}]
    [:div.chart-container
     [:h3 title]
     [:div.pie-container {:style pie-container-style}
      [:svg
       {:width "100%"
        :height "100%"
        :viewBox "0 0 200 200"}
       (let [start-angle 0]
         (map-indexed
          (fn [idx segment]
            (let [angle (* segment 360)
                  end-angle (+ start-angle angle)
                  start-x (+ center (* radius (Math/cos (/ (* Math/PI start-angle) 180))))
                  start-y (+ center (* radius (Math/sin (/ (* Math/PI start-angle) 180))))
                  end-x (+ center (* radius (Math/cos (/ (* Math/PI end-angle) 180))))
                  end-y (+ center (* radius (Math/sin (/ (* Math/PI end-angle) 180))))
                  large-arc (if (> angle 180) 1 0)
                  color (get colors (mod idx (count colors)))]
              [:path
               {:key idx
                :d (str "M " center "," center " L " start-x "," start-y " A " radius "," radius " 0 " large-arc " 1 " end-x "," end-y " Z")
                :fill color}]))
          segments))]]
     [:div.legend
      {:style {:display "flex"
               :flex-wrap "wrap"
               :justify-content "center"
               :gap "10px"}}
      (map-indexed
       (fn [idx label]
         [:div.legend-item
          {:key idx
           :style {:display "flex"
                   :align-items "center"
                   :gap "5px"}}
          [:div.legend-color
           {:style {:width "12px"
                    :height "12px"
                    :background-color (get colors (mod idx (count colors)))
                    :border-radius "2px"}}]
          [:div.legend-label
           {:style {:font-size "12px"}}
           (str (get labels idx (str "Item " idx)) ": " (get data idx))]])
       labels)]]))

(defn scatter-chart
  "Render a scatter chart with the given data"
  [data labels title]
  (let [width 300
        height 200
        padding 30
        points (partition 2 data)
        x-values (map first points)
        y-values (map second points)
        x-min (apply min x-values)
        x-max (apply max x-values)
        y-min (apply min y-values)
        y-max (apply max y-values)
        x-scale (fn [x] (+ padding (* (- width (* 2 padding)) (/ (- x x-min) (- x-max x-min)))))
        y-scale (fn [y] (- height (+ padding (* (- height (* 2 padding)) (/ (- y y-min) (- y-max y-min))))))]
    [:div.chart-container
     [:h3 title]
     [:div.chart
      {:style {:width "100%"
               :height (str height "px")
               :position "relative"}}
      [:svg
       {:width "100%"
        :height "100%"
        :viewBox (str "0 0 " width " " height)}
       ;; X-axis
       [:line {:x1 padding :y1 (- height padding) :x2 (- width padding) :y2 (- height padding) :stroke "#ccc" :stroke-width "1"}]
       ;; Y-axis
       [:line {:x1 padding :y1 padding :x2 padding :y2 (- height padding) :stroke "#ccc" :stroke-width "1"}]
       ;; Points
       (map-indexed
        (fn [idx [x y]]
          [:circle
           {:key idx
            :cx (x-scale x)
            :cy (y-scale y)
            :r "5"
            :fill "rgb(234, 88, 12)"}])
        points)]]]))

(defn render-visual
  "Render a visualization from the code"
  [code]
  (try
    (let [type-match (re-find #"type: ['\"]([^'\"]+)" code)
          data-match (re-find #"data: (\[.+\])" code)
          labels-match (re-find #"labels: (\[.+\])" code)
          title-match (re-find #"title: ['\"]([^'\"]+)" code)]
      
      (if (and type-match data-match)
        (let [chart-type (second type-match)
              data (js/JSON.parse (second data-match))
              labels (when labels-match (js/JSON.parse (second labels-match)))
              title (if title-match (second title-match) "Chart")]
          
          (case chart-type
            "bar" [bar-chart data labels title]
            "line" [line-chart data labels title]
            "pie" [pie-chart data labels title]
            "scatter" [scatter-chart data labels title]
            [:div.fallback-visualization
             [:h3 "Unknown Chart Type"]
             [:pre code]]))
        
        [:div.fallback-visualization
         [:h3 "Visualization Code"]
         [:pre code]]))
    (catch js/Error e
      (js/console.warn "Error rendering visualization:" e)
      [:div.fallback-visualization
       [:h3 "Visualization Code"]
       [:pre code]])))

(defn execute-ui-code
  "Execute the UI code and render the result"
  [code]
  (try
    (let [js-code (str "
      (function() {
        try {
          const chartConfig = " code "
          return chartConfig;
        } catch(e) {
          console.error('Error executing UI code:', e);
          return null;
        }
      })()
    ")]
      (js/console.log "Executing UI code")
      (let [result (js/eval js-code)]
        (js/console.log "UI code execution result:" result)
        (if result
          (let [chart-type (.-type result)
                data (.-data result)
                labels (.-labels result)
                title (.-title result)]
            (case chart-type
              "bar" [bar-chart (js->clj data) (js->clj labels) title]
              "line" [line-chart (js->clj data) (js->clj labels) title]
              "pie" [pie-chart (js->clj data) (js->clj labels) title]
              "scatter" [scatter-chart (js->clj data) (js->clj labels) title]
              [:div.fallback-visualization
               [:h4 "Unknown Chart Type"]
               [:pre.code-block code]]))
          [:div.fallback-visualization
           [:h4 "Failed to execute visualization code"]
           [:pre.code-block code]]))
    (catch js/Error e
      (js/console.error "Error executing UI code:" e)
      [:div.fallback-visualization
       [:h4 "Error executing code"]
       [:pre.code-block code]])))
   

