(ns second-brain-studio-webapp.prosemirror
  (:require
   [reagent.core :as r]
   ["prosemirror-model" :refer [Schema DOMParser DOMSerializer]]
   ["prosemirror-state" :refer [EditorState]]
   ["prosemirror-view" :refer [EditorView]]
   ["prosemirror-schema-basic" :refer [schema]]))

(defn create-editor [element]
  (let [editor-state (.create EditorState #js {:schema schema})]
    (EditorView. element #js {:state editor-state})))

(defn prosemirror-editor []
  (let [editor-ref (r/atom nil)   ;; Reference to store the ProseMirror instance
        dom-node (r/atom nil)]   ;; Reference to the DOM node for the editor
    (r/create-class
     {:component-did-mount
      (fn [_]
        (when-let [node @dom-node]
          (reset! editor-ref (create-editor node)))) ;; Initialize the editor
      :component-will-unmount
      (fn [_]
        (when-let [editor @editor-ref]
          (.destroy editor))) ;; Clean up the editor instance
      :reagent-render
      (fn []
        [:div#editor
         {:ref #(reset! dom-node %)
          :style {:border "1px solid #ccc"
                  :min-height "300px"}}])})))
