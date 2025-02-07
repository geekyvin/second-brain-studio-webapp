(ns second-brain-studio-webapp.editor
  (:require
   [reagent.core :as r]
   [second-brain-studio-webapp.app-state :refer [app-state]] ;; Import app-state
   ["@codemirror/view" :refer [EditorView]]
   ["@codemirror/state" :refer [EditorState]]
   ["@codemirror/lang-markdown" :refer [markdown]]
   ["markdown-it" :as MarkdownIt]))

;; Markdown Parser
(def markdown-parser (MarkdownIt.))


(defn markdown-editor []
  (let [editor-container (r/atom nil)
        editor-instance (r/atom nil)
        mode (r/atom :edit)
        title (r/atom "Untitled")
        editing-title (r/atom false)]

    ;; Initialize CodeMirror Editor
    (r/create-class
     {:component-did-mount
      (fn [_]
        (when-let [node @editor-container]
          (let [state (EditorState.create
                       #js {:doc (:selected-note @app-state)
                            :extensions #js [(markdown)]})
                view (EditorView. #js {:state state :parent node})]
            (reset! editor-instance view))))

      :component-will-unmount
      (fn [_]
        (when-let [instance @editor-instance]
          (.destroy instance)))

      :reagent-render
      (fn []
        [:div {:style {:flex "1" :padding "20px"}}
         ;; Title Section
         [:div {:style {:margin-bottom "10px"}}
          (if @editing-title
            [:input {:type "text"
                     :value @title
                     :auto-focus true
                     :on-change #(reset! title (-> % .-target .-value))
                     :on-blur #(reset! editing-title false)
                     :on-key-down #(when (= (.-key %) "Enter")
                                     (reset! editing-title false))
                     :style {:font-size "20px"
                             :font-weight "bold"
                             :border "1px solid #ccc"
                             :border-radius "5px"
                             :padding "5px"}}]
            [:h2 {:on-click #(reset! editing-title true)
                  :style {:font-size "20px"
                          :font-weight "bold"
                          :cursor "pointer"}}
             @title])]

         ;; Mode Toggle
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

         ;; Editor / Preview
         (case @mode
           :edit [:div {:ref #(reset! editor-container %)
                        :style {:border "1px solid #ccc"
                                :border-radius "5px"
                                :padding "10px"
                                :height "400px"
                                :overflow-y "auto"}}]
           :preview [:div {:style {:overflow-y "auto"
                                   :border-radius "5px"
                                   :background-color "#f9f9f9"}}
                     [:div {:dangerouslySetInnerHTML
                            #js {:__html (.render markdown-parser (:selected-note @app-state))}}]])])})))
