import os
from collections import Counter

import psycopg2
from dotenv import load_dotenv
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL")
MIN_K = int(os.getenv("MIN_K", "2"))
MAX_K = int(os.getenv("MAX_K", "6"))
MIN_CLUSTER_SIZE = int(os.getenv("MIN_CLUSTER_SIZE", "4"))


def choose_best_k(coords: list[list[float]]) -> int:
    n = len(coords)

    if n < 2:
        return 1

    max_k = min(MAX_K, n)

    best_k = 1
    best_score = -1.0
    found_valid = False

    for k in range(MIN_K, max_k + 1):
        try:
            model = KMeans(n_clusters=k, random_state=42, n_init=10)
            labels = model.fit_predict(coords)

            counts = Counter(labels)

            if min(counts.values()) < MIN_CLUSTER_SIZE:
                print(f"k={k} -> rejected (cluster size < {MIN_CLUSTER_SIZE})")
                continue

            score = silhouette_score(coords, labels)
            print(f"k={k} -> silhouette={score:.4f} | counts={dict(counts)}")

            if score > best_score:
                best_score = score
                best_k = k
                found_valid = True

        except Exception as e:
            print(f"k={k} -> error: {e}")

    if found_valid:
        return best_k

    return 1


def main() -> None:
    if not DATABASE_URL:
        raise ValueError("DATABASE_URL not found in .env")

    print("Connecting to database...")
    conn = psycopg2.connect(DATABASE_URL)
    cur = conn.cursor()

    print("Loading active bins with zone...")
    cur.execute(
        """
        SELECT id, lat, lng, zone_id
        FROM bins
        WHERE is_active = true
          AND lat IS NOT NULL
          AND lng IS NOT NULL
          AND zone_id IS NOT NULL
        ORDER BY zone_id, id
        """
    )
    rows = cur.fetchall()

    if not rows:
        print("No bins found.")
        cur.close()
        conn.close()
        return

    bins_by_zone: dict[int, list[tuple[int, float, float]]] = {}

    for row in rows:
        bin_id = int(row[0])
        lat = float(row[1])
        lng = float(row[2])
        zone_id = int(row[3])
        bins_by_zone.setdefault(zone_id, []).append((bin_id, lat, lng))

    total_updated = 0

    for zone_id, zone_bins in bins_by_zone.items():
        coords = [[b[1], b[2]] for b in zone_bins]
        bin_ids = [b[0] for b in zone_bins]

        print(f"\nZone {zone_id}: {len(coords)} bins")

        if len(coords) < 2:
            selected_k = 1
            labels = [0] * len(coords)
        else:
            selected_k = choose_best_k(coords)

            if selected_k == 1:
                labels = [0] * len(coords)
            else:
                final_model = KMeans(n_clusters=selected_k, random_state=42, n_init=10)
                labels = final_model.fit_predict(coords)

        print(f"-> selected k = {selected_k}")

        for bin_id, cluster_id in zip(bin_ids, labels):
            cur.execute(
                """
                UPDATE bins
                SET cluster_id = %s
                WHERE id = %s
                """,
                (int(cluster_id), int(bin_id))
            )
            total_updated += 1

    conn.commit()
    print(f"\nUpdated {total_updated} bins with cluster_id.")
    cur.close()
    conn.close()
    print("Done.")


if __name__ == "__main__":
    main()