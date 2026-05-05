CREATE TABLE leave_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    default_days INTEGER NOT NULL,
    description VARCHAR(500)
);
