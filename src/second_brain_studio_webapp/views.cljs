(ns second-brain-studio-webapp.views
  (:require
   [re-frame.core :as re-frame]
   [second-brain-studio-webapp.subs :as subs]
    [second-brain-studio-webapp.prosemirror :refer [prosemirror-editor]]))
  
(defn main-panel []
  (println "main-panel called")
  (println "prosemirror-editor: " prosemirror-editor)
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1 "Hello from " @name]
     [prosemirror-editor]
    ]))

