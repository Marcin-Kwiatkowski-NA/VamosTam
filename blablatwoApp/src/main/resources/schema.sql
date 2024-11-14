-- Enable foreign key constraints
SET REFERENTIAL_INTEGRITY FALSE;

-- 2. Cities Table
CREATE TABLE IF NOT EXISTS city (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE
);
-- 1. Users Table
CREATE TABLE IF NOT EXISTS traveler (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled INT NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    traveler_type VARCHAR(10) CHECK (traveler_type IN ('DRIVER', 'PASSENGER', 'BOTH'))
);

CREATE TABLE IF NOT EXISTS authorities (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(45) NULL,
    authority VARCHAR(45) NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (username) REFERENCES traveler(username)
);


-- 3. Vehicles Table
CREATE TABLE IF NOT EXISTS vehicle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    make VARCHAR(255),
    model VARCHAR(255),
    production_year INT,
    color VARCHAR(50),
    license_plate VARCHAR(50),
    owner_id BIGINT,
    FOREIGN KEY (owner_id) REFERENCES traveler(id)
);


-- 4. Rides Table
CREATE TABLE IF NOT EXISTS ride (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    driver_id BIGINT NOT NULL,
    origin_id BIGINT NOT NULL,
    destination_id BIGINT NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    available_seats INT NOT NULL,
    price_per_seat DECIMAL(10,2),
    vehicle_id BIGINT,
    ride_status VARCHAR(10) CHECK (ride_status IN ('OPEN', 'FULL', 'COMPLETED', 'CANCELLED')),
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    FOREIGN KEY (driver_id) REFERENCES traveler(id),
    FOREIGN KEY (origin_id) REFERENCES city(id),
    FOREIGN KEY (destination_id) REFERENCES city(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id)
);
