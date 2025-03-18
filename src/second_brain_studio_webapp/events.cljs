(ns second-brain-studio-webapp.events
  (:require
   [re-frame.core :as re-frame]
   [second-brain-studio-webapp.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
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

;; Save note status events
(re-frame/reg-event-db
 ::set-current-content
 (fn [db [_ content]]
   (assoc db :current-content content)))

(re-frame/reg-event-db
 ::set-last-saved-content
 (fn [db [_ content]]
   (assoc db :last-saved-content content)))

(re-frame/reg-event-db
 ::set-saving
 (fn [db [_ saving?]]
   (assoc db :saving? saving?)))

(re-frame/reg-event-db
 ::set-save-error
 (fn [db [_ error]]
   (assoc db :save-error error)))

(re-frame/reg-event-fx
 ::save-note
 (fn [{:keys [db]} _]
   (let [current-content (:current-content db)
         saving? (:saving? db)]
     (if (and (not saving?) current-content)
       ;; Dispatch to the existing save function - this will need to be properly connected
       {:dispatch [::trigger-save-note current-content]}
       {})))) ;; No effect if already saving or no content

;; Actual save trigger
(re-frame/reg-event-fx
 ::trigger-save-note
 (fn [{:keys [db]} [_ content]]
   (js/console.log "Triggering save from re-frame" content)
   ;; This event needs access to the save function from markdown-editor
   ;; For now, we'll just update the state to show we tried to save
   {:db (-> db
            (assoc :saving? true)
            (assoc :current-content content))}))

