(ns second-brain-studio-webapp.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

;; Save status subscriptions
(re-frame/reg-sub
 ::current-content
 (fn [db]
   (:current-content db)))

(re-frame/reg-sub
 ::last-saved-content
 (fn [db]
   (:last-saved-content db)))

(re-frame/reg-sub
 ::saving?
 (fn [db]
   (:saving? db)))

(re-frame/reg-sub
 ::save-error
 (fn [db]
   (:save-error db)))
