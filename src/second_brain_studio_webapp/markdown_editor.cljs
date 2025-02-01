(ns second-brain-studio-webapp.markdown-editor
  (:require
   [reagent.core :as r]
   ["markdown-it" :as MarkdownIt]))

;; Initialize the Markdown parser
(def markdown-parser (MarkdownIt.))

(defn markdown-editor []
  (let [content (r/atom "# Hello Markdown\n\nWrite some **Markdown** here!")
        mode (r/atom :edit)] ;; Mode can be :edit or :preview
    (fn []
      [:div {:style {:display "flex"
                     :flex-direction "column"
                     :gap "20px"}}
       ;; Radio buttons to toggle modes
       [:div {:style {:margin-bottom "10px"}}
        [:label {:style {:margin-right "10px"}}
         [:input {:type "radio"
                  :name "mode"
                  :checked (= @mode :edit)
                  :on-change #(reset! mode :edit)}]
         "Edit"]
        [:label
         [:input {:type "radio"
                  :name "mode"
                  :checked (= @mode :preview)
                  :on-change #(reset! mode :preview)}]
         "Preview"]]
       ;; Conditionally render Edit or Preview mode
       (case @mode
         :edit [:textarea {:value @content
                           :on-change #(reset! content (-> % .-target .-value))
                           :style {:width "100%"
                                   :height "400px"
                                   :padding "10px"
                                   :font-family "monospace"
                                   :font-size "14px"
                                   :border "1px solid #ccc"
                                   :border-radius "5px"}}]
         :preview [:div {:style {:width "100%"
                                 :height "400px"
                                 :overflow-y "auto"
                                 :padding "10px"
                                 :border "1px solid #ccc"
                                 :border-radius "5px"
                                 :background-color "#f9f9f9"}}
                   [:div {:dangerouslySetInnerHTML
                          #js {:__html (.render markdown-parser @content)}}]])])))
