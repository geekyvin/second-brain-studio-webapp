(ns second-brain-studio-webapp.ui-generator
  (:require
   [reagent.core :as r]
   [cljs.js :as cljs]
   [ajax.core :refer [POST]]))

;; 🔹 State to store user input and generated UI
(defonce ui-state (r/atom {:input ""
                           :generated-ui nil
                           :loading? false
                           :error nil}))

;; 🔹 Function to Evaluate & Render UI at Runtime
(defn evaluate-cljs [cljs-code]
  (let [state (cljs/empty-state)]
    (cljs/eval-str
     state
     cljs-code
     nil
     {:eval cljs/js-eval}
     (fn [{:keys [value error]}]
       (if error
         (swap! ui-state assoc :error (str "Evaluation error: " error))
         (do
           (println "Successfully evaluated UI code:" value)
           ;; Store the evaluated function to be used in UI
           (swap! ui-state assoc :generated-ui value)))))))

;; 🔹 API Call to Fetch Generated UI Code
(defn fetch-ui []
   (swap! ui-state assoc :loading? true) ;; Set loading state
   (-> (js/fetch "http://localhost:3000/generate-ui"
                 #js {:method "POST"
                      :headers #js {"Content-Type" "application/json"}
                      :body (js/JSON.stringify #js {:prompt (:input @ui-state)})})
       (.then (fn [response]
                (if (.-ok response)
                  (.json response) ;; Parse JSON response properly
                  (throw (js/Error (str "HTTP error! status: " (.-status response)))))))
       (.then (fn [data]
                (println "Parsed UI Code:" data)
               ;; Extract the correct part of the response
                (let [cljs-code (get-in data ["code" "button_component"])]
                  (when cljs-code
                    (evaluate-cljs cljs-code))))
              (swap! ui-state assoc
                     :loading? false
                     :error nil)))
   (.catch (fn [error]
             (js/console.error "Error generating UI:" error)
             (swap! ui-state assoc
                    :error (str "Error: " error)
                    :loading? false))))

;; 🔹 UI Generator Component
(defn ui-generator []
  [:div {:style {:width "100%"
                 :border "1px solid #ccc"
                 :padding "10px"
                 :border-radius "5px"
                 :background "#f9f9f9"
                 :margin-top "10px"}}
   ;; Input Box
   [:textarea {:value (:input @ui-state)
               :placeholder "Describe the UI component..."
               :on-change #(swap! ui-state assoc :input (-> % .-target .-value))
               :style {:width "100%" :height "80px"
                       :padding "10px"
                       :border "1px solid #ccc"
                       :border-radius "5px"}}]

   ;; Generate Button
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

   ;; Error Message
   (when (:error @ui-state)
     [:p {:style {:color "red"}} (:error @ui-state)])

   ;; Render Evaluated Component
   (when (:generated-ui @ui-state)
     [:div {:style {:margin-top "10px"
                    :border "1px dashed #999"
                    :padding "10px"
                    :background "#fff"}}
      ;; Dynamically render the evaluated function
      [(:generated-ui @ui-state)
       [:div {:style {:margin-top "10px"
                    :border "1px dashed #999"
                    :padding "10px"
                    :background "#fff"}}
      ;; Evaluate and Render UI Component
      (evaluate-cljs (:generated-ui @ui-state))]]])])

