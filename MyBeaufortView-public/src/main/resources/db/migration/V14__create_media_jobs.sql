CREATE TABLE media_jobs (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_media_jobs_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT chk_media_jobs_attempt_count_nonnegative
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_media_jobs_post_id ON media_jobs(post_id);
CREATE INDEX idx_media_jobs_status ON media_jobs(status);
CREATE INDEX idx_media_jobs_type_status ON media_jobs(job_type, status);
