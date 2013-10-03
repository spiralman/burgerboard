(ns burgerboard.authentication
  (:use burgerboard.database)
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
       (if (= (:owner group) (:email user))
         (handler user group request)
         {:status 403
          :body ""}
         )
       )
     )
   )
  )
