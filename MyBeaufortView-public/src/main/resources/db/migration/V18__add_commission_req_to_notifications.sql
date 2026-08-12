ALTER TABLE notifications
ADD COLUMN commission_request_id BIGINT NULL,
ADD CONSTRAINT fk_notifications_commission_request
    FOREIGN KEY (commission_request_id) REFERENCES commission_requests(id) ON DELETE SET NULL;
