INSERT INTO users (username, password, name, role) VALUES
    ('User1', '$2a$10$BqF7VXu7S7uH94FGCUeCduEOt9BohdnZeM0.c1Cq/PFw2P7wVdbQ6', 'User1', 'USER'),
    ('User2', '$2a$10$BqF7VXu7S7uH94FGCUeCduEOt9BohdnZeM0.c1Cq/PFw2P7wVdbQ6', 'User2', 'USER'),
    ('admin', '$2a$10$NvpNj18IUehXFU07zqv/dediRD7lbqwDVdklxofuYuPB4nsFGqo3i', 'Admin', 'ADMIN');

INSERT INTO rooms (name, max_capacity) VALUES
    ('A', 5),
    ('B', 6),
    ('C', 7),
    ('D', 8),
    ('E', 9);

INSERT INTO booking_settings (slot_minutes, max_slots) VALUES (30, 6);
