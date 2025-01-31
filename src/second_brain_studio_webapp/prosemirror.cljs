(ns second-brain-studio-webapp.prosemirror
  (:require
   [reagent.core :as r]
   ["prosemirror-model" :refer [Schema DOMParser DOMSerializer]]
   ["prosemirror-state" :refer [EditorState]]
   ["prosemirror-view" :refer [EditorView]]
   ["prosemirror-schema-basic" :refer [schema]]
   ["prosemirror-markdown" :refer [MarkdownParser defaultMarkdownParser markdownSyntax]]))

;; Initialize markdown-parser properly
;; (def markdown-parser (MarkdownParser. schema markdownSyntax))

;; Use the default markdown parser (pre-configured)
(def markdown-parser defaultMarkdownParser)

(defn create-editor [element]
  ;; Parse initial markdown content
  (let [doc (.parse markdown-parser "# Initial Markdown Content")
        editor-state (.create EditorState #js {:doc doc
                                               :schema schema})]
    ;; Create and return the ProseMirror editor view
    (EditorView. element #js {:state editor-state})))

(defn prosemirror-editor []
  (let [editor-ref (r/atom nil)
        dom-node (r/atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [_]
        (when-let [node @dom-node]
          (reset! editor-ref (create-editor node))))
      :component-will-unmount
      (fn [_]
        (when-let [editor @editor-ref]
          (.destroy editor)))
      :reagent-render
      (fn []
        [:div
         [:div#editor {:ref #(reset! dom-node %)
                       :style {:border "1px solid #ccc"
                               :min-height "300px"}}]])})))

