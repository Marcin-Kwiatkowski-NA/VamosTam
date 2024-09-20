-- With Id
INSERT INTO city (name, latitude, longitude) VALUES ('Kielce', 52.2297, 21.0122);
-- Without Id
INSERT INTO city (id, name, latitude, longitude) VALUES (100, 'Kraków', 53.2297, 24.0122);
INSERT INTO city (name, latitude, longitude) VALUES ('Wrocław', 56.2297, 27.0122);

-- Insert sample users
INSERT INTO traveler (username, password, email, first_name, last_name, traveler_type)
VALUES ('jdoe', 'password123', 'jdoe@example.com', 'John', 'Doe', 'DRIVER');
INSERT INTO traveler (id, username, password, email, first_name, last_name, traveler_type)
VALUES (100, 'jdoe2', 'password1232', 'jdoe2@example.com', 'John2', 'Doe2', 'DRIVER');

INSERT INTO vehicle (make, model, production_year, color, license_plate, owner_id)
VALUES ('Toyota', 'Corolla', 2015, 'Blue', 'ABC123', 1);
INSERT INTO vehicle (id, make, model, production_year, color, license_plate, owner_id)
VALUES (100, 'Toyota', 'Corolla', 2015, 'Blue', 'ABC123', 1);


INSERT INTO ride (driver_id, origin_id, destination_id, departure_time, available_seats, price_per_seat, vehicle_id, ride_status)
VALUES (1, 1, 2, '2024-09-16 09:00:00', 3, 50.00, 1, 'OPEN');
INSERT INTO ride (id, driver_id, origin_id, destination_id, departure_time, available_seats, price_per_seat, vehicle_id, ride_status)
VALUES (100, 100, 100, 1, '2024-01-16 09:00:00', 1, 10.00, 100, 'OPEN');

