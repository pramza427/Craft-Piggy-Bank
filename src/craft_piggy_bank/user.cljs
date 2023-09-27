(ns craft-piggy-bank.user
  (:require ["@supabase/supabase-js" :refer [createClient]]
            [re-frame.core :as rf]
            [clojure.walk :refer [keywordize-keys]])
  )

(def SUPABASE_URL "https://vvgtkwjuaclhuojzensg.supabase.co")
(def SUPABASE_KEY "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ2Z3Rrd2p1YWNsaHVvanplbnNnIiwicm9sZSI6ImFub24iLCJpYXQiOjE2OTQ1NTE0NzIsImV4cCI6MjAxMDEyNzQ3Mn0.3XNebFROPUYpOXkAdaTSX_2ox-ay_FBPkvx7MxvNGQQ")

(def supabase (createClient SUPABASE_URL SUPABASE_KEY))

(rf/reg-event-db
  ::remove-error
  (fn [db _]
    (dissoc db :error)))

(rf/reg-event-fx
  ::set-error
  (fn [{:keys [db]} [_ val]]
    {:db (assoc db :error val)
     :dispatch-later {:ms 5000 :dispatch [::remove-error]}}))

(rf/reg-event-db
  ::remove-success
  (fn [db _]
    (dissoc db :success)))

(rf/reg-event-fx
  ::set-success
  (fn [{:keys [db]} [_ val]]
    {:db (assoc db :success val)
     :dispatch-later {:ms 5000 :dispatch [::remove-success]}}))

(rf/reg-sub
  :user/get-user
  (fn [db _]
    (get db :user)))

(rf/reg-event-db
  ::sign-out
  (fn [db _]
    (-> db
        (dissoc :projects)
        (dissoc :user)
        (dissoc :current-project)
        (dissoc :loading?))))

(rf/reg-event-fx
  ::sign-in
  (fn [{:keys [db]} [_ user]]
    {:db (-> db
             (assoc :user (select-keys user [:id :email]))
             (assoc :loading? true))
     :dispatch [:user/get-db-projects]}))

(rf/reg-event-db
  ::add-project-local
  (fn [db [_ project]]
    (when (and (some? (:t_name project)) (some? (:f_rate project)))
      (-> db
          (assoc-in [:projects (:id project)] project)
          (assoc :loading? false)))))

(rf/reg-event-db
  :user/add-db-project
  (fn [db [_ {:keys [t_name f_rate b_add_expenses] :as project}]]
    (let [time (+
                 (* 3600 (:hours project))
                 (* 60 (:minutes project))
                 (:seconds project))]
      (cond
        (empty? (:user db))
        (assoc-in db [:projects (:id project)] {:id (:id project) :t_name t_name :i_time time :f_rate f_rate :b_add_expenses b_add_expenses})

        :else
        (do
          (.then (-> supabase
                     (.from "projects")
                     (.insert (clj->js [{"t_name" t_name "i_time" time "f_rate" f_rate "b_add_expenses" b_add_expenses}]))
                     (.select))
                 (fn [result]
                   (let [keyed-result (keywordize-keys (js->clj result))]
                     (if (:error keyed-result)
                       (rf/dispatch [::set-error (:error keyed-result)])
                       (rf/dispatch [::add-project-local (into {} (:data keyed-result))])))))
          (assoc db :loading? true))))))

(rf/reg-event-db
  ::set-projects-local
  (fn [db [_ projects]]
    (let [project-map (->> projects
                           (map (fn [project]
                                  [(:id project) project]))
                           (into {}))]
      (-> db
          (assoc :projects project-map)
          (assoc :loading? false)))))

(rf/reg-event-db
  :user/get-db-projects
  (fn [db _]
    (let [ret (-> supabase
                  (.from "projects")
                  (.select "*,
                  expenses(*)"))]
      (.then ret
             (fn [result]
               (let [keyed-result (keywordize-keys (js->clj result))]
                 (if (:error keyed-result)
                   (rf/dispatch [::set-error (:error keyed-result)])
                   (rf/dispatch [::set-projects-local (:data keyed-result)])))))
      (assoc db :loading? true))))

(rf/reg-event-db
  ::delete-project-local
  (fn [db [_ project-id]]
    (-> db
        (update :projects dissoc project-id)
        (dissoc :current-project)
        (assoc :loading? false))))

(rf/reg-event-db
  :user/delete-db-project
  (fn [db [_ project-id]]
    (let [ret (-> supabase
                  (.from "projects")
                  (.delete)
                  (.eq "id" project-id))]
      (cond
        (empty? (:user db))
        (-> db
            (update :projects dissoc project-id)
            (dissoc :current-project))

        :else
        (do (.then ret
                   (fn [result]
                     (let [keyed-result (keywordize-keys (js->clj result))]
                       (if (:error keyed-result)
                         (rf/dispatch [::set-error (:error keyed-result)])
                         (rf/dispatch [::delete-project-local project-id])))))
            (assoc db :loading? true)) ))))

(rf/reg-event-db
  ::add-expense-local
  (fn [db [_ {:keys [id t_name f_cost r_project] :as expense}]]
    (-> db
        (update-in [:projects (:current-project db) :expenses] conj {:id id :t_name t_name :f_cost f_cost :r_project r_project})
        (assoc :loading? false))))

(rf/reg-event-db
  :user/add-db-expense
  (fn [db [_ {:keys [t_name f_cost] :as expense}]]
    (cond
      (empty? (:user db))
      (update-in db [:projects (:current-project db) :expenses] conj {:id (str (random-uuid)) :t_name t_name :f_cost f_cost :r_project (:current-project db)})

      :else
      (do
        (.then (-> supabase
                   (.from "expenses")
                   (.insert (clj->js [{"t_name" t_name "f_cost" f_cost "r_user" (get-in db [:user :id]) "r_project" (:current-project db)}]))
                   (.select))
               (fn [result]
                 (let [keyed-result (keywordize-keys (js->clj result))]
                   (if (:error keyed-result)
                     (rf/dispatch [::set-error (:error keyed-result)])
                     (rf/dispatch [::add-expense-local (into {} (:data keyed-result))])))))
        (assoc db :loading? true)))))

(rf/reg-event-db
  ::delete-expense-local
  (fn [db [_ expense-id]]
    (let [expenses (into [] (filter (fn [expense] (not= (:id expense) expense-id))
                                    (get-in db [:projects (:current-project db) :expenses])))]
      (-> db
          (assoc-in [:projects (get db :current-project) :expenses] expenses)
          (assoc :loading? false)))))

(rf/reg-event-db
  :user/delete-db-expense
  (fn [db [_ expense-id]]
    (cond
      (empty? (:user db))
      (let [expenses (into [] (filter (fn [expense] (not= (:id expense) expense-id))
                                      (get-in db [:projects (:current-project db) :expenses])))]
        (assoc-in db [:projects (get db :current-project) :expenses] expenses))

      :else
      (do
        (.then (-> supabase
                   (.from "expenses")
                   (.delete)
                   (.eq "id" expense-id))
               (fn [result]
                 (let [keyed-result (keywordize-keys (js->clj result))]
                   (if (:error keyed-result)
                     (rf/dispatch [::set-error (:error keyed-result)])
                     (rf/dispatch [::delete-expense-local expense-id])))))
        (assoc db :loading? true)))))

(rf/reg-event-db
  ::update-project-local
  (fn [db [_ updated-project]]
    (-> db
        (assoc-in [:projects (:id updated-project)] updated-project)
        (assoc :loading? false))))

(rf/reg-event-db
  :user/update-db-project
  (fn [db [_ project-edits]]
    (let [updated-project (assoc
                            (select-keys project-edits [:id :t_name :f_rate :b_add_expenses :expenses])
                            :i_time (+
                                      (* 3600 (:hours project-edits))
                                      (* 60 (:minutes project-edits))
                                      (:seconds project-edits)))
          ret (-> supabase
                  (.from "projects")
                  (.update #js{:t_name (:t_name updated-project) :f_rate (:f_rate updated-project)
                               :i_time (:i_time updated-project) :b_add_expenses (:b_add_expenses updated-project)})
                  (.eq "id" (:id project-edits)))]
      (cond
        (empty? (:user db))
        (assoc-in db [:projects (:id updated-project)] updated-project)

        :else
        (do
          (.then ret
                 (fn [result]
                   (let [keyed-result (keywordize-keys (js->clj result))]
                     (if (:error keyed-result)
                       (rf/dispatch [::set-error (:error keyed-result)])
                       (rf/dispatch [::update-project-local updated-project])))))
          (assoc db :loading? true))))))

(rf/reg-event-db
  ::add-time-local
  (fn [db [_ time project-id]]
    (-> db
        (assoc-in [:projects project-id :i_time] time)
        (assoc :loading? false))))

(rf/reg-event-db
  :user/add-db-time
  (fn [db [_ time project-id]]
    (let [time-combined (+
                          (* 3600 (:hours time))
                          (* 60 (:minutes time))
                          (:seconds time))
          current-time (get-in db [:projects (get db :current-project) :i_time])
          total-time (+ time-combined current-time)]
      (cond
        (empty? (:user db))
        (assoc-in db [:projects project-id :i_time] total-time)

        :else
        (do
          (.then (-> supabase
                     (.from "projects")
                     (.update #js{:i_time total-time})
                     (.eq "id" project-id))
                 (fn [result]
                   (let [keyed-result (keywordize-keys (js->clj result))]
                     (if (:error keyed-result)
                       (rf/dispatch [::set-error (:error keyed-result)])
                       (rf/dispatch [::add-time-local total-time project-id])))))
          (assoc db :loading? true))))))

(rf/reg-event-db
  :user/update-db-time
  (fn [db _]
    (when (not-empty (:user db))
      (let [project-id (:current-project db)
            time (get-in db [:projects project-id :i_time])]
        (.then (-> supabase
                   (.from "projects")
                   (.update #js{:i_time time})
                   (.eq "id" project-id))
               (fn [result]
                 (let [keyed-result (keywordize-keys (js->clj result))]
                   (if (:error keyed-result)
                     (rf/dispatch [::set-error (:error keyed-result)])
                     (rf/dispatch [::set-success "Time added to DB"])))))
        (assoc db :loading? false)))))

(defn sign-in [resp]
  (if (and (get-in resp [:data :user :confirmation_sent_at])
           (not (get-in resp [:data :session :access_token])))
    (rf/dispatch [::set-success "Confirmation Email Sent"])
    (do (rf/dispatch [::sign-in (get-in resp [:data :user])])
        (rf/dispatch [::set-success (str "Logged in as " (get-in resp [:data :user :email]))]))))

(defn sign-up-submitted [email password]
  (.then (-> supabase .-auth (.signUp #js{:email email :password password}))
         (fn [resp]
           (if (some? (.-error resp))
             (rf/dispatch [::set-error (-> ^js resp .-error .-message)])
             (sign-in (keywordize-keys (js->clj resp)))))))

(defn sign-in-submitted [email password]
  (.then (-> supabase .-auth (.signInWithPassword #js{:email email :password password}))
         (fn [resp]
           (if (some? (.-error resp))
             (rf/dispatch [::set-error (-> ^js resp .-error .-message)])
             (sign-in (keywordize-keys (js->clj resp)))))))

(defn sign-out-submitted []
  (.then (-> supabase .-auth .signOut)
         #(rf/dispatch [::sign-out])))