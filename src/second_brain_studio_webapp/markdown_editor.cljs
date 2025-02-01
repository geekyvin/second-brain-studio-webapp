(ns second-brain-studio-webapp.markdown-editor
  (:require
   [reagent.core :as r]
   ["markdown-it" :as MarkdownIt]))

;; Initialize the Markdown parser
(def markdown-parser (MarkdownIt.))

(defn markdown-editor []
  (let [content (r/atom "# Hello Markdown\n\nWrite some **Markdown** here!")
        preview-html (r/atom nil)]
    (fn []
      [:div {:style {:display "flex"
                     :gap "20px"
                     :align-items "flex-start"}}
       ;; Textarea for Markdown input
       [:textarea {:value @content
                   :on-change #(reset! content (-> % .-target .-value))
                   :style {:width "50%"
                           :height "400px"
                           :padding "10px"
                           :font-family "monospace"
                           :font-size "14px"
                           :border "1px solid #ccc"
                           :border-radius "5px"}}]
       ;; Markdown preview (HTML rendered)
       [:div {:style {:width "50%"
                      :height "400px"
                      :overflow-y "auto"
                      :padding "10px"
                      :border "1px solid #ccc"
                      :border-radius "5px"
                      :background-color "#f9f9f9"}}
        [:div {:dangerouslySetInnerHTML
               #js {:__html (.render markdown-parser @content)}}]]])))
