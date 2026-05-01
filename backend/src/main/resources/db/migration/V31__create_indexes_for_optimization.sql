CREATE INDEX IF NOT EXISTS idx_trucks_status ON trucks(status);
CREATE INDEX IF NOT EXISTS idx_trucks_driver ON trucks(assigned_driver_id);

CREATE INDEX IF NOT EXISTS idx_truck_incidents_truck ON truck_incidents(truck_id);
CREATE INDEX IF NOT EXISTS idx_truck_incidents_mission ON truck_incidents(mission_id);
CREATE INDEX IF NOT EXISTS idx_truck_incidents_status ON truck_incidents(status);

CREATE INDEX IF NOT EXISTS idx_route_plans_mission ON route_plans(mission_id);
CREATE INDEX IF NOT EXISTS idx_route_plans_truck ON route_plans(truck_id);

CREATE INDEX IF NOT EXISTS idx_route_stops_route_plan ON route_stops(route_plan_id);
CREATE INDEX IF NOT EXISTS idx_route_stops_bin ON route_stops(bin_id);

CREATE INDEX IF NOT EXISTS idx_mission_reassignments_mission ON mission_reassignments(original_mission_id);
CREATE INDEX IF NOT EXISTS idx_mission_reassignments_bin ON mission_reassignments(bin_id);