import math


def euclidean_distance_km(lat1, lng1, lat2, lng2):
    return math.sqrt((lat1 - lat2) ** 2 + (lng1 - lng2) ** 2) * 111.0