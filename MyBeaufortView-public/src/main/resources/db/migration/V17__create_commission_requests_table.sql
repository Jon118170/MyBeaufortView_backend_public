CREATE TABLE commission_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    photographer_id BIGINT NOT NULL,
    post_id BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_commission_requests_requester
        FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_commission_requests_photographer
        FOREIGN KEY (photographer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_commission_requests_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE SET NULL,
    CONSTRAINT chk_commission_requests_not_self
        CHECK (requester_id <> photographer_id)
);

CREATE INDEX idx_commission_requests_requester_created_at
    ON commission_requests(requester_id, created_at DESC);

CREATE INDEX idx_commission_requests_photographer_created_at
    ON commission_requests(photographer_id, created_at DESC);

CREATE INDEX idx_commission_requests_status
    ON commission_requests(status);
