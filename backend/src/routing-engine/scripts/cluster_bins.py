import os
import psycopg2
from sklearn.cluster import KMeans
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL")
N_CLUSTERS = 4

def main():
    if not DATABASE_URL:
        raise ValueError("DATABASE_URL not found in .env")

    print("Connecting to database...")
    conn = psycopg2.connect(DATABASE_URL)
    cur = conn.cursor()

    print("Loading active bins...")
    cur.execute("""
        SELECT id, lat, lng
        FROM bins
        WHERE is_active = true
          AND lat IS NOT NULL
          AND lng IS NOT NULL
    """)
    rows = cur.fetchall()

    if not rows:
        print("No bins found.")
        cur.close()
        conn.close()
        return

    bin_ids = [row[0] for row in rows]
    coords = [[float(row[1]), float(row[2])] for row in rows]

    print(f"Loaded {len(coords)} bins.")

    if len(coords) < N_CLUSTERS:
        print(f"Not enough bins ({len(coords)}) for {N_CLUSTERS} clusters.")
        cur.close()
        conn.close()
        return

    print(f"Running KMeans with {N_CLUSTERS} clusters...")
    model = KMeans(n_clusters=N_CLUSTERS, random_state=42, n_init=10)
    labels = model.fit_predict(coords)

    print("Updating cluster_id in database...")
    for bin_id, cluster_id in zip(bin_ids, labels):
        cur.execute("""
            UPDATE bins
            SET cluster_id = %s
            WHERE id = %s
        """, (int(cluster_id), int(bin_id)))

    conn.commit()

    print(f"Updated {len(bin_ids)} bins with cluster_id.")
    print("Cluster distribution:")

    counts = {}
    for label in labels:
        counts[label] = counts.get(label, 0) + 1

    for cluster_id, count in sorted(counts.items()):
        print(f"  Cluster {cluster_id}: {count} bins")

    cur.close()
    conn.close()
    print("Done.")

if __name__ == "__main__":
    main()