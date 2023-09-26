(ns craft-piggy-bank.dialogs
  (:require
    [re-frame.core :as rf]
    [craft-piggy-bank.user :as user]))

;; ------------------------ New Project Dialog

(rf/reg-sub
  ::new-project
  (fn [db [_ val]]
    (if val
      (get-in db [::new-project val])
      (get db ::new-project))))

(rf/reg-event-db
  :dialogs/open-add-project-dialog
  (fn [db _]
    (assoc db ::new-project {:id (str (random-uuid))})))

(rf/reg-event-db
  ::close-add-project-dialog
  (fn [db _]
    (dissoc db ::new-project)))

(rf/reg-event-db
  ::set-new-project
  (fn [db [_ key val]]
    (assoc-in db [::new-project key] val)))

(defn add-project-dialog []
  (let [new-project @(rf/subscribe [::new-project])]
    (when (some? new-project)
      [:div.bg-gray-800.dark:bg-gray-950.bg-opacity-50.dark:bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
       {:style {:width "100vw" :height "100vh"}
        :on-click #(rf/dispatch [::close-add-project-dialog])}
       [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5.dark:bg-stone-950.dark:border-stone-800
        {:style {:width "30rem"}
         :on-click (fn [e] (.stopPropagation e))}
        [:div.text-center.mb-5 "Add a project"]
        [:input.focus:outline-none.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
         {:type "text"
          :placeholder "Project Name"
          :value (:t_name new-project)
          :on-change #(rf/dispatch [::set-new-project :t_name (-> % .-target .-value)])}]

        [:input.focus:outline-none.remove-arrow.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
         {:type "number"
          :placeholder "Hourly Rate"
          :default 0.00
          :value (:f_rate new-project)
          :on-change #(rf/dispatch [::set-new-project :f_rate (-> % .-target .-value js/parseFloat)])}]
        [:div.mb-2 "Time already clocked in:"]
        [:div.flex.justify-center
         [:input.focus:outline-none.remove-arrow.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
          {:type "number"
           :placeholder "Hours"
           :value (:hours new-project)
           :on-change #(rf/dispatch [::set-new-project :hours (-> % .-target .-value js/parseInt)])}]
         [:div ":"]
         [:input.focus:outline-none.remove-arrow.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
          {:type "number"
           :placeholder "Minutes"
           :value (:minutes new-project)
           :on-change #(rf/dispatch [::set-new-project :minutes (-> % .-target .-value js/parseInt)])}]
         [:div ":"]
         [:input.focus:outline-none.remove-arrow.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
          {:type "number"
           :placeholder "Seconds"
           :value (:seconds new-project)
           :on-change #(rf/dispatch [::set-new-project :seconds (-> % .-target .-value js/parseInt)])}]]
        [:div.w-full.flex.justify-between
         [:div "Add expenses?"]
         [:input.focus:outline-none.w-10.h-10.border.mx-5.rounded-lg.mb-5
          {:type "button"
           :class (if (:b_add_expenses new-project)
                    "bg-blue-300 border-blue-500 dark:bg-blue-950 dark:border-blue-800"
                    "hover:bg-blue-100 hover:border-blue-300 bg-gray-100 border-gray-300 dark:bg-stone-900 dark:border-stone-800")
           :on-click #(rf/dispatch [::set-new-project :b_add_expenses (not (:b_add_expenses new-project))])}]]
        [:div.w-full.flex
         [:div.p-3.cursor-pointer.hover:bg-gray-100.rounded-lg.dark:hover:bg-stone-800
          {:on-click #(rf/dispatch [::close-add-project-dialog])}
          "Cancel"]
         [:div.flex-grow]
         [:div.p-3.cursor-pointer.text-center.rounded-lg.hover:bg-green-500.dark:hover:bg-teal-900
          {:on-click (fn []
                       (rf/dispatch [:user/add-db-project new-project])
                       (rf/dispatch [::close-add-project-dialog]))}
          "Add"]]]])))

;; ------------------------ Edit Project Dialog
(rf/reg-sub
  ::project-edits
  (fn [db [_]]
    (get db ::project-edits)))

(rf/reg-event-db
  :dialogs/open-edit-project-dialog
  (fn [db [_ id]]
    (let [project (get-in db [:projects id])
          time (:i_time project)
          seconds (mod time 60)
          minutes (mod (Math/floor (/ time 60)) 60)
          hours (Math/floor (/ time 3600))]
      (assoc db ::project-edits (merge project {:hours hours :minutes minutes :seconds seconds})))))

(rf/reg-event-db
  ::close-edit-project-dialog
  (fn [db _]
    (dissoc db ::project-edits)))

(rf/reg-event-db
  ::set-project-edits
  (fn [db [_ key val]]
    (assoc-in db [::project-edits key] val)))

(defn edit-project-dialog []
  (let [show? @(rf/subscribe [::project-edits])]
    (when show?
      (let [project-edits @(rf/subscribe [::project-edits])]
        [:div.bg-gray-800.bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl.dark:bg-gray-950.dark:bg-opacity-50
         {:style {:width "100vw" :height "100vh"}
          :on-click #(rf/dispatch [::close-edit-project-dialog])}
         [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5.dark:bg-stone-950.dark:border-stone-800
          {:style {:width "30rem"}
           :on-click (fn [e] (.stopPropagation e))}
          [:div.text-center.mb-5 (str "Editing " (:t_name project-edits))]
          [:input.focus:outline-none.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
           {:type "text"
            :placeholder "Project Name"
            :value (or (:t_name project-edits) "Project")
            :on-change #(rf/dispatch [::set-project-edits :t_name (-> % .-target .-value)])
            :on-focus #(-> % .-target .select)}]
          [:input.focus:outline-none.remove-arrow.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
           {:type "number"
            :placeholder "Hourly Rate"
            :value (:f_rate project-edits)
            :on-change #(rf/dispatch [::set-project-edits :f_rate (-> % .-target .-value js/parseFloat)])}]
          [:div "Time already clocked in:"]
          [:div.flex.justify-center
           [:input.focus:outline-none.remove-arrow.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
            {:type "number"
             :placeholder "Hours"
             :value (:hours project-edits)
             :on-change #(rf/dispatch [::set-project-edits :hours (-> % .-target .-value js/parseInt)])}]
           [:div ":"]
           [:input.focus:outline-none.remove-arrow.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
            {:type "number"
             :placeholder "Minutes"
             :value (:minutes project-edits)
             :on-change #(rf/dispatch [::set-project-edits :minutes (-> % .-target .-value js/parseInt)])}]
           [:div ":"]
           [:input.focus:outline-none.remove-arrow.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
            {:type "number"
             :placeholder "Seconds"
             :value (:seconds project-edits)
             :on-change #(rf/dispatch [::set-project-edits :seconds (-> % .-target .-value js/parseInt)])}]]
          [:div.w-full.flex.justify-between
           [:div "Add expenses?"]
           [:input.focus:outline-none.w-10.h-10.border.mx-5.rounded-lg.mb-5.cursor-pointer
            {:type "button"
             :class (if (:b_add_expenses project-edits)
                      "bg-blue-300 border-blue-500 dark:bg-blue-950 dark:border-blue-800"
                      "hover:bg-blue-100 hover:border-blue-300 bg-gray-100 border-gray-300 dark:bg-stone-900 dark:border-stone-800")
             :on-click #(rf/dispatch [::set-project-edits :b_add_expenses (not (:b_add_expenses project-edits))])}]]
          [:div.w-full.flex
           [:div.p-3.cursor-pointer.hover:bg-gray-100.rounded-lg.dark:hover:bg-stone-800
            {:on-click #(rf/dispatch [::close-edit-project-dialog])}
            "Cancel"]
           [:div.flex-grow]
           [:div.p-3.cursor-pointer.text-center.rounded-lg.hover:bg-green-500.dark:hover:bg-teal-900
            {:on-click (fn []
                         (rf/dispatch [:user/update-db-project project-edits])
                         (rf/dispatch [::close-edit-project-dialog]))}
            "Confirm"]]]]))))

;; ------------------------ New Expense Dialog
(rf/reg-sub
  ::new-expense
  (fn [db [_]]
    (get db ::new-expense)))

(rf/reg-event-db
  :dialogs/new-expense
  (fn [db [_ val]]
    (when (some? (get db :current-project))
      (assoc db ::new-expense val))))

(rf/reg-event-db
  ::set-expense-edits
  (fn [db [_ key val]]
    (assoc-in db [::new-expense key] val)))

(rf/reg-event-db
  ::close-expense-dialog
  (fn [db _]
    (dissoc db ::new-expense)))

(defn add-expense-dialog []
  (let [new-expense @(rf/subscribe [::new-expense])]
    (when (some? new-expense)
      [:div.bg-gray-800.bg-opacity-50.dark:bg-gray-950.dark:bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
       {:style {:width "100vw" :height "100vh"}
        :on-click #(rf/dispatch [::close-expense-dialog])}
       [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5.dark:bg-stone-950.dark:border-stone-800
        {:style {:width "30rem"}
         :on-click (fn [e] (.stopPropagation e))}
        [:div.text-center.mb-5 "Add an expense"]
        [:input.focus:outline-none.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
         {:type "text"
          :placeholder "Expense Name"
          :value (:t_name new-expense)
          :on-change #(rf/dispatch [::set-expense-edits :t_name (-> % .-target .-value)])}]
        [:input.remove-arrow.focus:outline-none.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
         {:type "number"
          :placeholder "Cost"
          :value (:f_cost new-expense)
          :on-change #(rf/dispatch [::set-expense-edits :f_cost (-> % .-target .-value js/parseFloat)])}]
        [:div.w-full.flex
         [:div.p-3.cursor-pointer.hover:bg-gray-100.rounded-lg.dark:hover:bg-stone-800
          {:on-click #(rf/dispatch [::close-expense-dialog])}
          "Cancel"]
         [:div.flex-grow]
         [:div.p-3.cursor-pointer.text-center.rounded-lg.hover:bg-green-500.dark:hover:bg-teal-900
          {:on-click (fn []
                       (rf/dispatch [:user/add-db-expense new-expense])
                       (rf/dispatch [::close-expense-dialog]))}
          "Add"]]]])))

;; ------------------------ Add Time Dialog
(rf/reg-sub
  ::add-time
  (fn [db _]
    (get db ::add-time)))

(rf/reg-event-db
  ::set-add-time
  (fn [db [_ k val]]
    (assoc-in db [::add-time k] val)))

(rf/reg-event-db
  :dialogs/open-add-time-dialog
  (fn [db _]
    (assoc db ::add-time {})))

(rf/reg-event-db
  ::close-add-time-dialog
  (fn [db _]
    (dissoc db ::add-time)))

(defn add-time-dialog []
  (let [add-time @(rf/subscribe [::add-time])
        current-project @(rf/subscribe [:current-project-data])]
    (when (some? add-time)
      [:div.bg-gray-800.bg-opacity-50.dark:bg-gray-950.dark:bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
       {:style {:width "100vw" :height "100vh"}
        :on-click #(rf/dispatch [::close-add-time-dialog])}
       [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5.dark:bg-stone-950.dark:border-stone-800
        {:style {:width "30rem"}
         :on-click (fn [e] (.stopPropagation e))}
        [:div.text-center.mb-5 (str "Add time to " (:t_name current-project))]
        [:div.flex.justify-center
         [:input.focus:outline-none.remove-arrow.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
          {:type "number"
           :placeholder "Hours"
           :value (:hours add-time)
           :on-change #(rf/dispatch [::set-add-time :hours (-> % .-target .-value js/parseInt)])}]
         [:div ":"]
         [:input.focus:outline-none.remove-arrow.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
          {:type "number"
           :placeholder "Minutes"
           :value (:minutes add-time)
           :on-change #(rf/dispatch [::set-add-time :minutes (-> % .-target .-value js/parseInt)])}]
         [:div ":"]
         [:input.focus:outline-none.remove-arrow.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
          {:type "number"
           :placeholder "Seconds"
           :value (:seconds add-time)
           :on-change #(rf/dispatch [::set-add-time :seconds (-> % .-target .-value js/parseInt)])}]]
        [:div.w-full.flex
         [:div.p-3.cursor-pointer.hover:bg-gray-100.rounded-lg.dark:hover:bg-stone-800
          {:on-click #(rf/dispatch [::close-add-time-dialog])}
          "Cancel"]
         [:div.flex-grow]
         [:div.p-3.cursor-pointer.text-center.rounded-lg.hover:bg-green-500.dark:hover:bg-teal-900
          {:on-click (fn []
                       (rf/dispatch [:user/add-db-time add-time (:id current-project)])
                       (rf/dispatch [::close-add-time-dialog]))}
          "Add"]]]])))

;; ------------------------ Sign In Dialoge
(rf/reg-sub
  ::sign-in-creds
  (fn [db _]
    (get db ::sign-in-creds)))

(rf/reg-event-db
  ::set-sign-in-creds
  (fn [db [_ k val]]
    (assoc-in db [::sign-in-creds k] val)))

(rf/reg-event-db
  :dialogs/open-sign-in-dialog
  (fn [db _]
    (assoc db ::sign-in-creds {})))

(rf/reg-event-db
  ::close-sign-in-dialog
  (fn [db _]
    (dissoc db ::sign-in-creds)))

(defn sign-in-dialog []
  (let [sign-in-creds @(rf/subscribe [::sign-in-creds])]
    (when (some? sign-in-creds)
      [:div.bg-gray-800.bg-opacity-50.dark:bg-gray-950.dark:bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
       {:style {:width "100vw" :height "100vh"}
        :on-click #(rf/dispatch [::close-sign-in-dialog])}
       [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5.dark:bg-stone-950.dark:border-stone-800
        {:style {:width "30rem"}
         :on-click (fn [e] (.stopPropagation e))}
        [:div.text-center.mb-5 "Sign In"]
        (when (not-empty @(rf/subscribe [:projects]))
          [:div.flex.w-full.text-base.text-orange-700.justify-center.text-center.mb-5
           [:div.flex.items-center.mr-2 [:i.fas.fa-circle-info]]
           [:div "Local projects will not be migrated when signing in."]])
        [:input.focus:outline-none.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
         {:type "email"
          :placeholder "Email"
          :value (:email sign-in-creds)
          :on-change #(rf/dispatch [::set-sign-in-creds :email (-> % .-target .-value)])}]
        [:input.focus:outline-none.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5.dark:bg-stone-900.dark:border-stone-800.dark:focus:border-stone-600
         {:type "password"
          :placeholder "Password"
          :value (:password sign-in-creds)
          :on-change #(rf/dispatch [::set-sign-in-creds :password (-> % .-target .-value)])}]
        [:div.w-full.flex
         [:div.p-3.cursor-pointer.text-center.rounded-lg.hover:bg-green-500.dark:hover:bg-teal-900
          {:on-click (fn []
                       (user/sign-up-submitted (:email sign-in-creds) (:password sign-in-creds))
                       (rf/dispatch [::close-sign-in-dialog]))}
          "Sign Up"]
         [:div.flex-grow]
         [:div.p-3.cursor-pointer.text-center.rounded-lg.hover:bg-green-500.dark:hover:bg-teal-900
          {:on-click (fn []
                       (user/sign-in-submitted (:email sign-in-creds) (:password sign-in-creds))
                       (rf/dispatch [::close-sign-in-dialog]))}
          "Sign In"]]]])))

;; ------------------------ Error Dialog

(rf/reg-event-db
  ::close-error-dialog
  (fn [db _]
    (dissoc db :error)))

(rf/reg-sub
  ::get-error
  (fn [db _]
    (get db :error)))

(rf/reg-sub
  ::get-success
  (fn [db _]
    (get db :success)))

(defn error-dialog []
  (let [error @(rf/subscribe [::get-error])
        success @(rf/subscribe [::get-success])]
    [:div.absolute.bottom-0.left-0.w-full.text-white.text-lg
     [:div.flex.flex-center
      {:style {:opacity (if error "100" "0")
               :transition "all 0.3s ease-in"}}
      (when error [:div.border.border-red-900.bg-red-700.mb-3.p-5.rounded-lg (str "Error: " error)])]
     [:div.flex.flex-center
      {:style {:opacity (if success "100" "0")
               :transition "all 0.3s ease-in"}}
      (when success [:div.border.border-green-900.bg-green-700.mb-3.p-5.rounded-lg success])]]))