(ns burgerboard.handler
  (:use compojure.core
        burgerboard.database
        burgerboard.users
        burgerboard.authentication
        burgerboard.board-handlers
        burgerboard.api
        hiccup.core
        hiccup.page
        hiccup.element
        [clojure.data.json :as json :only [write-str]]
        )
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.session.memory :as memory-session]))

(defn render-group [request group]
  (->
   (dissoc group :owner)
   (assoc
     :boards_url (resolve-route request
                                "groups" (:id group)
                                "boards")
     :members_url (resolve-route request
                                 "groups" (:id group)
                                 "members"))
   )
  )

(defn render-user [request user]
  (->
   (assoc user :groups_url (resolve-route request "groups"))
   (assoc :groups (map (partial render-group request) (:groups user)))
   (dissoc :password)))

(defn login [request]
  (let [{:keys [email password]} (body-json request)
        user (find-user email)]
    (if (login-valid (find-user email) email password)
      {:status 200
       :session (assoc (:session request) :email email)
       :body (write-str (render-user request user))}
      {:status 403
       :body "Invalid email or password"}
      )
    )
  )

(defn logout [request]
  {:status 201
   :session nil
   :body ""})

(defn invalid-user []
  {:status 400
   :body "Invalid user"}
  )

(defn signup-user [request email password name]
  (let [session (:session request)]
    (if-let [user (create-user email password name)]
      (let [user (assoc user
                   :groups (map #(dissoc (find-group (:group_id %)) :users)
                                (find-users-invitations user)))]
        (do
          (insert-user user)
          (delete-users-invitations user)
          {:status 201
           :session (assoc session :email email)
           :body (write-str (render-user request user))
           }))
      (invalid-user)
      ))
  )

(defn invitation-id [request]
  (Integer. (:invitation-id (:params request))))

(defn signup [request]
  (let [{:keys [email password name]} (body-json request)]
    (if (not (nil? (find-user email)))
      (invalid-user)
      (signup-user request email password name)
      )
    )
  )

(defn invite [user group request]
  (let [{:keys [email name]} (body-json request)]
    (if-let [user (find-user email)]
      (do
        (insert-member group user)
        {:status 201})
      (do
        (insert-invitation {:group_id (:id group) :user_email email})
        {:status 201})
      )
    )
  )

(defn accept-invitation [request]
  (if-let [invitation (find-invitation (invitation-id request))]
    (let [{:keys [name password]} (body-json request)]
      (signup-user request (:user_email invitation) password name)
      )
    {:status 404}
    )
  )

(defn get-users-groups [user request]
  (->
   {:groups
    (map (partial render-group request)
     (find-users-groups user))}
   (json-response 200))
  )

(defn post-group [user request]
  (let [{:keys [name]} (body-json request)]
    (-> (create-group name user)
        (insert-group)
        (insert-member user)
        (->> (render-group request))
        (json-response 201)
     )
    )
  )

(defroutes api-routes
  (POST "/login" request
        (login request))

  (POST "/signups" request
        (signup request))

  (DELETE "/login/current" request
          (logout request))

  (GET "/groups" request
       (require-login request get-users-groups))

  (POST "/groups" request
       (require-login request post-group))

  (POST "/groups/:group-id/members" request
        (require-ownership request invite))

  (POST "/invitations/:invitation-id" request
        (accept-invitation request))

  board-routes
  )

(defn fetch-user-data [request]
  (if-let [email (:email (:session request))]
    (write-str (render-user request (find-user email)))
    "undefined"
    )
  )

(defn page [request]
  (let [user-data (fetch-user-data request)]
    (html5
     [:head
      (include-css "/static/css/burgerboard.css")
      ]
     [:body
      [:div#burgerboard {}]
      (javascript-tag (str "var burgerboard_init_state="
                                              user-data
                                              ";"))
      (include-js "/static/burgerboard.js")
      ]
     )
    )
  )


(defroutes app-routes
  (context "/api/v1" [] api-routes)
  (GET "/" request
       (page request))
  (route/files "/static" {:root "web-client"})
  (route/not-found "Not Found"))

(defn build-app [session-store]
  (handler/site app-routes
                {:session {:cookie-attrs {:secure true}
                           :store session-store}}
                ))

(def app
  (build-app (memory-session/memory-store))
  )
