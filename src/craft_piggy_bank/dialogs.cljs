(ns craft-piggy-bank.dialogs
  (:require
    [reagent.dom :as rdom]
    [re-frame.core :as rf]
    [clojure.string :as str]))

;; ------------------------ New Project Dialog
(rf/reg-sub
  :dialog/add-project-dialog
  (fn [db [_]]
    (get db :dialog/add-project-dialog)))

(rf/reg-sub
  :dialog/new-project
  (fn [db [_ val]]
    (if val
      (get-in db [:dialog/new-project val])
      (get db :dialog/new-project))))

(rf/reg-event-db
  :dialog/add-project-dialog
  (fn [db [_ val]]
    (-> db
        (assoc :dialog/add-project-dialog val)
        (dissoc :dialog/new-project))))

(rf/reg-event-db
  :dialog/set-new-project
  (fn [db [_ key val]]
    (assoc-in db [:dialog/new-project key] val)))

(rf/reg-event-db
  :dialog/add-project
  (fn [db [_ project]]
    (print "Project " project)
    (when (and (some? (:name project)) (some? (:rate project)))
      (-> db
          (assoc-in [:projects (:name project)] (assoc project :time (+
                                                                       (* 3600 (:hours project))
                                                                       (* 60 (:minutes project))
                                                                       (:seconds project))))
          (assoc-in [:dialog/add-project-dialog] false)))))

(defn add-project-dialog []
  (let [show? @(rf/subscribe [:dialog/add-project-dialog])
        new-project @(rf/subscribe [:dialog/new-project])]
    (when show?
      [:div.bg-gray-800.bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
       {:style {:width "100vw" :height "100vh"}
        :on-click #(rf/dispatch [:dialog/add-project-dialog false])}
       [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5
        {:style {:width "30rem"}
         :on-click (fn [e] (.stopPropagation e))}
        [:div.text-center.mb-5 "Add a project"]
        [:input.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
         {:type "text"
          :placeholder "Project Name"
          :value (:name new-project)
          :on-change #(rf/dispatch [:dialog/set-new-project :name (-> % .-target .-value)])}]
        [:input.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
         {:type "number"
          :placeholder "Hourly Rate"
          :value (:rate new-project)
          :on-change #(rf/dispatch [:dialog/set-new-project :rate (-> % .-target .-value js/parseInt)])}]
        [:div.flex.justify-center
         [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
          {:type "number"
           :placeholder "Hours"
           :value (:hours new-project)
           :on-change #(rf/dispatch [:dialog/set-new-project :hours (-> % .-target .-value js/parseInt)])}]
         [:div ":"]
         [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
          {:type "number"
           :placeholder "Minutes"
           :value (:minutes new-project)
           :on-change #(rf/dispatch [:dialog/set-new-project :minutes (-> % .-target .-value js/parseInt)])}]
         [:div ":"]
         [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
          {:type "number"
           :placeholder "Seconds"
           :value (:seconds new-project)
           :on-change #(rf/dispatch [:dialog/set-new-project :seconds (-> % .-target .-value js/parseInt)])}]]
        [:div.w-full.flex
         [:div.flex-grow]
         [:div.p-2.cursor-pointer.hover:bg-gray-100.rounded-lg "Cancel"]
         [:div.p-2.cursor-pointer.hover:bg-blue.text-center.rounded-lg.hover:bg-green-500
          {:on-click #(rf/dispatch [:dialog/add-project new-project])} "Add"]]]])))

;; ------------------------ Edit Project Dialog
(rf/reg-sub
  :dialog/project-edits
  (fn [db [_]]
    (get db :dialog/project-edits)))

(rf/reg-event-db
  :dialog/open-edit-project-dialog
  (fn [db [_ name]]
    (assoc-in db [:dialog/project-edits]
              (assoc (get-in db [:projects name]) :old-name name))))

(rf/reg-event-db
  :dialog/close-edit-project-dialog
  (fn [db _]
    (dissoc db :dialog/project-edits)))

(rf/reg-event-db
  :dialog/set-project-edits
  (fn [db [_ key val]]
    (assoc-in db [:dialog/project-edits key] val)))

(rf/reg-event-db
  :dialog/edit-project
  (fn [db [_ project]]
    (print "Project " project)
    (when (and (some? (:name project)) (some? (:rate project)))
      (-> db
          (update :projects dissoc (:old-name project))
          (assoc-in [:projects (:name project)] (assoc project :time (+
                                                                       (* 3600 (:hours project))
                                                                       (* 60 (:minutes project))
                                                                       (:seconds project))))
          (dissoc :dialog/project-edits)))))

(defn edit-project-dialog []
  (let [show? @(rf/subscribe [:dialog/project-edits])]
    (when show?
      (let [project-edits @(rf/subscribe [:dialog/project-edits])]
        [:div.bg-gray-800.bg-opacity-50.absolute.top-0.left-0.flex.flex-center.text-3xl
         {:style {:width "100vw" :height "100vh"}
          :on-click #(rf/dispatch [:dialog/close-edit-project-dialog])}
         [:div.bg-white.border.border-gray-300.rounded-lg.relative.p-5
          {:style {:width "30rem"}
           :on-click (fn [e] (.stopPropagation e))}
          [:div.text-center.mb-5 "edit a project"]
          [:input.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
           {:type "text"
            :placeholder "Project Name"
            :value (:name project-edits)
            :on-change #(rf/dispatch [:dialog/set-project-edits :name (-> % .-target .-value)])}]
          [:input.py-2.w-full.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
           {:type "number"
            :placeholder "Hourly Rate"
            :value (:rate project-edits)
            :on-change #(rf/dispatch [:dialog/set-project-edits :rate (-> % .-target .-value js/parseInt)])}]
          [:div.flex.justify-center
           [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
            {:type "number"
             :placeholder "Hours"
             :value (:hours project-edits)
             :on-change #(rf/dispatch [:dialog/set-project-edits :hours (-> % .-target .-value js/parseInt)])}]
           [:div ":"]
           [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
            {:type "number"
             :placeholder "Minutes"
             :value (:minutes project-edits)
             :on-change #(rf/dispatch [:dialog/set-project-edits :minutes (-> % .-target .-value js/parseInt)])}]
           [:div ":"]
           [:input.py-2.w-24.bg-gray-100.text-xl.text-center.rounded-lg.border.border-gray-300.mb-5
            {:type "number"
             :placeholder "Seconds"
             :value (:seconds project-edits)
             :on-change #(rf/dispatch [:dialog/set-project-edit :seconds (-> % .-target .-value js/parseInt)])}]]
          [:div.w-full.flex
           [:div.flex-grow]
           [:div.p-2.cursor-pointer.hover:bg-gray-100.rounded-lg
            {:on-click #(rf/dispatch [:dialog/close-edit-project-dialog])} "Cancel"]
           [:div.p-2.cursor-pointer.hover:bg-blue.text-center.rounded-lg.hover:bg-green-500
            {:on-click #(rf/dispatch [:dialog/edit-project project-edits])} "Confirm"]]]]))))

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
          :on-change #(rf/dispatch [::set-expense-edits :cost (-> % .-target .-value js/parseInt)])}]
        [:div.w-full.flex
         [:div.flex-grow]
         [:div.p-2.cursor-pointer.hover:bg-gray-100.rounded-lg
          {:on-click #(rf/dispatch [::close-expense-dialog])}
          "Cancel"]
         [:div.p-2.cursor-pointer.hover:bg-blue.text-center.rounded-lg.hover:bg-green-500
          {:on-click #(rf/dispatch [::add-expense new-expense])}
          "Add"]]]])))