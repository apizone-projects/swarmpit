(ns swarmpit.component.registry-v2.create
  (:require [material.components :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.common :as common]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

(def text "Please enter your custom v2 registry name & address. If you are using secured registry provide account credentials as well.")

(defn- form-name [name]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :key             "name"
     :variant         "outlined"
     :defaultValue    name
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:name] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-url [url]
  (comp/text-field
    {:label           "Url"
     :fullWidth       true
     :key             "url"
     :variant         "outlined"
     :defaultValue    url
     :required        true
     :margin          "normal"
     :placeholder     "e.g. https://my.registry.io"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:url] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-auth [value]
  (comp/switch
    {:name     "authentication"
     :label    "Authentication"
     :color    "primary"
     :value    (str value)
     :checked  value
     :onChange #(state/update-value [:withAuth] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- form-username [value]
  (comp/text-field
    {:label           "Username"
     :fullWidth       true
     :name            "username"
     :key             "username"
     :variant         "outlined"
     :margin          "normal"
     :defaultValue    value
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:username] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-password [value show-password?]
  (comp/text-field
    {:label           "Password"
     :variant         "outlined"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if show-password?
                        "text"
                        "password")
     :defaultValue    value
     :onChange        #(state/update-value [:password] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment show-password?)}}))

(defn- create-registry-handler
  []
  (ajax/post
    (routes/path-for-backend :registry-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :reg-v2-info (select-keys response [:id]))))
                   (message/info
                     (str "Registry " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry creation failed. " (:error response))))}))

(defn init-form-state
  []
  (state/set-value {:valid?       false
                    :processing?  false
                    :showPassword false} state/form-state-cursor))

(defn init-form-value
  []
  (state/set-value {:name     ""
                    :url      ""
                    :public   false
                    :withAuth false
                    :username ""
                    :password ""} state/form-value-cursor))

(defn reset-form
  []
  (init-form-state)
  (init-form-value))

(rum/defc form < rum/reactive [_]
  (let [{:keys [name url withAuth username password]} (state/react state/form-value-cursor)
        {:keys [showPassword]} (state/react state/form-state-cursor)]
    (state/update-value [:valid?] (not
                                    (or
                                      (str/blank? name)
                                      (str/blank? url))) state/form-state-cursor)

    (html
      [:div
       (form-name name)
       (form-url name)
       (comp/form-control
         {:component "fieldset"}
         (comp/form-group
           {}
           (comp/form-control-label
             {:control (form-auth withAuth)
              :label   "Secured"})))
       (when withAuth
         (html
           [:div
            (form-username username)
            (form-password password showPassword)]))])))
