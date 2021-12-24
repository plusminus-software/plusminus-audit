CREATE TABLE audit_log (
  number BIGINT AUTO_INCREMENT NOT NULL,
  tenant VARCHAR(255) NULL,
  entity_type VARCHAR(255) NULL,
  entity_id BIGINT NULL,
  time datetime NULL,
  username VARCHAR(255) NULL,
  device VARCHAR(255) NULL,
  action VARCHAR(255) NULL,
  current BIT(1) NULL,
  CONSTRAINT pk_auditlog PRIMARY KEY (number)
);

CREATE INDEX idx_a8a889640e487d676d70bee48 ON audit_log(tenant, entity_type, entity_id, current);

CREATE INDEX idx_c99d298eb0ee1c67f695c9445 ON audit_log(tenant, entity_type, device, number, current);