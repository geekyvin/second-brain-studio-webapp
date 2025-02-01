(ns second-brain-studio-webapp.md-component
  (:require [reagent.core :as r]
            ["marked" :as marked]))

(defonce app-state (r/atom {:lines [""]})) ; Initial state with one empty line.

(defn render-markdown [content]
  [:div {:dangerouslySetInnerHTML
         {:__html (marked content)}}]) ; Use Marked to render Markdown.

(defn editor-line [index content]
  (let [editing (r/atom false)] ; Track if the current line is being edited.
    (fn []
      [:div.editor-line
       (if @editing
         [:input {:type "text"
                  :value content
                  :auto-focus true
                  :on-blur #(reset! editing false)
                  :on-change #(swap! app-state assoc-in [:lines index] (.. % -target -value))
                  :on-key-down #(when (= (.-key %) "Enter")
                                  (reset! editing false)
                                  (when (not (str/blank? (.. % -target -value))) ; Only add if non-empty
                                    (swap! app-state update :lines conj "")))}]
         [:div {:on-click #(reset! editing true)}
          (if (str/blank? content)
            [:span.placeholder "Type here..."]
            [render-markdown content])])])))

(defn editor []
  [:div.editor
   ;; Render each line in the state.
   (for [[index line] (map-indexed vector (:lines @app-state))]
     ^{:key index}
     [editor-line index line])
   ;; Button to add a new line.
   [:div.new-line
    {:on-click #(swap! app-state update :lines conj "")}
    "+"]])

