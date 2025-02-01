(ns second-brain-studio-webapp.markdown-editor
  (:require
   [reagent.core :as r]
   ["markdown-it" :as MarkdownIt]
   [second-brain-studio-webapp.note-view :as note-view]))

;; Initialize the Markdown parser
(def markdown-parser (MarkdownIt.))

(defn markdown-editor []
  (let [content (r/atom "### Hello World!\n\nWrite some **Markdown** here!")
        mode (r/atom :edit)
        title (r/atom "Untitled") ;; Default title
        editing-title (r/atom false)] ;; Track if the title is being edited] ;; Mode can be :edit or :preview
    (fn []
      [:div {:style {:display "flex"
                     :flex-direction "column"
                     :gap "20px"}
             :class "markdown-window"}
            ;; Left Pane: Note view
       ;; Title Section
       [:div {:style {:margin-bottom "10px"}}
        (if @editing-title
          ;; Title in edit mode
          [:input {:type "text"
                   :value @title
                   :auto-focus true
                   :on-change #(reset! title (-> % .-target .-value))
                   :on-blur #(reset! editing-title false)
                   :on-key-down #(when (= (.-key %) "Enter")
                                   (reset! editing-title false))
                   :style {:font-size "20px"
                           :font-weight "bold"
                           :margin-bottom "10px"
                           :border "1px solid #ccc"
                           :border-radius "5px"
                           :padding "5px"}
                   :class "markdown-title"}]
          ;; Title in label mode
          [:h2 {:on-click #(reset! editing-title true)
                :style {:font-size "20px"
                        :font-weight "bold"
                        :margin-bottom "10px"
                        :cursor "pointer"}}
           @title])]
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
