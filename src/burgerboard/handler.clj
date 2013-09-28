(ns burgerboard.handler
  (:use compojure.core
        burgerboard.database
        burgerboard.users
        )
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defn login [session username password]
  (if (login-valid (find-user username) username password)
    {:status 200
     :session (assoc session :username username)
     :body "Login"}
    {:status 403
     :body "Invalid username or password"}
    )
  )

(defn boards [session]
  (if-not (:username session)
    {:status 401
     :body "Login required"}
    )
  )

(defroutes api-routes
  (POST "/login" [username password :as {session :session}]
        (login session username password))
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
