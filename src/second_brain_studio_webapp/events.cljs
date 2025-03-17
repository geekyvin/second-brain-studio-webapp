(ns second-brain-studio-webapp.events
  (:require
   [re-frame.core :as re-frame]
   [second-brain-studio-webapp.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::login-success
 (fn [db [_ user-info]]
   (-> db
       (assoc-in [:auth :logged-in] true)
       (assoc-in [:auth :user] {:email (.. user-info -attributes -email)
                               :sub (.. user-info -attributes -sub)
                               :provider "cognito"}))))

(re-frame/reg-event-db
 ::logout
 (fn [db _]
   (update db :auth dissoc :logged-in :user)))

