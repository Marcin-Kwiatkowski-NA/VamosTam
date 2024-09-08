-- Create schema
CREATE SCHEMA IF NOT EXISTS ride_sharing;

-- Cities Table
CREATE TABLE IF NOT EXISTS ride_sharing.cities (
    id UUID NOT NULL DEFAULT random_uuid(),
    name VARCHAR(56) NOT NULL,
    PRIMARY KEY (id)
);

-- Users Table
CREATE TABLE IF NOT EXISTS ride_sharing.users (
    id UUID NOT NULL DEFAULT random_uuid(),
    name VARCHAR(56),
    role VARCHAR(16) CHECK (role IN ('DRIVER', 'PASSENGER')),
    PRIMARY KEY (id)
);

-- Rides Table (with driver_id reference)
CREATE TABLE IF NOT EXISTS ride_sharing.rides (
    id UUID NOT NULL DEFAULT random_uuid(),
    driver_id UUID NOT NULL,  -- Reference to users (drivers)
    start_city_id UUID NOT NULL,
    destination_city_id UUID NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    price NUMERIC(16, 2) DEFAULT 100.00 NOT NULL,
    pet_friendly BOOLEAN,
    status VARCHAR(16) CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED')),
    PRIMARY KEY (id),
    FOREIGN KEY (driver_id) REFERENCES ride_sharing.users(id),
    FOREIGN KEY (start_city_id) REFERENCES ride_sharing.cities(id),
    FOREIGN KEY (destination_city_id) REFERENCES ride_sharing.cities(id)
);

-- City Stops Table
CREATE TABLE IF NOT EXISTS ride_sharing.city_stops (
    id UUID NOT NULL DEFAULT random_uuid(),
    ride_id UUID NOT NULL,
    city_id UUID NOT NULL,
    position INTEGER NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (ride_id) REFERENCES ride_sharing.rides(id),
    FOREIGN KEY (city_id) REFERENCES ride_sharing.cities(id)
);

-- Passengers Table
CREATE TABLE IF NOT EXISTS ride_sharing.passengers (
    id UUID NOT NULL DEFAULT random_uuid(),
    ride_id UUID NOT NULL,
    passenger_id UUID NOT NULL,  -- Reference to users (passengers)
    status VARCHAR(16) CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    PRIMARY KEY (id),
    FOREIGN KEY (ride_id) REFERENCES ride_sharing.rides(id),
    FOREIGN KEY (passenger_id) REFERENCES ride_sharing.users(id)
);

-- Feedback Table (with driver_id reference)
CREATE TABLE IF NOT EXISTS ride_sharing.feedback (
    id UUID NOT NULL DEFAULT random_uuid(),
    ride_id UUID NOT NULL,
    driver_id UUID NOT NULL,  -- Reference to drivers
    user_id UUID NOT NULL,  -- Reference to users providing feedback
    rating INTEGER CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(255),
    PRIMARY KEY (id),
    FOREIGN KEY (ride_id) REFERENCES ride_sharing.rides(id),
    FOREIGN KEY (driver_id) REFERENCES ride_sharing.users(id),
    FOREIGN KEY (user_id) REFERENCES ride_sharing.users(id)
);

-- Notifications Table
CREATE TABLE IF NOT EXISTS ride_sharing.notifications (
    id UUID NOT NULL DEFAULT random_uuid(),
    user_id UUID NOT NULL,  -- Reference to users receiving the notification
    message VARCHAR(255),
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES ride_sharing.users(id)
);

-- Conversations Table
CREATE TABLE IF NOT EXISTS ride_sharing.conversations (
    id UUID NOT NULL DEFAULT random_uuid(),
    PRIMARY KEY (id)
);

-- Messages Table
CREATE TABLE IF NOT EXISTS ride_sharing.messages (
    id UUID NOT NULL DEFAULT random_uuid(),
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,  -- Reference to the sender (user)
    receiver_id UUID NOT NULL,  -- Reference to the receiver (user)
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (conversation_id) REFERENCES ride_sharing.conversations(id),
    FOREIGN KEY (sender_id) REFERENCES ride_sharing.users(id),
    FOREIGN KEY (receiver_id) REFERENCES ride_sharing.users(id)
);

-- Seed data for cities table
INSERT INTO ride_sharing.cities (id, name) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Warsaw'),
  ('11111111-1111-1111-1111-111111111112', 'Krakow'),
  ('11111111-1111-1111-1111-111111111113', 'Gdansk'),
  ('11111111-1111-1111-1111-111111111114', 'Poznan'),
  ('11111111-1111-1111-1111-111111111115', 'Lodz'),
  ('11111111-1111-1111-1111-111111111116', 'Wroclaw'),
  ('11111111-1111-1111-1111-111111111117', 'Katowice'),
  ('11111111-1111-1111-1111-111111111118', 'Szczecin'),
  ('11111111-1111-1111-1111-111111111119', 'Bialystok'),
  ('11111111-1111-1111-1111-111111111120', 'Torun');

-- Seed data for users table
INSERT INTO ride_sharing.users (id, name, role) VALUES
  ('22222222-2222-2222-2222-222222222221', 'John Doe', 'DRIVER'),
  ('22222222-2222-2222-2222-222222222222', 'Jane Smith', 'PASSENGER'),
  ('22222222-2222-2222-2222-222222222223', 'Adam Nowak', 'DRIVER'),
  ('22222222-2222-2222-2222-222222222224', 'Ewa Kowalska', 'PASSENGER'),
  ('22222222-2222-2222-2222-222222222225', 'Pawel Mazur', 'DRIVER'),
  ('22222222-2222-2222-2222-222222222226', 'Anna Zielinska', 'PASSENGER');

-- Seed data for rides table
INSERT INTO ride_sharing.rides (id, driver_id, start_city_id, destination_city_id, departure_time, price, pet_friendly, status) VALUES
  ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111112', '2024-09-12 09:00:00', 50.00, true, 'ACTIVE'),
  ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111113', '11111111-1111-1111-1111-111111111114', '2024-09-15 14:30:00', 70.00, false, 'ACTIVE'),
  ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222225', '11111111-1111-1111-1111-111111111116', '11111111-1111-1111-1111-111111111117', '2024-09-20 10:00:00', 45.00, true, 'ACTIVE');

-- Seed data for city_stops table
INSERT INTO ride_sharing.city_stops (id, ride_id, city_id, position) VALUES
  ('44444444-4444-4444-4444-444444444441', '33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111115', 1),
  ('44444444-4444-4444-4444-444444444442', '33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111118', 1);

-- Seed data for passengers table
INSERT INTO ride_sharing.passengers (id, ride_id, passenger_id, status) VALUES
  ('55555555-5555-5555-5555-555555555551', '33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222222', 'CONFIRMED'),
  ('55555555-5555-5555-5555-555555555552', '33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222224', 'PENDING');

-- Seed data for feedback table
INSERT INTO ride_sharing.feedback (id, ride_id, user_id, driver_id, rating, comment) VALUES
  ('66666666-6666-6666-6666-666666666661', '33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222221', 5, 'Great ride!'),
  ('66666666-6666-6666-6666-666666666662', '33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222224', '22222222-2222-2222-2222-222222222223', 4, 'Comfortable and smooth journey.');

-- Seed data for notifications table
INSERT INTO ride_sharing.notifications (id, user_id, message) VALUES
  ('77777777-7777-7777-7777-777777777771', '22222222-2222-2222-2222-222222222222', 'Your ride is confirmed.'),
  ('77777777-7777-7777-7777-777777777772', '22222222-2222-2222-2222-222222222224', 'Your booking is pending confirmation.');

-- Seed data for conversations table
INSERT INTO ride_sharing.conversations (id) VALUES
  ('88888888-8888-8888-8888-888888888881'),
  ('88888888-8888-8888-8888-888888888882');

-- Seed data for messages table
INSERT INTO ride_sharing.messages (id, conversation_id, sender_id, receiver_id, content, timestamp) VALUES
  ('99999999-9999-9999-9999-999999999991', '88888888-8888-8888-8888-888888888881', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222221', 'Hey, looking forward to the ride!', '2024-09-10 08:00:00'),
  ('99999999-9999-9999-9999-999999999992', '88888888-8888-8888-8888-888888888882', '22222222-2222-2222-2222-222222222224', '22222222-2222-2222-2222-222222222223', 'Hi, is it possible to bring my luggage?', '2024-09-11 09:30:00');
