CREATE TABLE test_entity (
  id BIGINT AUTO_INCREMENT NOT NULL,
  my_field VARCHAR(255) NULL,
  version BIGINT NULL,
  tenant VARCHAR(255) NULL,
  CONSTRAINT pk_testentity PRIMARY KEY (id)
);

CREATE TABLE user (
  id BIGINT AUTO_INCREMENT NOT NULL,
  username VARCHAR(255) NULL,
  display_name VARCHAR(255) NULL,
  email VARCHAR(255) NULL,
  phone VARCHAR(255) NULL,
  password VARCHAR(60) NOT NULL,
  status VARCHAR(255) NULL,
  tenant VARCHAR(255) NULL,
  CONSTRAINT pk_user PRIMARY KEY (id)
);

CREATE TABLE user_roles (user_id BIGINT NOT NULL, roles VARCHAR(255) NULL);

ALTER TABLE user ADD CONSTRAINT uc_user_email UNIQUE (email);

ALTER TABLE user ADD CONSTRAINT uc_user_phone UNIQUE (phone);

ALTER TABLE user ADD CONSTRAINT uc_user_username UNIQUE (username);

ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_on_user FOREIGN KEY (user_id) REFERENCES user (id);