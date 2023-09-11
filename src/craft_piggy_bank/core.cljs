(ns craft-piggy-bank.core
  (:require
    [reagent.dom :as rdom]
    [reagent.core :as reagent :refer [atom]]
    [re-frame.core :as rf]
    [clojure.string :as str]
    [clojure.walk :refer [keywordize-keys]]
    [craft-piggy-bank.dialogs :as dialogs]
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
                                 (select-keys e [:name :time :rate :expenses])))
                         (clj->js)
                         (js/JSON.stringify)))))

(rf/reg-event-db
  ::load-from-local
  (fn [db _]

    (let [projects (keywordize-keys (js->clj (js/JSON.parse (get-local-storage "projects"))))]
      (when (some? projects)
        (assoc db :projects (into {}
                                  (map (fn [project] [(:name project) project])
                                       projects)))))))

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

(rf/reg-sub
  ::current-user
  (fn [db _]
    (get db ::current-user)))

;; ---------- Events ----------
(rf/reg-event-db
  ::delete-project
  (fn [db [_ name]]
    (update-in db [:projects] dissoc name)))

(rf/reg-event-db
  ::set-time
  (fn [db [_ val]]
    (assoc db ::current-time val)))

(rf/reg-event-db
  ::inc-time
  (fn [db _]
    (let [current-project (get db :current-project)]
      (assoc-in db [:projects current-project :time] (inc (get-in db [:projects current-project :time]))))))

(rf/reg-event-db
  ::set-counting
  (fn [db [_ val]]
    (assoc db ::counting? val)))

(rf/reg-event-db
  :set-current-project
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
  (let [user @(rf/subscribe [::current-user])]
    [:div.border-t.p-2
     (if (some? user)
       [:div (str "Hello " user)]
       [:div.p-1.cursor-pointer.hover:bg-blue-50.text-center
        {:on-click #(js/alert "NOT IMPLEMENTED. I'M SORRY!")} "Sign In"])
     [:div.p-1.cursor-pointer.hover:bg-blue-50.text-center
      {:on-click #(rf/dispatch [::save-to-local])}
      "Save to local"]
     [:div.p-1.cursor-pointer.hover:bg-blue-50.text-center
      {:on-click #(rf/dispatch [::load-from-local])}
      "Load from local"]]))

(defn project-list-item [project]
  (let [current-project @(rf/subscribe [:current-project])]
    [:div.py-2.border-b.cursor-pointer.rounded
     {:class (if (= current-project (:name project)) "bg-blue-100" "hover:bg-blue-50")
      :on-click #(rf/dispatch [:set-current-project (:name project)])}
     [:div.flex
      [:div.px-2 (:name project)]
      [:div.flex-grow]
      [:div.px-2 (format-time (:time project))]]
     [:div.flex
      [:div.px-2.hover:bg-blue-200
       {:on-click #(rf/dispatch [:dialog/open-edit-project-dialog (:name project)])}
       [:i.fas.fa-edit]]
      [:div.px-2.hover:bg-red-100
       {:on-click #(when (js/confirm (str "Are you sure you want to delete " (:name project) "?"))
                     (rf/dispatch [::delete-project (:name project)]))}
       [:i.fas.fa-trash]]
      [:div.flex-grow]
      [:div.px-2 (str (format-money (:rate project)) "/hr")]]]))

(defn project-list []
  (let [projects @(rf/subscribe [:projects])]
    [:div.w-80.border-r.text-xl.flex.flex-col
     [:div.cursor-pointer.bg-teal-400.hover:bg-teal-500.text-center.p-1.mb-1.rounded.border-b.border-gray-300
      {:on-click #(rf/dispatch [:dialog/add-project-dialog true])}
      "Add Project"]
     (print "List: " projects)
     [:div.overflow-auto.p-2
      (if (some? projects)
        (for [[key project] projects]
          ^{:key key}
          [project-list-item project])
        [:div "No projects to show"])]
     [:div.flex-grow]
     [save-load]]))

(defn project []
  (let [current-project @(rf/subscribe [:current-project])
        projects @(rf/subscribe [:projects])
        time (get-in projects [current-project :time])
        rate (get-in projects [current-project :rate])
        counting @(rf/subscribe [::counting?])
        current-expenses @(rf/subscribe [::current-expenses])
        expense-total (reduce + (map :cost current-expenses))]
    (cond
      (some? current-project)
      [:div.text-6xl.text-center
       [:div.mt-10 current-project]
       [:div.my-5 (format-time time)]
       [:div.border.border-gray-300.p-2.cursor-pointer.hover:bg-blue-50
        {:on-click #(rf/dispatch [::set-counting (not counting)])}
        (if counting "Stop" "Start")]
       [:div.mt-10 "Piggy Bank:"]
       [:div.my-5 (format-money (- (* rate (/ time 3600)) expense-total))]]

      :else
      [:div.text-6xl
       [:div "Select Project"]
       [:div "<---"]]
      )))

(defn expenses []
  (let [current-expenses @(rf/subscribe [::current-expenses])
        current-project @(rf/subscribe [:current-project])]
    [:div.w-80.border-l.text-xl.flex.flex-col
     [:div.bg-red-300.text-center.p-1.mb-1.rounded.border-b.border-gray-300
      {:class (if (some? current-project)
                "cursor-pointer hover:bg-red-400"
                "cursor-none")
       :on-click #(rf/dispatch [:dialogs/new-expense {}])}
      "Add Expense"]
     [:div.overflow-auto.p-2
      (cond
        (nil? current-project)
        [:div "Expenses tied to projects. Select a project to see its expenses here."]

        (some? current-expenses)
        (for [expenses current-expenses]
          ^{:key (:name expenses)}
          [:div.flex
           [:div (:name expenses) ]
           [:div.flex-grow]
           [:div (format-money (:cost expenses))]])

        :else
        [:div "No expenses yet"])]]))
(defn main []
  [:div.flex.justify-between.min-h-full.relative.font-serif
   {:style {:height "100vh"}}
   [project-list]
   [project]
   [expenses]
   [dialogs/add-project-dialog]
   [dialogs/edit-project-dialog]
   [dialogs/add-expense-dialog]
   ])

(defn start []
  (rdom/render [main]
               (. js/document (getElementById "app"))))

(defn ^:export init []
  (js/setInterval #(when @(rf/subscribe [::counting?])
                     (rf/dispatch [::inc-time])) 1000)
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))
