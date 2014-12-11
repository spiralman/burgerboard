(ns burgerboard.handler
  (:use compojure.core
        burgerboard.database
        burgerboard.users
        burgerboard.authentication
        burgerboard.board-handlers
        burgerboard.api
        [clojure.data.json :as json :only [write-str]]
        )
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defn render-group [request group]
  (assoc group
    :boards_url (resolve-route request
                               "groups" (:id group)
                               "boards")
    :members_url (resolve-route request
                                "groups" (:id group)
                                "members"))
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
           :body (json/write-str (dissoc user :password))
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

(defroutes api-routes
  (POST "/login" request
        (login request))

  (POST "/signups" request
        (signup request))

  (GET "/groups" request
       (require-login request get-users-groups))

  (POST "/groups/:group-id/members" request
        (require-ownership request invite))

  board-routes
  )

(defn page [request]
"<html>
  <body>
    <div id=\"burgerboard\"></div>
    <script src=\"/static/contrib/react-0.9.0.js\"></script>
    <script src=\"/static/out/goog/base.js\" type=\"text/javascript\"></script>
    <script src=\"/static/burgerboard.js\" type=\"text/javascript\"></script>
    <script type=\"/static/text/javascript\">goog.require(\"burgerboard_web.app\");</script>
  </body>
</html>
"
  )

(defroutes app-routes
  (context "/api/v1" [] api-routes)
  (GET "/" request
       (page request))
  (route/files "/static" {:root "web-client"})
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
