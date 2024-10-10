-- Insert sample cities
INSERT INTO city (name, latitude, longitude)
VALUES ('Warsaw', 52.2297, 21.0122),
       ('Krakow', 50.0647, 19.9450);

-- Insert sample users
INSERT INTO traveler (username, password, enabled, email, first_name, last_name, traveler_type)
VALUES ('jdoe', 'password123', 1, 'jdoe@example.com', 'John', 'Doe', 'DRIVER');

INSERT INTO vehicle (make, model, production_year, color, license_plate, owner_id)
VALUES ('Toyota', 'Corolla', 2015, 'Blue', 'ABC123', 1);

INSERT INTO ride (driver_id, origin_id, destination_id, departure_time, available_seats, price_per_seat, vehicle_id, ride_status)
VALUES (1, 1, 2, '2024-09-16 09:00:00', 3, 50.00, 1, 'OPEN');

INSERT INTO authorities (authority, username)
VALUES ('read', 'jdoe');


