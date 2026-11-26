ALTER TABLE incidents ADD COLUMN assigned_to_user_id BIGINT NULL;
ALTER TABLE incidents ADD COLUMN assignee_name VARCHAR(200) NULL;
ALTER TABLE incidents ADD COLUMN acknowledged_by_user_id BIGINT NULL;
ALTER TABLE incidents ADD COLUMN acknowledged_at DATETIME(6) NULL;
ALTER TABLE incidents ADD COLUMN context_payload JSON NULL;
CREATE TABLE alert_updates (
  sequence_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  event_id CHAR(36) NOT NULL UNIQUE,
  organization_id BIGINT NOT NULL,
  incident_id BIGINT NOT NULL,
  payload JSON NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  INDEX ix_alert_updates_org (organization_id,sequence_id)
);
CREATE TABLE corrective_actions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  incident_id BIGINT NOT NULL,
  organization_id BIGINT NOT NULL,
  author_user_id BIGINT NOT NULL,
  description VARCHAR(2000) NOT NULL,
  created_at DATETIME(6) NOT NULL
);

ALTER TABLE incidents ADD COLUMN assigned_at DATETIME(6) NULL;
