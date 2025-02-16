(ns second-brain-studio-webapp.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [second-brain-studio-webapp.events :as events]
   [second-brain-studio-webapp.views :as views]
   [second-brain-studio-webapp.config :as config]

   [second-brain-studio-webapp.cognito-auth :refer [auth-provider]]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/render [auth-provider [views/main-panel]]
     root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))