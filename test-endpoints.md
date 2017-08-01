`POST        /current-profile-setup`  
will try to GET current profile from Business Registration (BR), if it's not found will POST to Business Registration to create a profile entry; this only needs to be called once per user to "setup" that user

`POST        /clear`  
clears all entries in the **registration-information** mongo collection

