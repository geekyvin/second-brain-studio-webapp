(ns second-brain-studio-webapp.views
  (:require
   [re-frame.core :as re-frame]
   [second-brain-studio-webapp.subs :as subs]
   [second-brain-studio-webapp.markdown-editor :as markdown-editor]))
  
(defn main-panel []
  (println "main-panel called")
  (println "markdown-editor: " markdown-editor/markdown-editor)
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1 "Hello from " @name]
     [markdown-editor/markdown-editor]
    ]))

