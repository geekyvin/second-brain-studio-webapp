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
                     :gap "20px"}
             :class "markdown-window"}
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
                           :class "markdown-editor"
                           :style {
                                   :padding "10px"
                                   :font-family "monospace"
                                   :font-size "14px"
                                   :border-radius "5px"}}]
         :preview [:div {:class "markdown-editor"
                         :style {:overflow-y "auto"
                                 :border-radius "5px"
                                 :background-color "#f9f9f9"}}
                   [:div {:dangerouslySetInnerHTML
                          #js {:__html (.render markdown-parser @content)}}]])])))
