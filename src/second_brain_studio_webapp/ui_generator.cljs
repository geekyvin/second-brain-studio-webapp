(ns second-brain-studio-webapp.ui-generator
  (:require [reagent.core :as r]
            [cljs.reader :as reader]
            ["recharts" :refer [ResponsiveContainer PieChart Pie Cell BarChart Bar LineChart Line XAxis YAxis CartesianGrid Tooltip Legend]]
            [clojure.string :as str]))

;; 🔹 State Management
(defonce ui-state (r/atom {:input ""
                           :generated-ui nil
                           :loading? false
                           :error nil}))

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
(defn fetch-ui []
  (swap! ui-state assoc :loading? true)
  (-> (js/fetch "http://localhost:3000/generate-ui"
                #js {:method "POST"
                     :headers #js {"Content-Type" "application/json"}
                     :body (js/JSON.stringify #js {:prompt (:input @ui-state)})})
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. "HTTP error!")))))
      (.then (fn [data]
               (let [raw-code (extract-ui-code (.-body data))
                     parsed (parse-hiccup raw-code)
                     with-placeholders (substitute-placeholders parsed)
                     resolved-ui (resolve-components with-placeholders)]
                 (println "Hiccup Code:" parsed)
                 (println "Resolved UI:" resolved-ui)
                 (swap! ui-state assoc :generated-ui resolved-ui :loading? false :error nil))))
      (.catch (fn [error]
                (js/console.error "Error generating UI:" error)
                (swap! ui-state assoc :error (str "Error: " error) :loading? false)))))


;; 🔹 UI Generator Component

(defn ui-generator []
  (let [command (r/atom "")]
    (fn []
      [:div.generate-visual-container
       [:input.generate-visual-input
        {:type "text"
         :value @command
         :placeholder "Enter command (e.g., 'chart from data', 'timeline')"
         :on-change #(reset! command (.. % -target -value))}]
       [:button.generate-visual-button
        {:on-click #(js/console.log "Generate visual from:" @command)}
        [:svg {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
         [:path {:d "M12 3v3m0 0 3-3m-3 3L9 3"}]
         [:path {:d "M3 12h3m0 0-3-3m3 3-3 3"}]
         [:path {:d "M21 12h-3m0 0 3-3m-3 3 3 3"}]
         [:path {:d "M12 21v-3m0 0 3 3m-3-3-3 3"}]]
        "Generate Visual"]])))
   

