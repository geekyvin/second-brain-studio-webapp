(ns second-brain-studio-webapp.ui-generator
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [second-brain-studio-webapp.sb-backend-client :as sbb-client]
            ["recharts" :refer [ResponsiveContainer BarChart LineChart PieChart Bar Line Pie
                                XAxis YAxis CartesianGrid Tooltip Legend Cell]]))

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

;; Define a map of React components that we might receive in [:> Component] syntax
;; This is just for documentation purposes - we aren't actually importing these components
(def component-map
  {'ResponsiveContainer "A container component that resizes the chart to fit its parent container"
   'PieChart           "A circular statistical graphic divided into slices"
   'Pie                "A component to render pie/donut chart"
   'Cell               "A component for customizing sectors/points/bars"
   'BarChart           "A chart with rectangular bars proportional to the values they represent"
   'Bar                "A component to render bars in a bar chart"
   'XAxis              "A component to render an axis on the bottom of a chart"
   'YAxis              "A component to render an axis on the left or right of a chart"
   'CartesianGrid      "A component to render grid lines within the chart"
   'Tooltip            "A component to show data when hovering over chart elements"
   'Legend             "A component to render a legend for the chart"
   'Line               "A component to render a line in a line chart"
   'LineChart          "A chart that displays data as a series of points connected by lines"})

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
  (try
    (js/console.log "Extracting UI code from response:" (pr-str response-body))
    
    ;; Ensure response-body is a string
    (let [response-str (cond
                         (nil? response-body) 
                         (do
                           (js/console.warn "Response body is nil")
                           nil)
                         
                         (string? response-body) 
                         response-body
                         
                         :else 
                         (do
                           (js/console.log "Converting response to string, type:" (type response-body))
                           (str response-body)))
          pattern #"(?s)\$ui-code-start\$(.*?)\$ui-code-end\$"]
      
      (if response-str
        (let [match (re-find pattern response-str)]
          (if match
            (do
              (js/console.log "Found UI code between delimiters")
              (-> (second match)
                  str/trim))
            (do
              (js/console.log "No UI code delimiters found, returning full response")
              ;; If it contains vector notation, it might be code
              (if (re-find #"^\s*\[" response-str)
                response-str
                nil))))
        nil))
    (catch js/Error e
      (js/console.error "Error extracting UI code:" e)
      nil)))

(defn parse-hiccup [hiccup-str]
  (try
    (reader/read-string hiccup-str)
    (catch js/Error e
      (println "Error parsing Hiccup:" (.-message e))
      nil)))

;; Forward declare chart functions
(declare bar-chart line-chart pie-chart scatter-chart)

;; React component lookup map for resolving symbols to imported components
(def react-components
  {'ResponsiveContainer ResponsiveContainer
   'BarChart BarChart
   'LineChart LineChart 
   'PieChart PieChart
   'Bar Bar 
   'Line Line
   'Pie Pie
   'XAxis XAxis 
   'YAxis YAxis
   'CartesianGrid CartesianGrid
   'Tooltip Tooltip
   'Legend Legend
   'Cell Cell})

(defn process-hiccup
  "Process Hiccup form, resolving :> React component references"
  [form]
  (cond
    ;; React component reference [:> Component {...}]
    (and (vector? form) (= :> (first form)))
    (let [component-name (second form)
          component (get react-components component-name)
          props (nth form 2 {})
          children (drop 3 form)]
      (if component
        (into [:> component props] (map process-hiccup children))
        [:div.missing-component 
         [:strong "Missing React component: "] 
         (str component-name)]))
    
    ;; Regular vector (nested elements)
    (vector? form)
    (mapv process-hiccup form)
    
    ;; Map (props/attributes)
    (map? form)
    (reduce-kv (fn [m k v] 
                 (assoc m k (if (or (vector? v) (map? v)) 
                              (process-hiccup v) 
                              v)))
               {} form)
    
    ;; Everything else remains unchanged
    :else form))

(defn execute-ui-code
  "Execute the UI code and render the result"
  [code]
  (try
    (js/console.log "Executing UI code, raw format:", code)
    
    ;; Handle Hiccup format
    (if (and (string? code) (or (str/starts-with? (str/trim code) "[:") 
                                (str/starts-with? (str/trim code) "[:")))
      (try
        (js/console.log "Detected Hiccup format, parsing and processing")
        (let [hiccup (reader/read-string code)]
          (if (vector? hiccup)
            ;; Process the Hiccup to resolve React components
            (process-hiccup hiccup)
            [:div.error
             [:h4 "Parsing Error"]
             [:pre.code-block code]]))
        (catch js/Error e
          (js/console.error "Error parsing Hiccup:" e)
          [:div.error
           [:h4 "Error parsing UI code"]
           [:pre.code-block code]]))
      
      ;; If not Hiccup, show the raw code
      [:div.error
       [:h4 "Unexpected code format"]
       [:pre.code-block code]])
    (catch js/Error e
      (js/console.error "Error executing UI code:" e)
      [:div.error
       [:h4 "Error executing code"]
       [:pre.code-block code]])))

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

;; 🔹 Fetch UI Code from Backend
(defn fetch-ui
  "Fetch UI from the API based on the prompt and editor content"
  [prompt editor-content]
  (js/console.log "Fetching UI with prompt:" prompt)
  (js/console.log "Editor content:" (if editor-content (subs editor-content 0 (min 50 (count editor-content))) "nil"))
  
  (swap! ui-state assoc :loading? true :error? false)
  
  (sbb-client/call-generate-ui-api
   prompt
   editor-content
   ;; Success callback
   (fn [data]
     (js/console.log "API Response type:" (type data))
     (js/console.log "API Response:" (pr-str data))
     
     (let [ui-code (cond
                     ;; If data has a 'code' property, use that
                     (and data (.-code data))
                     (.-code data)
                     
                     ;; If data has a 'body' property (which might contain code between delimiters)
                     (and data (.-body data))
                     (extract-ui-code (.-body data))
                     
                     ;; If data itself is a string, try to extract code
                     (string? data)
                     (extract-ui-code data)
                     
                     ;; Otherwise try to use the data itself
                     :else
                     (do
                       (js/console.log "No code or body property found, trying to use full data")
                       (extract-ui-code (str data))))]
       
       (js/console.log "Extracted UI code:" (if ui-code (subs ui-code 0 (min 50 (count ui-code))) "nil"))
       (swap! ui-state assoc 
              :loading? false 
              :ui-code ui-code)
       
       ;; Try to parse the visualization information from the code
       (try
         (when ui-code
           (let [type-match (re-find #"type: ['\"]([^'\"]+)" ui-code)
                 data-match (re-find #"data: (\[.+\])" ui-code)
                 labels-match (re-find #"labels: (\[.+\])" ui-code)
                 title-match (re-find #"title: ['\"]([^'\"]+)" ui-code)]
             
             (when (and type-match data-match)
               (swap! ui-state assoc
                      :visual-type (second type-match)
                      :visual-data (js/JSON.parse (second data-match))
                      :visual-labels (when labels-match (js/JSON.parse (second labels-match)))
                      :visual-title (when title-match (second title-match))))))
         (catch js/Error e
           (js/console.warn "Failed to parse visualization data:" e)))))
   
   ;; Error callback
   (fn [error]
     (js/console.error "Error fetching UI:" error)
     (swap! ui-state assoc 
            :loading? false
            :error? true
            :error-message (.-message error)))))

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
                      (fetch-ui (:command @ui-state) (or js/window.editorContent "")))}]
    [:button.generate-visual-button
     {:on-click #(fetch-ui (:command @ui-state) (or js/window.editorContent ""))}
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
       data)]]))

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


