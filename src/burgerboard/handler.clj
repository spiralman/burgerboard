(ns burgerboard.handler
  (:use compojure.core
        burgerboard.database
        burgerboard.users
        burgerboard.authentication
        burgerboard.board-handlers
        burgerboard.api
        hiccup.core
        hiccup.page
        [clojure.data.json :as json :only [write-str]]
        )
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

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

(defn invalid-user []
  {:status 400
   :body "Invalid user"}
  )

(defn signup [request]
  (let [session (:session request)
        {:keys [email password name]} (body-json request)]
    (if (not (nil? (find-user email)))
      (invalid-user)
      (if-let [user (create-user email password name)]
        (do
          (insert-user user)
          {:status 201
           :session (assoc session :email email)
           :body (write-str (render-user request user))
           }
          )
        (invalid-user)
        )
      )
    )
  )

(defn invite [user group request]
  (let [{:keys [email name]} (body-json request)]
    (insert-member group (find-user email))
    {:status 201}
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

  (GET "/groups" request
       (require-login request get-users-groups))

  (POST "/groups" request
       (require-login request post-group))

  (POST "/groups/:group-id/members" request
        (require-ownership request invite))

  board-routes
  )

(defn fetch-user-data [request]
  (if-let [email (:email (:session request))]
    (write-str (render-user request (find-user email)))
    ""
    )
  )

(defn page [request]
  (let [user-data (fetch-user-data request)]
    (html5
     [:body
      [:div#burgerboard {}]
      (include-js "/static/contrib/react-0.9.0.js"
                  "/static/burgerboard.js")
      [:script {:type "text/javascript"} (str "var burgerboard_init_state=\""
                                              user-data
                                              "\";")]
      [:script {:type "text/javascript"} "goog.require(\"burgerboard_web.app\");"]]
     )
    )
  )

(defroutes app-routes
  (context "/api/v1" [] api-routes)
  (GET "/" request
       (page request))
  (route/files "/static" {:root "web-client"})
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
