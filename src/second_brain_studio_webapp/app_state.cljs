(ns second-brain-studio-webapp.app-state
  (:require [reagent.core :as r]))

(defonce app-state (r/atom {:selected-note "Untitled"}))

