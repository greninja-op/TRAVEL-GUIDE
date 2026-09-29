import urllib.request
import json

def get_osrm_route(coords_list, mode='walking'):
    coord_str = ';'.join([f'{lng:.6f},{lat:.6f}' for lat, lng in coords_list])
    url = f'https://router.project-osrm.org/route/v1/{mode}/{coord_str}?overview=full&geometries=geojson'
    req = urllib.request.Request(url, headers={'User-Agent': 'TravelGuideApp/1.0'})
    with urllib.request.urlopen(req) as r:
        res = json.loads(r.read().decode())
        route = res['routes'][0]
        points = [(p[1], p[0]) for p in route['geometry']['coordinates']]
        dist_m = route['distance']
        print(f"Mode: {mode}, Distance: {dist_m:.1f}m, Points: {len(points)}")
        return points

# 1. Fort Kochi Loop (pedestrian road network):
fort_kochi_waypoints = [
    (9.9675, 76.2442), # Jetty
    (9.9672, 76.2428), # Chinese Fishing Nets
    (9.9663, 76.2418), # Vasco da Gama Square
    (9.9659, 76.2408), # St Francis Church
    (9.9648, 76.2390), # Princess Street
    (9.9646, 76.2394), # Kashi Art
    (9.9640, 76.2383), # Burgher Street
    (9.9635, 76.2385), # Indo-Portuguese Museum
    (9.9630, 76.2370), # Beach Walk
    (9.9658, 76.2410), # Fort Kochi Beach
    (9.9672, 76.2428), # Back to Fishing Nets
]
fk_points = get_osrm_route(fort_kochi_waypoints, 'walking')

# 2. Fort Kochi to Mattancherry Palace
fk_to_mp = [
    (9.9672, 76.2428), # Fishing Nets
    (9.9625, 76.2545), # Calvathy Bridge
    (9.9598, 76.2575), # Bazaar Rd
    (9.9585, 76.2595), # Mattancherry Palace
]
mp_points = get_osrm_route(fk_to_mp, 'driving')

print(f"Sample Fort Kochi point 0: {fk_points[0]}")
print(f"Sample Mattancherry point 0: {mp_points[0]}")
