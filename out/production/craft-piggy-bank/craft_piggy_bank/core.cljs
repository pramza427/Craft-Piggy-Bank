(ns craft-piggy-bank.core
  (:require
    [reagent.dom :as rdom]
    [reagent.core :as reagent :refer [atom]]
    [re-frame.core :as rf]
    [clojure.string :as str]
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

(rf/reg-event-db
  ::save-to-local
  (fn [db _]
      (set-local-storage "projects"
                         (mapv (fn [e]
                                   (select-keys e [:name :time :rate :expenses]))
                               (vals (get db ::projects))))))

(rf/reg-event-db
  ::load-from-local
  (fn [db _]
      (print (get-local-storage "projects"))
      (let [projects (js/JSON.parse (get-local-storage "projects"))]
           (when (some? projects)
                 (assoc db ::projects (into {} (map (fn [project] [(:name project) project]) projects)))))))

;; ---------- Subscriptions ----------
(rf/reg-sub
  ::dark-mode?
  (fn [db _]
      (get db ::dark-mode)))

(rf/reg-sub
  ::current-project
  (fn [db _]
      (get db ::current-project)))

(rf/reg-sub
  ::projects
  (fn [db [_]]
      (get db ::projects)))

(rf/reg-sub
  ::add-project-dialog
  (fn [db [_]]
      (get db ::add-project-dialog)))

(rf/reg-sub
  ::new-project
  (fn [db [_ val]]
      (if val
        (get-in db [::new-project val])
        (get db ::new-project))))


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
  ::set-time
  (fn [db [_ val]]
      (assoc db ::current-time val)))

(rf/reg-event-db
  ::inc-time
  (fn [db _]
      (let [current-project (get db ::current-project)]
           (assoc-in db [::projects current-project :time] (inc (get-in db [::projects current-project :time]))))))

(rf/reg-event-db
  ::set-counting
  (fn [db [_ val]]
      (assoc db ::counting? val)))

(rf/reg-event-db
  ::set-current-project
  (fn [db [_ val]]
      (assoc db ::current-project val)))

(rf/reg-event-db
  ::set-new-project
  (fn [db [_ key val]]
      (assoc-in db [::new-project key] val)))

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

(rf/reg-event-db
  ::add-project-dialog
  (fn [db [_ val]]
      (-> db
          (assoc ::add-project-dialog val)
          (dissoc ::new-project))))

(rf/reg-event-db
  ::add-project
  (fn [db [_ project]]
      (print "Project " project)
      (when (and (some? (:name project)) (some? (:rate project)))
            (-> db
                (assoc-in [::projects (:name project)] (assoc project :time (+
                                                                              (* 3600 (:hours project))
                                                                              (* 60 (:minutes project))
                                                                              (:seconds project))))
                (assoc-in [::add-project-dialog] false)))))

(defn save-load []
      (let [user @(rf/subscribe [::current-user])]
           [:div.border-t.p-2
            (if (some? user)
              [:div (str "Hello " user)]
              [:div.p-1.cursor-pointer.hover:bg-blue-50.text-center "Sign In"])
            [:div.p-1.cursor-pointer.hover:bg-blue-50.text-center
             {:on-click #(rf/dispatch [::save-to-local])}
             "Save to local"]
            [:div.p-1.cursor-pointer.hover:bg-blue-50.text-center
             {:on-click #(rf/dispatch [::load-from-local])}
             "Load from local"]]))

(defn project-list-item [project]
      (let [current-project @(rf/subscribe [::current-project])]
           [:div.py-2.border-b.cursor-pointer.rounded
            {:class (if (= current-project (:name project)) "bg-blue-100" "hover:bg-blue-50")
             :on-click #(rf/dispatch [::set-current-project (:name project)])}
            [:div.flex
             [:div.px-2 (:name project)]
             [:div.flex-grow]
             [:div.px-2 (format-time (:time project))]]
            [:div.flex
             [:div.px-2 "E"]
             [:div.px-2 "D"]
             [:div.flex-grow]
             [:div.px-2 (str "Rate: $" (:rate project))]]]))

(defn project-list []
      (let [projects @(rf/subscribe [::projects])]
           [:div.w-80.border-r.text-xl.flex.flex-col
            [:div.cursor-pointer.bg-teal-400.hover:bg-teal-500.text-center.p-1.mb-1.rounded.border-b.border-gray-300
             {:on-click #(rf/dispatch [::add-project-dialog true])}
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
      (let [current-project @(rf/subscribe [::current-project])
            projects @(rf/subscribe [::projects])
            time (get-in projects [current-project :time])
            rate (get-in projects [current-project :rate])
            counting @(rf/subscribe [::counting?])]
           (cond
             (some? current-project)
             [:div.text-6xl.text-center
              [:div.mt-10 current-project]
              [:div.my-5 (format-time time)]
              [:div.border.border-gray-300.p-2.cursor-pointer.hover:bg-blue-50
               {:on-click #(rf/dispatch [::set-counting (not counting)])}
               (if counting "Stop" "Start")]
              [:div.mt-10 "Piggy Bank:"]
              [:div.my-5 (str "$" (.toFixed (* rate (/ time 3600)) 2))]]

             :else
             [:div.text-6xl
              [:div "Select Project"]
              [:div "<---"]]
             )))

(defn add-project-dialog []
      (let [new-project @(rf/subscribe [::new-project])]
           [:div.bg-gray-800.bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
            {:style {:width "100vw" :height "100vh"}
             :on-click #(rf/dispatch [::add-project-dialog false])}
            [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5
             {:style {:width "30rem"}
              :on-click (fn [e] (.stopPropagation e))}
             [:div.text-center.mb-5 "Add a project"]
             [:input.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
              {:type "text"
               :placeholder "Project Name"
               :value (:name new-project)
               :on-change #(rf/dispatch [::set-new-project :name (-> % .-target .-value)])}]
             [:input.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
              {:type "number"
               :placeholder "Hourly Rate"
               :value (:rate new-project)
               :on-change #(rf/dispatch [::set-new-project :rate (-> % .-target .-value js/parseInt)])}]
             [:div.flex.justify-center
              [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
               {:type "number"
                :placeholder "Hours"
                :value (:hours new-project)
                :on-change #(rf/dispatch [::set-new-project :hours (-> % .-target .-value js/parseInt)])}]
              [:div ":"]
              [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
               {:type "number"
                :placeholder "Minutes"
                :value (:minutes new-project)
                :on-change #(rf/dispatch [::set-new-project :minutes (-> % .-target .-value js/parseInt)])}]
              [:div ":"]
              [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
               {:type "number"
                :placeholder "Seconds"
                :value (:seconds new-project)
                :on-change #(rf/dispatch [::set-new-project :seconds (-> % .-target .-value js/parseInt)])}]]
             [:div.w-full.flex
              [:div.flex-grow]
              [:div.p-2.cursor-pointer.hover:bg-gray-100.rounded-lg "Cancel"]
              [:div.p-2.cursor-pointer.hover:bg-blue.text-center.rounded-lg.hover:bg-green-500
               {:on-click #(rf/dispatch [::add-project new-project])} "Add"]]]]))

(defn expenses []
      [:div.w-80.border-l.text-xl.flex.flex-col
       [:div.cursor-pointer.bg-red-300.hover:bg-red-400.text-center.p-1.mb-1.rounded.border-b.border-gray-300
        {:on-click #(rf/dispatch [::add-expense-dialog true])}
        "Add Expense"]])
(defn main []
      (let [add-project? @(rf/subscribe [::add-project-dialog])]
           [:div.flex.justify-between.min-h-full.relative.font-serif
            {:style {:height "100vh"}}
            [project-list]
            [project]
            [expenses]
            (when add-project?
                  [add-project-dialog])
            ]))

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
