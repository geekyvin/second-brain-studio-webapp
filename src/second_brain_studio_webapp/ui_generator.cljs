(ns second-brain-studio-webapp.ui-generator
  (:require [reagent.core :as r]
            [cljs.reader :as reader]
            ["recharts" :refer [ResponsiveContainer BarChart Bar XAxis YAxis CartesianGrid Tooltip Legend]]
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
   'BarChart            BarChart
   'Bar                 Bar
   'XAxis              XAxis
   'YAxis              YAxis
   'CartesianGrid      CartesianGrid
   'Tooltip            Tooltip
   'Legend             Legend})

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
  [:div {:style {:width "100%"
                 :border "1px solid #ccc"
                 :padding "10px"
                 :border-radius "5px"
                 :background "#f9f9f9"
                 :margin-top "10px"}}
   [:textarea {:value (:input @ui-state)
               :placeholder "Describe the UI component..."
               :on-change #(swap! ui-state assoc :input (-> % .-target .-value))
               :style {:width "100%"
                       :height "80px"
                       :padding "10px"
                       :border "1px solid #ccc"
                       :border-radius "5px"}}]
   [:button {:on-click fetch-ui
             :style {:margin-top "10px"
                     :padding "10px"
                     :border "none"
                     :background "#007BFF"
                     :color "#fff"
                     :cursor "pointer"
                     :border-radius "5px"}}
    "Generate UI"]
   (when (:loading? @ui-state)
     [:p {:style {:color "#888"}} "Loading..."])
   (when (:error @ui-state)
     [:p {:style {:color "red"}} (:error @ui-state)])
   [:div {:style {:margin-top "10px"
                  :border "1px dashed #999"
                  :padding "10px"
                  :background "#fff"}}
    (if-let [ui (:generated-ui @ui-state)]
      ui
      [:p "Generated UI will appear here"])]])

