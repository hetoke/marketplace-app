CREATE TABLE admin_action_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_action_log_admin_id ON admin_action_log(admin_id);
CREATE INDEX idx_admin_action_log_target ON admin_action_log(target_type, target_id);
CREATE INDEX idx_admin_action_log_created_at ON admin_action_log(created_at);

ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
