(ns second-brain-studio-webapp.ui-generator
  (:require [reagent.core :as r]
            [cljs.reader :as reader]
            [clojure.string :as str]))

;; 🔹 State Management
(defonce ui-state (r/atom {:input ""
                           :generated-ui nil
                           :loading? false
                           :error nil}))

;; Extract Hiccup UI Code from the AI Response
(defn extract-ui-code [response-body]
  (let [pattern #"(?s)\$ui-code-start\$(.*?)\$ui-code-end\$"
        match   (re-find pattern response-body)]
    (if match
      (-> (second match)
          str/trim
          (str/replace #"\}$" ""))
      nil)))

;; 🔹 Convert UI Code String to ClojureScript Hiccup
(defn parse-hiccup [hiccup-str]
  (try
    (reader/read-string hiccup-str) ;; Parse string into Clojure data structure
    (catch js/Error e
      (println "Error parsing Hiccup:" (.-message e))
      nil))) ;; Return nil if parsing fails

;; 🔹 Fetch UI Code from Backend
(defn fetch-ui []
  (swap! ui-state assoc :loading? true)
  (-> (js/fetch "http://localhost:3000/generate-ui"
                #js {:method "POST"
                     :headers #js {"Content-Type" "application/json"}
                     :body (js/JSON.stringify #js {:prompt (:input @ui-state)})})
      (.then (fn [response]
               (if (.-ok response)
                 (.json response) ;; Parse JSON response
                 (throw (js/Error (str "HTTP error! status: " (.-status response)))))))
      (.then (fn [data]
               (let [raw-code (extract-ui-code (.-body data)) ;; 
                     hiccup-code (parse-hiccup raw-code)]
                 (println "API Response:" data)
                 (println "Raw Code:" raw-code)
                 (println "Hiccup Code:" hiccup-code)
                 (swap! ui-state assoc
                        :generated-ui hiccup-code
                        :loading? false
                        :error nil))))
      (.catch (fn [error]
                (js/console.error "Error generating UI:" error)
                (swap! ui-state assoc
                       :error (str "Error: " error)
                       :loading? false)))))

;; 🔹 UI Generator Component
(defn ui-generator []
  [:div {:style {:width "100%"
                 :border "1px solid #ccc"
                 :padding "10px"
                 :border-radius "5px"
                 :background "#f9f9f9"
                 :margin-top "10px"}}
   ;; Text Input for User Query
   [:textarea {:value (:input @ui-state)
               :placeholder "Describe the UI component..."
               :on-change #(swap! ui-state assoc :input (-> % .-target .-value))
               :style {:width "100%"
                       :height "80px"
                       :padding "10px"
                       :border "1px solid #ccc"
                       :border-radius "5px"}}]

   ;; Button to Generate UI
   [:button {:on-click fetch-ui
             :style {:margin-top "10px"
                     :padding "10px"
                     :border "none"
                     :background "#007BFF"
                     :color "#fff"
                     :cursor "pointer"
                     :border-radius "5px"}}
    "Generate UI"]

   ;; Loading Indicator
   (when (:loading? @ui-state)
     [:p {:style {:color "#888"}} "Loading..."])

   ;; Error Display
   (when (:error @ui-state)
     [:p {:style {:color "red"}} (:error @ui-state)])

   ;; Render UI or Placeholder
   [:div {:style {:margin-top "10px"
                  :border "1px dashed #999"
                  :padding "10px"
                  :background "#fff"}}
    (if (:generated-ui @ui-state)
      [:div (:generated-ui @ui-state)] ;; ✅ Render parsed Hiccup UI
      [:p "Generated UI will appear here"])]])

