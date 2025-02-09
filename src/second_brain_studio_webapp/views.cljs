(ns second-brain-studio-webapp.views
  (:require
   [re-frame.core :as re-frame]
   [second-brain-studio-webapp.subs :as subs]
   [second-brain-studio-webapp.note-view :as note-view]
   [second-brain-studio-webapp.line-chart :as line-chart]
   [second-brain-studio-webapp.sidebar :as sidebar]
   [second-brain-studio-webapp.editor :as editor]
   [second-brain-studio-webapp.ui-generator :as ui-generator]
   [second-brain-studio-webapp.left-pane :as left-pane]
   [second-brain-studio-webapp.markdown-editor :as markdown-editor]))

(defn main-panel []
  (println "main-panel called")
  (println "markdown-editor: " markdown-editor/markdown-editor) ;; Debug

  (let [name (re-frame/subscribe [::subs/name])]
    [:div {:style {:display "flex" :height "100vh"}}
     ;; Sidebar (left panel)
     [:div {:style {:width "300px"
                    :border-right "1px solid #ccc"
                    :padding "10px"}}
      ;;[sidebar/sidebar]
      [left-pane/left-pane]]
     ;; Main Content (Markdown Editor)
     [:div {:style {:flex "1"
                    :padding "20px"}}
      ;;[:h1 (str "Hello from " @name)]
      [markdown-editor/markdown-editor]
      [:h3 "Generated UI"]
      [ui-generator/ui-generator]]]))

