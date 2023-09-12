(ns craft-piggy-bank.dialogs
  (:require
    [reagent.dom :as rdom]
    [re-frame.core :as rf]
    [clojure.string :as str]))

;; ------------------------ New Project Dialog
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

(rf/reg-event-db
  :dialogs/open-add-project-dialog
  (fn [db _]
    (assoc db ::new-project {})))

(rf/reg-event-db
  ::close-add-project-dialog
  (fn [db _]
    (dissoc db ::new-project)))

(rf/reg-event-db
  ::set-new-project
  (fn [db [_ key val]]
    (assoc-in db [::new-project key] val)))

(rf/reg-event-db
  ::add-project
  (fn [db [_ project]]
    (print "Project " project)
    (when (and (some? (:name project)) (some? (:rate project)))
      (-> db
          (assoc-in [:projects (:name project)] (assoc project :time (+
                                                                       (* 3600 (:hours project))
                                                                       (* 60 (:minutes project))
                                                                       (:seconds project))))
          (assoc-in [::add-project-dialog] false)))))

(defn add-project-dialog []
  (let [new-project @(rf/subscribe [::new-project])]
    (when (some? new-project)
      [:div.bg-gray-800.bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
       {:style {:width "100vw" :height "100vh"}
        :on-click #(rf/dispatch [::close-add-project-dialog])}
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
          :default 0.00
          :value (:rate new-project)
          :on-change #(rf/dispatch [::set-new-project :rate (-> % .-target .-value js/parseFloat)])}]
        [:div "Time already clocked in:"]
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
         [:div.p-2.cursor-pointer.hover:bg-gray-100.rounded-lg
          {:on-click #(rf/dispatch [::close-add-project-dialog])}
          "Cancel"]
         [:div.p-2.cursor-pointer.hover:bg-blue.text-center.rounded-lg.hover:bg-green-500
          {:on-click #(rf/dispatch [::add-project new-project])}
          "Add"]]]])))

;; ------------------------ Edit Project Dialog
(rf/reg-sub
  ::project-edits
  (fn [db [_]]
    (get db ::project-edits)))

(rf/reg-event-db
  :dialogs/open-edit-project-dialog
  (fn [db [_ name]]
    (assoc-in db [::project-edits]
              (assoc (get-in db [:projects name]) :old-name name))))

(rf/reg-event-db
  ::close-edit-project-dialog
  (fn [db _]
    (dissoc db ::project-edits)))

(rf/reg-event-db
  ::set-project-edits
  (fn [db [_ key val]]
    (assoc-in db [::project-edits key] val)))

(rf/reg-event-db
  ::edit-project
  (fn [db [_ project]]
    (print "Project " project)
    (when (and (some? (:name project)) (some? (:rate project)))
      (-> db
          (update :projects dissoc (:old-name project))
          (assoc-in [:projects (:name project)] (assoc project :time (+
                                                                       (* 3600 (:hours project))
                                                                       (* 60 (:minutes project))
                                                                       (:seconds project))))
          (dissoc ::project-edits)))))

(defn edit-project-dialog []
  (let [show? @(rf/subscribe [::project-edits])]
    (when show?
      (let [project-edits @(rf/subscribe [::project-edits])]
        [:div.bg-gray-800.bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
         {:style {:width "100vw" :height "100vh"}
          :on-click #(rf/dispatch [::close-edit-project-dialog])}
         [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5
          {:style {:width "30rem"}
           :on-click (fn [e] (.stopPropagation e))}
          [:div.text-center.mb-5 (str "Editing " (:old-name project-edits))]
          [:input.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
           {:type "text"
            :placeholder "Project Name"
            :value (or (:name project-edits) "Project")
            :on-change #(rf/dispatch [::set-project-edits :name (-> % .-target .-value)])
            :on-focus #(-> % .-target .select)}]
          [:input.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
           {:type "number"
            :placeholder "Hourly Rate"
            :value (:rate project-edits)
            :on-change #(rf/dispatch [::set-project-edits :rate (-> % .-target .-value js/parseFloat)])}]
          [:div "Time already clocked in:"]
          [:div.flex.justify-center
           [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
            {:type "number"
             :placeholder "Hours"
             :value (:hours project-edits)
             :on-change #(rf/dispatch [::set-project-edits :hours (-> % .-target .-value js/parseInt)])}]
           [:div ":"]
           [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
            {:type "number"
             :placeholder "Minutes"
             :value (:minutes project-edits)
             :on-change #(rf/dispatch [::set-project-edits :minutes (-> % .-target .-value js/parseInt)])}]
           [:div ":"]
           [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
            {:type "number"
             :placeholder "Seconds"
             :value (:seconds project-edits)
             :on-change #(rf/dispatch [::set-project-edit :seconds (-> % .-target .-value js/parseInt)])}]]
          [:div.w-full.flex
           [:div.flex-grow]
           [:div.p-2.cursor-pointer.hover:bg-gray-100.rounded-lg
            {:on-click #(rf/dispatch [::close-edit-project-dialog])} "Cancel"]
           [:div.p-2.cursor-pointer.hover:bg-blue.text-center.rounded-lg.hover:bg-green-500
            {:on-click #(rf/dispatch [::edit-project project-edits])} "Confirm"]]]]))))

;; ------------------------ New Project Dialog
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

(rf/reg-event-db
  ::add-expense
  (fn [db [_ expense]]
    (print "expense " expense)
    (-> db
        (update-in [:projects (get db :current-project) :expenses]
                  conj expense)
        (dissoc ::new-expense))))

(defn add-expense-dialog []
  (let [new-expense @(rf/subscribe [::new-expense])]
    (when (some? new-expense)
      [:div.bg-gray-800.bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
       {:style {:width "100vw" :height "100vh"}
        :on-click #(rf/dispatch [::close-expense-dialog])}
       [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5
        {:style {:width "30rem"}
         :on-click (fn [e] (.stopPropagation e))}
        [:div.text-center.mb-5 "Add an expense"]
        [:input.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
         {:type "text"
          :placeholder "Expense Name"
          :value (:name new-expense)
          :on-change #(rf/dispatch [::set-expense-edits :name (-> % .-target .-value)])}]
        [:input.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
         {:type "number"
          :placeholder "Cost"
          :value (:cost new-expense)
          :on-change #(rf/dispatch [::set-expense-edits :cost (-> % .-target .-value js/parseFloat)])}]
        [:div.w-full.flex
         [:div.flex-grow]
         [:div.p-2.cursor-pointer.hover:bg-gray-100.rounded-lg
          {:on-click #(rf/dispatch [::close-expense-dialog])}
          "Cancel"]
         [:div.p-2.cursor-pointer.hover:bg-blue.text-center.rounded-lg.hover:bg-green-500
          {:on-click #(rf/dispatch [::add-expense new-expense])}
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

(rf/reg-event-db
  ::add-time
  (fn [db _]
    (let [time (get db ::add-time)
          time-combined (+
                          (* 3600 (:hours time))
                          (* 60 (:minutes time))
                          (:seconds time))
          current-time (get-in db [:projects (get db :current-project) :time])]
      (-> db
          (assoc-in [:projects (get db :current-project) :time] (+ current-time time-combined))
          (dissoc db ::add-time)))))



(defn add-time-dialog []
  (let [add-time @(rf/subscribe [::add-time])
        current-project @(rf/subscribe [:current-project])]
    (when (some? add-time)
      [:div.bg-gray-800.bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
       {:style {:width "100vw" :height "100vh"}
        :on-click #(rf/dispatch [::close-add-time-dialog])}
       [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5
        {:style {:width "30rem"}
         :on-click (fn [e] (.stopPropagation e))}
        [:div.text-center.mb-5 (str "Add time to " current-project)]
        [:div.flex.justify-center
         [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
          {:type "number"
           :placeholder "Hours"
           :value (:hours add-time)
           :on-change #(rf/dispatch [::set-add-time :hours (-> % .-target .-value js/parseInt)])}]
         [:div ":"]
         [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
          {:type "number"
           :placeholder "Minutes"
           :value (:minutes add-time)
           :on-change #(rf/dispatch [::set-add-time :minutes (-> % .-target .-value js/parseInt)])}]
         [:div ":"]
         [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
          {:type "number"
           :placeholder "Seconds"
           :value (:seconds add-time)
           :on-change #(rf/dispatch [::set-add-time :seconds (-> % .-target .-value js/parseInt)])}]]
        [:div.w-full.flex
         [:div.flex-grow]
         [:div.p-2.cursor-pointer.hover:bg-gray-100.rounded-lg
          {:on-click #(rf/dispatch [::close-add-time-dialog])}
          "Cancel"]
         [:div.p-2.cursor-pointer.hover:bg-blue.text-center.rounded-lg.hover:bg-green-500
          {:on-click #(rf/dispatch [::add-time])}
          "Add"]]]])))