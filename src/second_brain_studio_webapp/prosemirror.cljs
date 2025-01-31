(ns second-brain-studio-webapp.prosemirror
   (:require [reagent.core :as r]
    ["prosemirror-model" :refer [Schema DOMParser DOMSerializer]]
    ["prosemirror-state" :refer [EditorState Plugin]]
    ["prosemirror-view" :refer [EditorView]]
    ["prosemirror-schema-basic" :refer [schema]]
    ["prosemirror-markdown" :refer [markdownParser markdownSerializer]])
  
  (defn create-editor [element editor-state]
    (EditorView. element
                 #js {:state editor-state}))
  
  (defn prosemirror-editor []
    (let [editor-container (r/atom nil)
          editor-instance (r/atom nil)]
      (r/create-class
       {:component-did-mount
        (fn [this]
          (let [editor-state (EditorState.create
                              #js {:schema schema})
                container (.querySelector (r/dom-node this) "#editor")]
            (reset! editor-instance (create-editor container editor-state))))
        :component-will-unmount
        (fn [_]
          (when-let [instance @editor-instance]
            (.destroy instance)))
        :reagent-render
        (fn []
          [:div
           [:div#editor {:style {:border "1px solid #ccc"
                                 :min-height "300px"}}]])}))))
