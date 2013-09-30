(ns burgerboard.handler
  (:use compojure.core
        burgerboard.database
        burgerboard.users
        [clojure.data.json :as json]
        )
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defn login [session email password]
  (if (login-valid (find-user email) email password)
    {:status 200
     :session (assoc session :email email)
     :body "Login"}
    {:status 403
     :body "Invalid email or password"}
    )
  )

(defn invalid-user []
  {:status 400
   :body "Invalid user"}
  )

(defn signup [session email password name]
  (if (not (nil? (find-user email)))
   (invalid-user)
   (if-let [user (create-user email password name)]
     (do
       (insert-user user)
       {:status 201
        :session (assoc session :email email)
        :body (json/write-str (dissoc user :password))
        }
       )
      (invalid-user)
     )
   )
  )

(defn invite [user group request]
  (insert-member group (find-user (:email (:params request))))
  {:status 201}
  )

(defn boards [session]
  (if-not (:email session)
    {:status 401
     :body "Login required"}
    )
  )

(defn require-login [request handler]
  (if-let [email (:email (:session request))]
    (handler (find-user email) request)
    {:status 401
     :body "Login required"}
    )
  )

(defn require-ownership [request handler]
  (require-login
   request
   (fn [user request]
     (if-let [group (find-group (:group-id (:params request)))]
       (if-not (= (:owner group) (:email user))
         {:status 403
          :body ""}
         (handler user group request)
         )
       )
     )
   )
  )

(defroutes api-routes
  (POST "/login" [email password :as {session :session}]
        (login session email password))

  (POST "/signups" [email password name :as {session :session}]
        (signup session email password name))

  (POST "/groups/:group-id/members" request
        (require-ownership request invite))

  (GET "/boards" [:as {session :session}]
       (boards session))
  )

(defroutes app-routes
  (context "/api/v1" [] api-routes)
  (GET "/" [] "Hello World")
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
