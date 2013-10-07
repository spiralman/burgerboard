(ns burgerboard.authentication
  (:use burgerboard.database
        burgerboard.users)
  )

(defn require-login [request handler]
  (if-let [email (:email (:session request))]
    (handler (find-user email) request)
    {:status 401
     :body "Login required"}
    )
  )

(defn require-membership [request handler]
  (require-login
   request
   (fn [user request]
     (if-let [group (find-group (:group-id (:params request)))]
       (if (is-member? group user)
         (handler user group request)
         {:status 403
          :body ""}
         )
       )
     )
   )
  )
     

(defn require-ownership [request handler]
  (require-login
   request
   (fn [user request]
     (if-let [group (find-group (:group-id (:params request)))]
       (if (is-owner? group user)
         (handler user group request)
         {:status 403
          :body ""}
         )
       )
     )
   )
  )
