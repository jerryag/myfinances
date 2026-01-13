UPDATE "user" 
SET 
  login = 'master@myfinances.com',
  name = 'Usuário Master',
  type = 'MASTER',
  password = 'fc613b4dfd6736a7bd268c8a0e74ed0d1c04a959f59dd74ef2874983fd443fc9',
  change_pwd_on_login = false
WHERE login = 'admin@infotech.com';
