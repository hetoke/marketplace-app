CREATE TABLE upload_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    storage_path VARCHAR(1024) NOT NULL,
    supabase_token VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_upload_sessions_entity ON upload_sessions(entity_type, entity_id);
CREATE INDEX idx_upload_sessions_status ON upload_sessions(status);
CREATE INDEX idx_upload_sessions_storage_path ON upload_sessions(storage_path);
