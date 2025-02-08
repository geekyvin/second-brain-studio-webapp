(ns second-brain-studio-webapp.ui-generator
  (:require
   [reagent.core :as r]
   [cljs.js :as cljs]
   [cljs.reader :as reader])) ;; Import reader to parse Clojure strings

(defonce ui-state (r/atom {:input ""
                           :generated-ui nil
                           :loading? false
                           :error nil}))

;; Function to Evaluate & Render UI at Runtime
(defn evaluate-cljs [cljs-code]
  (if (string? cljs-code)
    (let [parsed-code (reader/read-string cljs-code)] ;; ✅ Parse as Clojure data
      (cljs/eval-str
       (cljs/empty-state)
       (str "(do " parsed-code ")") ;; ✅ Ensure it evaluates properly
       nil
       {:eval cljs/js-eval}
       (fn [{:keys [value error]}]
         (if error
           (swap! ui-state assoc :error (str "Evaluation error: " error))
           (do
             (println "Successfully evaluated UI code:" value)
             (swap! ui-state assoc :generated-ui value))))))
    (swap! ui-state assoc :error "Error: Received non-string code for evaluation.")))

(defn extract-ui-code [response-body]
  (let [pattern #"(?s)\{\s*:ui-code\s+(.*?)\}" ;; Regex to extract `:ui-code`
        match (re-find pattern response-body)]
    (if match
      (second match) ;; Extract UI Code
      nil))) ;; If no match, return nil

 (defn fetch-ui []
   (swap! ui-state assoc :loading? true)
   (-> (js/fetch "http://localhost:3000/generate-ui"
                 #js {:method "POST"
                      :headers #js {"Content-Type" "application/json"}
                      :body (js/JSON.stringify #js {:prompt (:input @ui-state)})})
       (.then (fn [response]
                (if (.-ok response)
                  (.text response) ;; ✅ Parse response as text first
                  (throw (js/Error (str "HTTP error! status: " (.-status response)))))))
       (.then (fn [response-text]
                (println "Raw Response from API:" response-text) ;; Debugging
                (let [cljs-code (extract-ui-code response-text)]
                  (println "Extracted CLJS Code:" cljs-code)
                  (if (string? cljs-code)
                    (evaluate-cljs cljs-code) ;; ✅ Evaluate extracted CLJS code
                    (swap! ui-state assoc :error "Error: Failed to extract code from API."))))))
   (.catch (fn [error]
             (js/console.error "Error generating UI:" error)
             (swap! ui-state assoc
                    :error (str "Error: " error)
                    :loading? false))))


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
               :style {:width "100%" :height "80px"
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
   (when (:loading? @ui-state) [:p {:style {:color "#888"}} "Loading..."])
   (when (:error @ui-state) [:p {:style {:color "red"}} (:error @ui-state)])
   (when (:generated-ui @ui-state)
     [:div {:style {:margin-top "10px"
                    :border "1px dashed #999"
                    :padding "10px"
                    :background "#fff"}}
      [(fn [] ((:generated-ui @ui-state))) ;; ✅ Render the evaluated component
       ]])])
