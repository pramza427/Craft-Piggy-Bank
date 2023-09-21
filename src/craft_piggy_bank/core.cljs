(ns craft-piggy-bank.core
  (:require
    [reagent.dom :as rdom]
    [reagent.core :as reagent :refer [atom]]
    [re-frame.core :as rf]
    [clojure.string :as str]
    [clojure.walk :refer [keywordize-keys]]
    [craft-piggy-bank.dialogs :as dialogs]
    [craft-piggy-bank.user :as user]
    ))

;; ---------- Helpers ----------
(defn set-local-storage [key val]
  (.setItem (.-localStorage js/window) key val))

(defn get-local-storage [key]
  (.getItem (.-localStorage js/window) key))

(defn format-time [time]
  (let [seconds (mod time 60)
        minutes (mod (Math/floor (/ time 60)) 60)
        hours (Math/floor (/ time 3600))]
    (str
      (when (> 10 hours) "0") hours
      ":"
      (when (> 10 minutes) "0") minutes
      ":"
      (when (> 10 seconds) "0") seconds)))

(defn format-money [money]
  (str "$" (.toFixed (or money 0) 2)))

(rf/reg-event-db
  ::save-to-local
  (fn [db _]
    (set-local-storage "projects"
                       (->>
                         (get db :projects) (vals)
                         (mapv (fn [e]
                                 (select-keys e [:id :t_name :i_time :f_rate :b_add_expenses :expenses])))
                         (clj->js)
                         (js/JSON.stringify)))))

(rf/reg-event-db
  ::load-from-local
  (fn [db _]
    (let [projects (keywordize-keys (js->clj (js/JSON.parse (get-local-storage "projects"))))]
      (when (some? projects)
        (assoc db :projects (into {}
                                  (map (fn [project] [(:id project) project])
                                       projects)))))))

(rf/reg-event-db
  ::print-db
  (fn [db _]
    (print db)))

;; ---------- Subscriptions ----------
(rf/reg-sub
  ::dark-mode?
  (fn [db _]
    (get db ::dark-mode)))

(rf/reg-sub
  :current-project
  (fn [db _]
    (get db :current-project)))

(rf/reg-sub
  :current-project-data
  (fn [db _]
    (get-in db [:projects (get db :current-project)])))

(rf/reg-sub
  :projects
  (fn [db [_]]
    (get db :projects)))

(rf/reg-sub
  ::current-expenses
  (fn [db _]
    (get-in db [:projects (get db :current-project) :expenses])))

(rf/reg-sub
  ::counting?
  (fn [db _]
    (get db ::counting?)))

;; ---------- Events ----------
(rf/reg-event-db
  ::set-time
  (fn [db [_ val]]
    (assoc db ::current-time val)))

(rf/reg-event-db
  ::inc-time
  (fn [db _]
    (when (get db ::counting?)
      (let [current-project (get db :current-project)]
        (assoc-in db [:projects current-project :i_time] (inc (get-in db [:projects current-project :i_time])))))))

(rf/reg-event-db
  ::set-counting
  (fn [db [_ val]]
    (assoc db ::counting? val)))

(rf/reg-event-db
  ::set-current-project
  (fn [db [_ val]]
    (assoc db :current-project val)))

(rf/reg-event-db
  ::toggle-dark-mode
  (fn [db _]
    (let [dark? (or (get-local-storage "dark") "true")
          state (if (= dark? "true") false true)]
      (set-local-storage "dark" state)
      (.classList.toggle (js/document.getElementById "app") "dark")
      (assoc db ::dark-mode state))))

(rf/reg-event-db
  ::init-dark-mode
  (fn [db _]
    (let [dark? (get-local-storage "dark")]
      (when (= dark? "true")
        (.classList.add (js/document.getElementById "app") "dark")
        (assoc db ::dark-mode true)))))

(defn save-load []
  (let [user @(rf/subscribe [:user/get-user])]
    [:div.border-t.p-2.border-gray-300.dark:border-slate-900
     (if (some? user)
       [:div
        [:div.text-center.mb-5 (:email user)]
        [:div.p-1.cursor-pointer.hover:bg-blue-50.text-center.rounded.dark:hover:bg-teal-950
         {:on-click #(user/sign-out-submitted)}
         "Sign Out"]]
       [:div
        [:div.p-1.cursor-pointer.hover:bg-blue-50.text-center.rounded.dark:hover:bg-teal-950
         {:on-click #(rf/dispatch [:dialogs/open-sign-in-dialog])}
         "Sign In"]
        [:div.p-1.cursor-pointer.hover:bg-blue-50.text-center.rounded.dark:hover:bg-teal-950
         {:on-click #(rf/dispatch [::save-to-local])}
         "Save to local"]
        [:div.p-1.cursor-pointer.hover:bg-blue-50.text-center.rounded.dark:hover:bg-teal-950
         {:on-click #(rf/dispatch [::load-from-local])}
         "Load from local"]])
     ]))

(defn project-list-item [project]
  (let [current-project @(rf/subscribe [:current-project])]
    [:div.py-2.border-b.cursor-pointer.rounded.group.border-gray-300.dark:border-gray-800
     {:class (if (= current-project (:id project)) "bg-blue-100 dark:bg-blue-900" "hover:bg-blue-50 dark:hover:bg-blue-950")
      :on-click #(rf/dispatch [::set-current-project (:id project)])}
     [:div.flex
      [:div.px-2 (:t_name project)]
      [:div.flex-grow]
      [:div.px-2 (format-time (:i_time project))]]
     [:div.flex
      [:div.px-2 (str (format-money (:f_rate project)) "/hr")]
      [:div.flex-grow]
      [:div.hidden.group-hover:flex
       [:div.px-2.mx-1.rounded.hover:bg-blue-200.dark:hover:bg-purple-900
        {:on-click #(rf/dispatch [:dialogs/open-edit-project-dialog (:id project)])}
        [:i.fas.fa-edit]]
       [:div.px-2.mx-1.rounded.hover:bg-red-200.dark:hover:bg-red-900
        {:on-click (fn [e]
                     (when (js/confirm (str "Are you sure you want to delete " (:t_name project) "?"))
                       (.stopPropagation e)
                       (rf/dispatch [:user/delete-db-project (:id project)])))}
        [:i.fas.fa-trash]]]]]))

(defn project-list []
  (let [projects @(rf/subscribe [:projects])]
    [:div.w-80.border-r.text-xl.flex.flex-col.m-2.rounded-lg.bg-white.border.border-gray-300.dark:border-slate-900.dark:bg-gray-950
     [:div.p-1.m-2.cursor-pointer.bg-teal-400.hover:bg-teal-500.text-center.rounded.border.border-teal-500.dark:bg-teal-900.dark:border-teal-800.dark:hover:bg-teal-800
      {:on-click #(rf/dispatch [:dialogs/open-add-project-dialog])}
      "Add Project"]
     [:div.overflow-auto.p-2
      (if (not-empty projects)
        (for [[key project] projects]
          ^{:key key}
          [project-list-item project])
        [:div.text-gray-800.dark:text-gray-400 "No projects to show"])]
     [:div.flex-grow]
     [save-load]]))

(defn project []
  (let [current-project-data @(rf/subscribe [:current-project-data])
        name (get current-project-data :t_name)
        time (get current-project-data :i_time)
        rate (get current-project-data :f_rate)
        earned (* rate (/ time 3600))
        counting @(rf/subscribe [::counting?])
        current-expenses @(rf/subscribe [::current-expenses])
        expense-total (reduce + (map :f_cost current-expenses))
        dark-mode? @(rf/subscribe [::dark-mode?])]
    [:div.flex.flex-col.flex-grow.text-6xl.text-center.relative.border.m-2.rounded-lg.bg-white.dark:bg-slate-950.dark:border-slate-800
     [:div.absolute.top-5.right-5.cursor-pointer.text-xl
      {:on-click #(rf/dispatch [::toggle-dark-mode])}
      [:div.flex.flex-center.w-8.h-8.rounded-lg.hover:bg-blue-100.dark:hover:bg-violet-900
       [:i.fas
        {:class (if dark-mode?
                  "fa-sun"
                  "fa-moon")}]]]
     (cond
       (not-empty current-project-data)
       [:div
        [:div.mt-10 name]
        [:div.my-5 (format-time time)]
        [:div.flex.flex-center
         [:div.flex.justify-evenly.my-5
          {:style {:width "40rem"}}
          [:div.text-5xl.rounded.border.border-gray-300.p-2.cursor-pointer.hover:bg-blue-50.dark:hover:bg-teal-950.dark:border-teal-900
           {:on-click #(rf/dispatch [:dialogs/open-add-time-dialog])}
           "Add Time"]
          [:div.text-5xl.rounded.border.border-gray-300.p-2.cursor-pointer.hover:bg-blue-50.dark:hover:bg-teal-950.dark:border-teal-900
           {:on-click #(rf/dispatch [::set-counting (not counting)])}
           (if counting "Stop" "Start")]]]
        [:div.mt-10 "Piggy Bank:"]
        [:div.my-5 (format-money (- earned expense-total))]]

       :else
       [:div.mt-10
        [:div "Select a project"]
        [:div "<---"]])
     [dialogs/error-dialog]]))

(defn expenses []
  (let [current-expenses @(rf/subscribe [::current-expenses])
        total-expenses (reduce + (map :f_cost current-expenses))
        current-project @(rf/subscribe [:current-project])
        projects @(rf/subscribe [:projects])
        hrs (/ (get-in projects [current-project :i_time]) 3600)
        rate (get-in projects [current-project :f_rate])
        earned (* rate hrs)]
    [:div.w-80.m-2.flex.flex-col.rounded-lg.bg-white.border.border-gray-300.text-xl.dark:bg-gray-950.dark:border-slate-900
     [:div.m-2.bg-red-300.text-center.p-1.rounded.border.border-gray-300.dark:bg-purple-900.dark:border-purple-800.dark:hover:bg-purple-800
      {:class (if (some? current-project)
                "cursor-pointer hover:bg-red-400"
                "cursor-not-allowed")
       :on-click #(rf/dispatch [:dialogs/new-expense {}])}
      "Add Expense"]
     [:div.overflow-auto.m-1
      (cond
        (nil? current-project)
        [:div "Expenses are tied to projects. Select a project to see its expenses here."]

        (not-empty current-expenses)
        (map-indexed (fn [idx expense]
                       ^{:key idx}
                       [:div.py-1.px-2.mb-1.flex.group.hover:bg-blue-50.rounded.dark:hover:bg-indigo-950
                        [:div (:t_name expense)]
                        [:div.flex-grow]
                        [:div.hidden.group-hover:flex
                         [:div.px-2.mx-1.rounded.hover:bg-red-200.cursor-pointer.dark:hover:bg-red-900
                          {:on-click #(rf/dispatch [:user/delete-db-expense (:id expense)])}
                          [:i.fas.fa-trash]]]
                        [:div (format-money (:f_cost expense))]])
                     current-expenses)

        :else
        [:div "No expenses yet"])]
     [:div.flex-grow]
     (when (and (some? current-project) (not-empty current-expenses))
       [:div.border-t.p-2.border-gray-300.dark:border-slate-900
        [:div.p-2
         [:div.flex
          [:div "Total earned"]
          [:div.flex-grow]
          [:div (format-money earned)]]
         [:div.text-sm.text-gray-500 (str (.toFixed hrs 2) " hrs at $" (.toFixed rate 2) "/hr")]]
        [:div.p-2.flex
         [:div "Total expenses"]
         [:div.flex-grow]
         [:div (format-money total-expenses)]]
        [:div.p-2.flex
         [:div "Piggy Bank"]
         [:div.flex-grow]
         [:div (format-money (- earned total-expenses))]]])]))

(defn main []
  [:div.flex.justify-between.min-h-full.relative.font-serif.bg-gray-100.dark:bg-black.dark:text-gray-300
   {:style {:height "100vh"}}
   [project-list]
   [project]
   [expenses]
   [dialogs/add-project-dialog]
   [dialogs/edit-project-dialog]
   [dialogs/add-expense-dialog]
   [dialogs/add-time-dialog]
   [dialogs/sign-in-dialog]])

(defn start []
  (rdom/render [main]
               (. js/document (getElementById "app"))))

(defn ^:export init []
  (js/setInterval #(rf/dispatch [::inc-time]) 1000)
  (rf/dispatch [::init-dark-mode])
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  )
