(ns second-brain-studio-webapp.line-chart
    (:require [reagent.core :as r]
              ["recharts" :refer [ResponsiveContainer LineChart Line XAxis YAxis Tooltip CartesianGrid Legend]]))
  
  (defonce market-data
    [{:year 2023 :tam 80000000 :sam 30000000 :som 10000000}
     {:year 2025 :tam 200000000 :sam 90000000 :som 20000000}
     {:year 2030 :tam 300000000 :sam 180000000 :som 90000000}
     {:year 2033 :tam 4000000000 :sam 250000000 :som 125000000}])
  
  ;; Function to dynamically set y-axis ticks based on data range
  (defn dynamic-ticks [max-value]
    (let [tick-step (if (<= max-value 300000000) 50000000 250000000)]
      (range 0 (+ max-value tick-step) tick-step)))
  
  (defn market-chart []
    (let [max-y (apply max (map (fn [d] (max (:tam d) (:sam d) (:som d))) market-data))
          y-ticks (dynamic-ticks max-y)]
      (fn []
        [:> ResponsiveContainer {:width "100%" :height 400}
         [:> LineChart {:data market-data}
          [:> CartesianGrid {:strokeDasharray "3 3"}]
          [:> XAxis {:dataKey "year"}]
          [:> YAxis {:domain [0 max-y] :tickFormatter #(str "$" (/ % 1000000) "M") :ticks (clj->js y-ticks)}]
          [:> Tooltip]
          [:> Legend]
          [:> Line {:type "monotone" :dataKey "tam" :stroke "blue" :name "TAM (Total Addressable Market)" :dot true}]
          [:> Line {:type "monotone" :dataKey "sam" :stroke "green" :name "SAM (Serviceable Available Market)" :dot true}]
          [:> Line {:type "monotone" :dataKey "som" :stroke "red" :name "SOM (Serviceable Obtainable Market)" :dot true}]]])))
