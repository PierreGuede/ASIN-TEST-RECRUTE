-- Requêtes SQL d'impact pour l'étude de cas TMA ASIN
-- Ces requêtes analysent l'export de la base de données pour identifier les anomalies

-- 1. Rendez-vous confirmés à venir pris un samedi ou un dimanche
-- Permet de prévenir les usagers concernés
SELECT
    id,
    npi,
    service,
    date_heure,
    statut,
    cree_le,
    strftime('%w', date_heure) AS jour_semaine
FROM rendez_vous
WHERE statut = 'confirme'
    AND date_heure > '2026-11-20 18:00:00'
    AND (strftime('%w', date_heure) = '0' OR strftime('%w', date_heure) = '6')
ORDER BY date_heure;

-- 2. Créneaux qui comptent plus de 3 rendez-vous non annulés
-- Identifie les créneaux surbookés
SELECT
    date_heure,
    COUNT(*) AS nombre_rendez_vous
FROM rendez_vous
WHERE statut != 'annule'
GROUP BY date_heure
HAVING COUNT(*) > 3
ORDER BY date_heure;

-- Détail des rendez-vous sur les créneaux surbookés
SELECT
    rv.date_heure,
    rv.id,
    rv.npi,
    rv.service,
    rv.statut
FROM rendez_vous rv
WHERE rv.statut != 'annule'
    AND rv.date_heure IN (
        SELECT date_heure
        FROM rendez_vous
        WHERE statut != 'annule'
        GROUP BY date_heure
        HAVING COUNT(*) > 3
    )
ORDER BY rv.date_heure, rv.id;

-- 3. Rendez-vous passés du statut "annulé" à un autre statut
-- Identifie les changements de statut anormaux sur des rendez-vous passés
SELECT
    rv.id,
    rv.npi,
    rv.service,
    rv.date_heure,
    rv.statut AS statut_actuel,
    rv.cree_le,
    hs.ancien_statut,
    hs.nouveau_statut,
    hs.modifie_le,
    hs.modifie_par
FROM rendez_vous rv
JOIN historique_statuts hs ON rv.id = hs.rendez_vous_id
WHERE rv.date_heure < '2026-11-20 18:00:00'
    AND hs.ancien_statut = 'annule'
    AND hs.nouveau_statut != 'annule'
ORDER BY rv.date_heure DESC, hs.modifie_le DESC;

-- 4. Analyse générale des statuts
SELECT
    statut,
    COUNT(*) AS nombre
FROM rendez_vous
GROUP BY statut
ORDER BY nombre DESC;

-- 5. Analyse par service
SELECT
    service,
    statut,
    COUNT(*) AS nombre
FROM rendez_vous
GROUP BY service, statut
ORDER BY service, nombre DESC;

-- 6. Rendez-vous confirmés à venir par jour
SELECT
    DATE(date_heure) AS jour,
    COUNT(*) AS nombre
FROM rendez_vous
WHERE statut = 'confirme'
    AND date_heure > '2026-11-20 18:00:00'
GROUP BY DATE(date_heure)
ORDER BY jour;

-- 7. Usagers avec plusieurs rendez-vous à venir (anomalie potentielle)
SELECT
    npi,
    COUNT(*) AS nombre_rendez_vous
FROM rendez_vous
WHERE statut != 'annule'
    AND statut != 'honore'
    AND statut != 'absent'
    AND date_heure > '2026-11-20 18:00:00'
GROUP BY npi
HAVING COUNT(*) > 1
ORDER BY nombre_rendez_vous DESC;
