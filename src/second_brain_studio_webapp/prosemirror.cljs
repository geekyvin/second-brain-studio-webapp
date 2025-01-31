(ns second-brain-studio-webapp.prosemirror
  (:require
   [reagent.core :as r]
   ["prosemirror-model" :refer [Schema DOMParser DOMSerializer]]
   ["prosemirror-state" :refer [EditorState]]
   ["prosemirror-view" :refer [EditorView]]
   ["prosemirror-schema-basic" :refer [schema]]
   ["prosemirror-markdown" :refer [MarkdownParser defaultMarkdownParser markdownSyntax]]))

;; Use the default markdown parser (pre-configured)
(def markdown-parser defaultMarkdownParser)

(def extended-schema
  (Schema. #js {:nodes (clj->js {:doc {:content "block+"}
                                 :paragraph {:content "inline*"
                                             :group "block"
                                             :parseDOM #js [#js {:tag "p"}]
                                             :toDOM (fn [] #js ["p" 0])}
                                 :text {:group "inline"}
                                 :customHeading {:content "text*"
                                                 :group "block"
                                                 :parseDOM #js [#js {:tag "h1"}]
                                                 :toDOM (fn [] #js ["h1" 0])}})
                :marks (clj->js {:strong {:parseDOM #js [#js {:tag "strong"}]
                                          :toDOM (fn [] #js ["strong" 0])}
                                 :em {:parseDOM #js [#js {:tag "em"}]
                                      :toDOM (fn [] #js ["em" 0])}})}))


(defn create-editor [element]
  ;; Parse initial markdown content
  (let [doc (.parse markdown-parser "# Initial Markdown Content")
        editor-state (.create EditorState #js {:doc doc
                                               :schema extended-schema})]
    ;; Create and return the ProseMirror editor view
    (EditorView. element #js {:state editor-state})))

(defn toolbar [editor-ref]
  [:div.toolbar {:style {:margin-bottom "10px"}}
   ;; Heading Button
   [:button {:style {:margin-right "5px"}
             :on-click #(when-let [editor @editor-ref]
                          (let [tr (.. editor -state -tr)]
                            (.setNodeMarkup tr (.. editor -state -selection -$from -pos) "heading" #js {:level 1})
                            (.dispatch editor tr)))}
    "H1"]
   ;; Bold Button
   [:button {:style {:margin-right "5px"}
             :on-click #(when-let [editor @editor-ref]
                          (let [state (.-state editor)
                                tr (.. state -tr)]
                            (.addMark tr
                                      (.. state -selection -from)
                                      (.. state -selection -to)
                                      (.. (.-schema state) -marks -strong))
                            (.dispatch editor tr)))}
    "Bold"]
   ;; Italic Button
   [:button {:style {:margin-right "5px"}
             :on-click #(when-let [editor @editor-ref]
                          (let [state (.-state editor)
                                tr (.. state -tr)]
                            (.addMark tr
                                      (.. state -selection -from)
                                      (.. state -selection -to)
                                      (.. (.-schema state) -marks -em))
                            (.dispatch editor tr)))}
    "Italic"]])


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
         ;; Toolbar
         [toolbar editor-ref]
         ;; Editor Container
         [:div#editor {:ref #(reset! dom-node %)
                       :style {:border "1px solid #ccc"
                               :min-height "300px"
                               :padding "10px"
                               :border-radius "5px"
                               :background-color "#f9f9f9"}}]])})))


